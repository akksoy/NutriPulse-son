package com.nutripulse.app.ui.ration

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nutripulse.app.R
import com.nutripulse.app.data.AppDatabase
import com.nutripulse.app.data.model.AnimalProfile
import com.nutripulse.app.data.model.AnimalSpecies
import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories
import com.nutripulse.app.data.solver.SmartFeedSelector
import com.nutripulse.app.data.solver.impl.FeasibilityChecker
import com.nutripulse.app.data.solver.impl.RationOptimizer
import com.nutripulse.app.data.solver.impl.RationOptimizer.ManualFeedInput
import com.nutripulse.app.data.solver.impl.RationOptimizer.ManualInput
import com.nutripulse.app.data.solver.impl.RationOptimizer.OptimizationInput
import com.nutripulse.app.data.solver.impl.RationOptimizer.RationResult
import com.nutripulse.app.data.solver.impl.RationStandards
import com.nutripulse.app.data.solver.impl.SpeciesOptimizationProfile.BalanceMode as SolverBalanceMode
import com.nutripulse.app.data.solver.impl.SpeciesOptimizationProfiles
import com.nutripulse.app.databinding.FragmentRationBinding
import com.nutripulse.app.ui.animal.AnimalListFragment
import com.nutripulse.app.ui.feed.FeedAdapter
import com.nutripulse.app.ui.ration.RationResultFragment.AlternativeRationSummary
import com.nutripulse.app.data.model.StockTransaction
import com.nutripulse.app.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RationFragment : Fragment() {

    private enum class BalanceMode(
        val label: String,
        val profileMode: SolverBalanceMode
    ) {
        LOW_COST("Düşük Maliyet", SolverBalanceMode.LOW_COST),
        BALANCED("Dengeli", SolverBalanceMode.BALANCED),
        STRICT("Hedefe Çok Yakın", SolverBalanceMode.STRICT)
    }

    private var _binding: FragmentRationBinding? = null
    private val binding get() = _binding!!

    private var selectedAnimal: AnimalProfile? = null
    private val candidateFeeds = mutableListOf<Feed>()
    private val selectedFeeds = mutableListOf<SelectedFeedInput>()
    private lateinit var candidateFeedAdapter: FeedAdapter
    private lateinit var selectedFeedAdapter: SelectedFeedAdapter
    private var selectedBalanceMode: BalanceMode = BalanceMode.BALANCED
    private val preparationCoordinator = RationPreparationCoordinator()

    companion object {
        const val REQUEST_SELECT_ANIMAL = "request_select_animal"
        const val KEY_ANIMAL_ID = "animal_id"

        fun newInstance(animalId: Int = -1) = RationFragment().apply {
            arguments = Bundle().apply { putInt(KEY_ANIMAL_ID, animalId) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        parentFragmentManager.setFragmentResultListener(REQUEST_SELECT_ANIMAL, viewLifecycleOwner) { _, result ->
            val animalId = result.getInt(KEY_ANIMAL_ID, -1)
            if (animalId > 0) loadAnimal(animalId)
        }

        setupCandidateFeedList()
        setupFinalFeedList()
        setupBalanceModeSpinner()

        binding.btnSelectAnimal.setOnClickListener { showAnimalPicker() }
        binding.btnAddFeeds.setOnClickListener { showFeedPicker() }
        binding.btnCalculate.setOnClickListener { runManualCalculation() }
        binding.btnOptimize.setOnClickListener { runOptimization() }
        binding.btnFinishRation.setOnClickListener { finishRation() }

        val animalId = arguments?.getInt(KEY_ANIMAL_ID, -1) ?: -1
        if (animalId > 0) loadAnimal(animalId) else tryAutoSelectLatestAnimal()

        updateFeedCount()
        refreshPreparationUi()
    }

    private fun setupCandidateFeedList() {
        candidateFeedAdapter = FeedAdapter(
            feeds = candidateFeeds,
            onFeedClick = { feed -> toggleFinalFeed(feed) },
            isInFinal = { feed -> selectedFeeds.any { it.feed.id == feed.id } }
        )
        binding.recyclerCandidateFeeds.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCandidateFeeds.adapter = candidateFeedAdapter
    }

    private fun setupFinalFeedList() {
        selectedFeedAdapter = SelectedFeedAdapter(
            feeds = selectedFeeds,
            onRemove = { item ->
                selectedFeeds.remove(item)
                selectedFeedAdapter.notifyDataSetChanged()
                updateFeedCount()
                refreshPreparationUi()
            },
            onAmountChanged = {
                updateManualNeedBars()
            }
        )
        binding.recyclerSelectedFeeds.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSelectedFeeds.adapter = selectedFeedAdapter
    }

    private fun setupBalanceModeSpinner() {
        val modes = BalanceMode.values().map { it.label }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, modes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBalanceMode.adapter = adapter
        binding.spinnerBalanceMode.setSelection(BalanceMode.BALANCED.ordinal, false)
        binding.spinnerBalanceMode.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedBalanceMode = BalanceMode.values().getOrElse(position) { BalanceMode.BALANCED }
                selectedAnimal?.let { applySpeciesConstraintDefaults(it, selectedBalanceMode.profileMode) }
                refreshPreparationUi()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                selectedBalanceMode = BalanceMode.BALANCED
            }
        })
    }

    private fun loadAnimal(id: Int) {
        lifecycleScope.launch {
            val animal = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).animalProfileDao().getById(id)
            }
            if (!isAdded || _binding == null) return@launch
            if (animal != null) setAnimal(animal, resetSelections = true) else {
                Toast.makeText(requireContext(), "Secilen hayvan bulunamadi.", Toast.LENGTH_SHORT).show()
                refreshPreparationUi()
            }
        }
    }

    private fun setAnimal(animal: AnimalProfile, resetSelections: Boolean = true) {
        val resolvedAnimal = RationOptimizer.resolveAnimalRequirements(animal)
        selectedAnimal = resolvedAnimal
        if (resetSelections) {
            candidateFeeds.clear()
            selectedFeeds.clear()
            candidateFeedAdapter.notifyDataSetChanged()
            selectedFeedAdapter.notifyDataSetChanged()
        }

        val emoji = when (resolvedAnimal.species) {
            "SIGIR" -> "🐄"
            "KOYUN" -> "🐑"
            "KANATLI" -> "🐔"
            "SU_URUNLERI" -> "🐟"
            "AT" -> "🐴"
            else -> "🐄"
        }
        binding.tvSelectedAnimal.text = "$emoji ${resolvedAnimal.profileName.ifEmpty { resolvedAnimal.category }}"
        binding.tvSelectedAnimal.setTextColor(Color.parseColor("#00FF88"))
        binding.tvAnimalNeeds.text =
            "KM:${fmtConstraint(resolvedAnimal.reqDmKg)}kg  NEL:${fmtConstraint(resolvedAnimal.reqNelMj)}MJ  HP:${resolvedAnimal.reqCpG.toInt()}g  " +
                "Ca:${resolvedAnimal.reqCaG.toInt()}g  P:${resolvedAnimal.reqPG.toInt()}g"

        applySpeciesConstraintDefaults(resolvedAnimal, selectedBalanceMode.profileMode)
        updateManualNeedBars()
        refreshPreparationUi()
    }

    private fun showAnimalPicker() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, AnimalListFragment.newInstance(selectionMode = true))
            .addToBackStack(null)
            .commit()
    }

    private fun showFeedPicker() {
        val animal = selectedAnimal
        if (animal == null) return
        lifecycleScope.launch {
            val allFeeds = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).feedDao().getAllFeedsSnapshot()
            }
            // SmartFeedSelector ile hayvan türüne özgü yem seçimi
            val chosen = SmartFeedSelector.buildCandidatePool(
                animal = animal,
                allFeeds = allFeeds,
                config = SmartFeedSelector.SelectionConfig(
                    limit = 15,
                    attempt = 0,
                    allowedCategories = when (animal.species) {
                        AnimalSpecies.KANATLI -> setOf(
                            FeedCategories.GRAIN, FeedCategories.PROTEIN,
                            FeedCategories.BYPRODUCT, FeedCategories.FAT,
                            FeedCategories.MINERAL, FeedCategories.VITAMIN,
                            FeedCategories.PREMIKS, FeedCategories.ADDITIVE
                        )
                        else -> setOf(
                            FeedCategories.ROUGHAGE_WET, FeedCategories.ROUGHAGE_DRY,
                            FeedCategories.GRAIN, FeedCategories.PROTEIN,
                            FeedCategories.BYPRODUCT, FeedCategories.FAT,
                            FeedCategories.MINERAL, FeedCategories.VITAMIN,
                            FeedCategories.PREMIKS, FeedCategories.ADDITIVE
                        )
                    },
                    requiredCategories = emptySet(),
                    categoryCaps = linkedMapOf(
                        FeedCategories.ROUGHAGE_WET to 2,
                        FeedCategories.ROUGHAGE_DRY to 2,
                        FeedCategories.GRAIN to 3,
                        FeedCategories.PROTEIN to 2,
                        FeedCategories.BYPRODUCT to 2,
                        FeedCategories.FAT to 1,
                        FeedCategories.MINERAL to 1,
                        FeedCategories.PREMIKS to 1,
                        FeedCategories.VITAMIN to 1,
                        FeedCategories.ADDITIVE to 0
                    )
                )
            )
            candidateFeeds.clear()
            candidateFeeds.addAll(chosen)
            selectedFeeds.removeAll { it.feed.id !in chosen.map { f -> f.id } }
            candidateFeedAdapter.notifyDataSetChanged()
            selectedFeedAdapter.notifyDataSetChanged()
            updateFeedCount()
            refreshPreparationUi()
            Toast.makeText(requireContext(), "${chosen.size} yem aday havuza eklendi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleFinalFeed(feed: Feed) {
        val existingIndex = selectedFeeds.indexOfFirst { it.feed.id == feed.id }
        if (existingIndex >= 0) {
            selectedFeeds.removeAt(existingIndex)
            selectedFeedAdapter.notifyDataSetChanged()
            updateFeedCount()
            refreshPreparationUi()
            Toast.makeText(requireContext(), "${feed.name} final rasyondan çıkarıldı", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedFeeds.size >= 14) {
            Toast.makeText(requireContext(), "Final rasyon en fazla 14 yem olabilir", Toast.LENGTH_SHORT).show()
            return
        }

        selectedFeeds.add(SelectedFeedInput(feed = feed, amountKg = 0.0))
        selectedFeedAdapter.notifyDataSetChanged()
        updateFeedCount()
        refreshPreparationUi()
        Toast.makeText(requireContext(), "${feed.name} final rasyona eklendi", Toast.LENGTH_SHORT).show()
    }

    private fun updateFeedCount() {
        binding.tvSelectedFeedCount.text = "${candidateFeeds.size} aday yem"
        binding.tvSelectedFeedPreview.text = if (candidateFeeds.isEmpty()) {
            "Aday havuz bos"
        } else {
            preparationCoordinator.buildSelectionSummary(candidateFeeds)
        }
        binding.tvFinalFeedCount.text = "${selectedFeeds.size} final yem"
        binding.tvFinalFeedPreview.text = if (selectedFeeds.isEmpty()) {
            "Final rasyon bos"
        } else {
            preparationCoordinator.buildSelectionSummary(selectedFeeds.map { it.feed })
        }
        candidateFeedAdapter.notifyDataSetChanged()
        updateManualNeedBars()
    }

    private fun updateManualNeedBars() {
        val animal = selectedAnimal
        if (animal == null) {
            setNeedBar(binding.pbDm, binding.tvDmCoverage, "DM", 0.0, 0.0)
            setNeedBar(binding.pbNel, binding.tvNelCoverage, "NEL", 0.0, 0.0)
            setNeedBar(binding.pbCp, binding.tvCpCoverage, "HP", 0.0, 0.0)
            setNeedBar(binding.pbCa, binding.tvCaCoverage, "Ca", 0.0, 0.0)
            setNeedBar(binding.pbP, binding.tvPCoverage, "P", 0.0, 0.0)
            return
        }

        val valid = selectedFeeds.filter { it.amountKg > 0.0 && it.feed.dm > 0.0 }
        val dmKg = valid.sumOf { it.amountKg * (it.feed.dm / 100.0) }
        val nel = valid.sumOf { (it.amountKg * (it.feed.dm / 100.0)) * it.feed.nel }
        val cp = valid.sumOf { (it.amountKg * (it.feed.dm / 100.0)) * it.feed.cp * 10.0 }
        val ca = valid.sumOf { (it.amountKg * (it.feed.dm / 100.0)) * it.feed.ca * 10.0 }
        val p = valid.sumOf { (it.amountKg * (it.feed.dm / 100.0)) * it.feed.p * 10.0 }

        setNeedBar(binding.pbDm, binding.tvDmCoverage, "DM", dmKg, animal.reqDmKg)
        setNeedBar(binding.pbNel, binding.tvNelCoverage, "NEL", nel, animal.reqNelMj)
        setNeedBar(binding.pbCp, binding.tvCpCoverage, "HP", cp, animal.reqCpG)
        setNeedBar(binding.pbCa, binding.tvCaCoverage, "Ca", ca, animal.reqCaG)
        setNeedBar(binding.pbP, binding.tvPCoverage, "P", p, animal.reqPG)
    }

    private fun setNeedBar(
        bar: android.widget.ProgressBar,
        label: android.widget.TextView,
        name: String,
        actual: Double,
        target: Double
    ) {
        val pct = if (target > 0) (actual / target * 100.0) else 0.0
        bar.progress = pct.coerceIn(0.0, 150.0).toInt()
        label.text = "$name: ${fmtConstraint(actual)}/${fmtConstraint(target)} (${fmtConstraint(pct)}%)"
    }

    private fun tryAutoSelectLatestAnimal() {
        lifecycleScope.launch {
            val latest = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).animalProfileDao().getAllSync().firstOrNull()
            }

            if (!isAdded || _binding == null) return@launch
            if (selectedAnimal == null && latest != null) {
                setAnimal(latest, resetSelections = true)
                Toast.makeText(requireContext(), "Son kullanilan hayvan secildi.", Toast.LENGTH_SHORT).show()
            } else {
                refreshPreparationUi()
            }
        }
    }

    private fun buildConstraintPresetSummary(animal: AnimalProfile): String {
        return RationStandards.presetSummaryFor(animal.species, selectedBalanceMode.profileMode)
    }

    private fun refreshPreparationUi() {
        val hasAnimal = selectedAnimal != null
        val candidateCount = candidateFeeds.size
        val finalCount = selectedFeeds.size

        binding.btnAddFeeds.isEnabled = true
        binding.btnAddFeeds.alpha = 1f
        binding.btnCalculate.isEnabled = true
        binding.btnCalculate.alpha = if (hasAnimal && preparationCoordinator.isFinalSelectionReady(finalCount)) 1f else 0.65f
        binding.btnOptimize.isEnabled = true
        binding.btnOptimize.alpha = if (hasAnimal) 1f else 0.85f
        binding.btnSelectAnimal.text = if (hasAnimal) "DEGISTIR ->" else "SEC / EKLE ->"

        binding.tvRationSubtitle.text = preparationCoordinator.buildStepHint(selectedAnimal, candidateCount, finalCount)

        if (!hasAnimal) {
            binding.tvSelectedAnimal.text = "Adim 1: Hayvan grubu seciniz..."
            binding.tvSelectedAnimal.setTextColor(Color.parseColor("#93A59A"))
            binding.tvAnimalNeeds.text = ""
            binding.tvSelectedFeedPreview.text = "Aday havuz icin YEMLER butonuna basin"
            binding.tvFinalFeedPreview.text = "Final rasyon burada gorunur"
        } else {
            val preset = buildConstraintPresetSummary(selectedAnimal!!)
            binding.tvAnimalNeeds.text =
                "Aday havuz: $candidateCount yem | Final rasyon: $finalCount yem\n" +
                    "${selectedBalanceMode.label} preset: $preset\n" +
                    "Kalibrasyon: ${RationStandards.CALIBRATION_PACKAGE_VERSION} (${RationStandards.CALIBRATION_PACKAGE_DATE})"
        }
    }

    private suspend fun resolveAnimalForAction(): AnimalProfile? {
        selectedAnimal?.let { return it }
        val latest = withContext(Dispatchers.IO) {
            AppDatabase.getDatabase(requireContext()).animalProfileDao().getAllSync().firstOrNull()
        }
        if (latest != null && isAdded && _binding != null) {
            setAnimal(latest, resetSelections = false)
        }
        return latest
    }

    private fun runManualCalculation() {
        lifecycleScope.launch {
            val animal = resolveAnimalForAction()
            if (animal == null) {
                Toast.makeText(requireContext(), "Hayvan grubu bulunamadi!", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (selectedFeeds.isEmpty()) {
                Toast.makeText(requireContext(), "Final rasyona en az 1 yem ekleyin.", Toast.LENGTH_LONG).show()
                return@launch
            }
            if (animal.reqDmKg <= 0) {
                Toast.makeText(requireContext(), "Hayvan ihtiyaclari hesaplanmamis! Once NRC hesaplayin.", Toast.LENGTH_LONG).show()
                return@launch
            }

            val filled = selectedFeeds.filter { it.amountKg > 0.0 }
            if (filled.isEmpty()) {
                Toast.makeText(requireContext(), "Hesapla icin secili yemlere miktar girin (kg/gun).", Toast.LENGTH_LONG).show()
                return@launch
            }

            val productPrice = binding.etProductPrice.text.toString().toDoubleOrNull() ?: 18.0
            val rationName = binding.etRationName.text.toString().trim().ifEmpty { "Manuel Rasyon ${System.currentTimeMillis()}" }

            val result = withContext(Dispatchers.Default) {
                RationOptimizer.evaluateManual(
                    ManualInput(
                        animal = animal,
                        feeds = filled.map { ManualFeedInput(feed = it.feed, amountKg = it.amountKg) },
                        milkPrice = productPrice
                    )
                )
            }

            val resultFragment = RationResultFragment.newInstance(result, rationName, animal, emptyList())
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, resultFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun runOptimization() {
        lifecycleScope.launch {
            val animal = resolveAnimalForAction()
            if (animal == null) {
                Toast.makeText(requireContext(), "Hayvan grubu bulunamadi!", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (animal.reqDmKg <= 0) {
                Toast.makeText(requireContext(), "Hayvan ihtiyaclari hesaplanmamis! Once NRC hesaplayin.", Toast.LENGTH_LONG).show()
                return@launch
            }

            val allFeeds = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).feedDao().getAllFeedsSnapshot()
            }
            // Feeds buttonundan secilen adaylar fabrika stok havuzu olarak kabul edilir.
            // Kullanici secim yaptiysa optimize sadece bu havuzdan calisir.
            val stockPool = if (candidateFeeds.isNotEmpty()) candidateFeeds.toList() else allFeeds
            val candidateAttempts = preparationCoordinator.buildOptimizationCandidateAttempts(
                animal = animal,
                allFeeds = stockPool,
                attempts = 12,
                limit = 16
            )

            if (candidateAttempts.isEmpty()) {
                val msg = if (candidateFeeds.isNotEmpty()) {
                    "Secilen fabrika stok havuzunda bu tur icin uygun yem bulunamadi. Yem havuzunu genisletin."
                } else {
                    "Otomatik motor icin uygun yem bulunamadi. Once fabrika stokundan yem secin."
                }
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                return@launch
            }

            val productPrice = binding.etProductPrice.text.toString().toDoubleOrNull() ?: 18.0
            val rationName = binding.etRationName.text.toString().trim().ifEmpty { "Rasyon ${System.currentTimeMillis()}" }
            val ndfMin = binding.etNdfMin.text.toString().toDoubleOrNull() ?: 0.0
            val ndfMax = binding.etNdfMax.text.toString().toDoubleOrNull() ?: 0.0
            val cpMax = binding.etCpMax.text.toString().toDoubleOrNull() ?: 0.0
            val roughageMin = binding.etRoughageMin.text.toString().toDoubleOrNull() ?: 0.0
            val starchMax = binding.etStarchMax.text.toString().toDoubleOrNull() ?: 0.0
            val sugarMax = binding.etSugarMax.text.toString().toDoubleOrNull() ?: 0.0
            val fatMax = binding.etFatMax.text.toString().toDoubleOrNull() ?: 0.0

            binding.btnOptimize.isEnabled = false
            var bestResult: RationResult? = null
            var bestCandidates: List<Feed> = emptyList()
            var fallbackResult: RationResult? = null
            var fallbackCandidates: List<Feed> = emptyList()
            var fallbackPenalty = Double.MAX_VALUE
            var hasProteinMineralExcessReject = false
            val rejectNotes = mutableListOf<String>()

            try {
                for ((idx, candidates) in candidateAttempts.withIndex()) {
                    val feasibility = FeasibilityChecker.check(animal, candidates)
                    if (!feasibility.isPossible) {
                        val detailedMsg = buildString {
                            append("Rasyon kurulamadı!\n")
                            if (feasibility.errors.isNotEmpty()) {
                                append("Hatalar:\n")
                                feasibility.errors.forEach { append("- $it\n") }
                            }
                            if (feasibility.warnings.isNotEmpty()) {
                                append("Uyarılar:\n")
                                feasibility.warnings.forEach { append("- $it\n") }
                            }
                            if (feasibility.suggestions.isNotEmpty()) {
                                append("Öneriler:\n")
                                feasibility.suggestions.forEach { append("- $it\n") }
                            }
                        }
                        Toast.makeText(requireContext(), detailedMsg, Toast.LENGTH_LONG).show()
                        rejectNotes.add("Deneme ${idx + 1}: feasibility basarisiz")
                        continue
                    }

                    val optimizationInput = OptimizationInput(
                        animal = animal,
                        feeds = candidates,
                        milkPrice = productPrice,
                        customNdfMin = ndfMin,
                        customNdfMax = ndfMax,
                        customCpMaxPct = cpMax,
                        customRoughageMinPct = roughageMin,
                        customStarchMaxPct = starchMax,
                        customSugarMaxPct = sugarMax,
                        customFatMaxPct = fatMax,
                        balanceMode = selectedBalanceMode.profileMode
                    )

                    val baseResult = withContext(Dispatchers.Default) {
                        RationOptimizer.optimize(optimizationInput)
                    }

                    val result = if (baseResult.status == "INFEASIBLE") {
                        val detailedMsg = buildString {
                            append("Brill LP çözüm bulunamadı!\n")
                            append("\n")
                            append("Brill LP motorundan gelen mesaj: ${baseResult.message}\n")
                            // Son feasibility kontrolünü tekrar yapıp detayları gösterelim
                            val feasibility = FeasibilityChecker.check(animal, candidates)
                            if (feasibility.errors.isNotEmpty()) {
                                append("Hatalar:\n")
                                feasibility.errors.forEach { append("- $it\n") }
                            }
                            if (feasibility.warnings.isNotEmpty()) {
                                append("Uyarılar:\n")
                                feasibility.warnings.forEach { append("- $it\n") }
                            }
                            if (feasibility.suggestions.isNotEmpty()) {
                                append("Öneriler:\n")
                                feasibility.suggestions.forEach { append("- $it\n") }
                            }
                        }
                        Toast.makeText(requireContext(), detailedMsg, Toast.LENGTH_LONG).show()
                        baseResult
                    } else {
                        baseResult
                    }

                    // Hard constraint ihlali varsa reject et, fallback için de almaz
                    if (result.status == "CONSTRAINT_FAILED") {
                        rejectNotes.add("Deneme ${idx + 1}: NRC hard kısıtlamalar ihlal edildi")
                        continue
                    }

                    val check = BrillQualityGate.assess(result, animal)
                    if (!check.passes) {
                        val penalty = (100 - check.score).coerceAtLeast(0) + (100.0 - result.yield.coveragePct).coerceAtLeast(0.0)
                        if (penalty < fallbackPenalty) {
                            fallbackPenalty = penalty
                            fallbackResult = result
                            fallbackCandidates = candidates
                        }

                        val excessReject = check.reasons.any {
                            it.startsWith("HP fazla") ||
                                it.startsWith("Ca fazla") ||
                                it.startsWith("P fazla") ||
                                it.startsWith("Mg fazla")
                        }
                        hasProteinMineralExcessReject = hasProteinMineralExcessReject || excessReject

                        val advisory = if (excessReject) {
                            " -> Son secilen rasyon yemlerini degistirin; protein/mineral yogun yemleri azaltin. Gerekirse aday havuzu artirin."
                        } else ""
                        rejectNotes.add("Deneme ${idx + 1}: ${check.reasons.joinToString("; ")}$advisory")
                        continue
                    }

                    if (bestResult == null || result.yield.coveragePct > (bestResult?.yield?.coveragePct ?: 0.0)) {
                        bestResult = result
                        bestCandidates = candidates
                    }

                    if ((result.status == "OPTIMAL" || result.status == "OPTIMAL_HIGHS") && result.yield.coveragePct >= 97.0) {
                        break
                    }
                }

                var finalResult = bestResult
                var finalCandidates = bestCandidates
                val fallbackHasProteinMineralExcess = fallbackResult?.let { result ->
                    BrillQualityGate.assess(result, animal).reasons.any {
                        it.startsWith("HP fazla") ||
                            it.startsWith("Ca fazla") ||
                            it.startsWith("P fazla") ||
                            it.startsWith("Mg fazla")
                    }
                } ?: false

                if (finalResult == null && fallbackResult != null && !fallbackHasProteinMineralExcess) {
                    finalResult = fallbackResult
                    finalCandidates = fallbackCandidates
                    val feedCount = finalResult.rationItems.size
                    Toast.makeText(
                        requireContext(),
                        "Brill kalite kapisi tam gecilemedi; en dusuk ihlalli cozum gosteriliyor ($feedCount yem).",
                        Toast.LENGTH_LONG
                    ).show()
                }

                if (finalResult == null) {
                    val msg = if (rejectNotes.isNotEmpty()) {
                        "Brill kalite kapisi gecilemedi:\n${rejectNotes.take(3).joinToString("\n")}" +
                            if (hasProteinMineralExcessReject || fallbackHasProteinMineralExcess) {
                                "\nOneri: Son secilen rasyon yemlerini degistirin; protein/mineral yogun yemleri azaltin ve aday havuzu genisletin."
                            } else ""
                    } else {
                        "Otomatik mod uygun çözüm bulamadı. Kısıtları gözden geçirin."
                    }
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    return@launch
                }

                candidateFeeds.clear()
                candidateFeeds.addAll(finalCandidates)
                candidateFeedAdapter.notifyDataSetChanged()
                updateFeedCount()

                val alternatives = withContext(Dispatchers.Default) {
                    buildAlternativeSummaries(
                        animal = animal,
                        feeds = finalCandidates,
                        productPrice = productPrice,
                        ndfMin = ndfMin,
                        ndfMax = ndfMax,
                        cpMax = cpMax,
                        roughageMin = roughageMin,
                        starchMax = starchMax,
                        sugarMax = sugarMax,
                        fatMax = fatMax
                    )
                }

                val resultFragment = RationResultFragment.newInstance(finalResult, rationName, animal, alternatives)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, resultFragment)
                    .addToBackStack(null)
                    .commit()
            } catch (e: Exception) {
                android.util.Log.e("RationFragment", "Optimizer run failed", e)
                val msg = e.message ?: e.toString()
                Toast.makeText(requireContext(), "Hata: $msg", Toast.LENGTH_LONG).show()
            } finally {
                if (isAdded && _binding != null) binding.btnOptimize.isEnabled = true
            }
        }
    }

    private fun buildAlternativeSummaries(
        animal: AnimalProfile,
        feeds: List<Feed>,
        productPrice: Double,
        ndfMin: Double,
        ndfMax: Double,
        cpMax: Double,
        roughageMin: Double,
        starchMax: Double,
        sugarMax: Double,
        fatMax: Double
    ): List<AlternativeRationSummary> {
        if (feeds.isEmpty()) return emptyList()

        val modes = listOf(
            SolverBalanceMode.LOW_COST to "En Ucuz",
            SolverBalanceMode.BALANCED to "En Dengeli"
        )

        return modes.map { (mode, title) ->
            val res = RationOptimizer.optimize(
                OptimizationInput(
                    animal = animal,
                    feeds = feeds,
                    milkPrice = productPrice,
                    customNdfMin = ndfMin,
                    customNdfMax = ndfMax,
                    customCpMaxPct = cpMax,
                    customRoughageMinPct = roughageMin,
                    customStarchMaxPct = starchMax,
                    customSugarMaxPct = sugarMax,
                    customFatMaxPct = fatMax,
                    balanceMode = mode
                )
            )
            AlternativeRationSummary(
                title = title,
                costPerDay = res.nutrient.totalCost,
                netProfit = res.yield.netProfit,
                coveragePct = res.yield.coveragePct,
                status = res.status
            )
        }
    }

    private fun applySpeciesConstraintDefaults(
        animal: AnimalProfile,
        mode: SolverBalanceMode = SolverBalanceMode.BALANCED
    ) {
        val profile = SpeciesOptimizationProfiles.forAnimal(animal)
        val standards = RationStandards.constraintsFor(animal.species, mode)

        val defaultNdfMin = when {
            animal.reqNdfPct > 0 -> animal.reqNdfPct
            else -> standards.ndfMinPct
        }
        val defaultNdfMax = standards.ndfMaxPct
        val defaultRoughageMin = standards.roughageMinPct
        val defaultCpMax = standards.cpMaxPct

        binding.etNdfMin.setText(if (defaultNdfMin > 0) fmtConstraint(defaultNdfMin) else "")
        binding.etNdfMax.setText(if (defaultNdfMax > 0) fmtConstraint(defaultNdfMax) else "")
        binding.etRoughageMin.setText(if (defaultRoughageMin > 0) fmtConstraint(defaultRoughageMin) else "")
        binding.etCpMax.setText(if (defaultCpMax > 0) fmtConstraint(defaultCpMax) else "")
        binding.etStarchMax.setText(if (standards.starchMaxPct > 0) fmtConstraint(standards.starchMaxPct) else if (profile.starchMaxPct > 0) fmtConstraint(profile.starchMaxPct) else "")
        binding.etSugarMax.setText(if (standards.sugarMaxPct > 0) fmtConstraint(standards.sugarMaxPct) else if (profile.sugarMaxPct > 0) fmtConstraint(profile.sugarMaxPct) else "")
        binding.etFatMax.setText(if (standards.fatMaxPct > 0) fmtConstraint(standards.fatMaxPct) else if (profile.fatMaxPct > 0) fmtConstraint(profile.fatMaxPct) else "")
    }

    private fun fmtConstraint(value: Double): String {
        return if (kotlin.math.abs(value - value.toInt()) < 0.001) value.toInt().toString() else String.format("%.1f", value)
    }

    private fun finishRation() {
        val rationName = binding.etRationName.text.toString().trim()
        if (rationName.isEmpty()) {
            Toast.makeText(requireContext(), "Lutfen rasyon adi girin", Toast.LENGTH_SHORT).show()
            return
        }

        val feedsToConsume = selectedFeeds
            .map { it.copy() }
            .filter { it.amountKg > 0.0 }

        if (feedsToConsume.isEmpty()) {
            Toast.makeText(requireContext(), "Stok dusumu icin miktar girin", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val now = System.currentTimeMillis()

                withContext(Dispatchers.IO) {
                    feedsToConsume.forEach { selectedFeed ->
                        val currentStock = db.stockDao().getByFeedId(selectedFeed.feed.id) ?: return@forEach
                        val consumedKg = selectedFeed.amountKg.coerceAtMost(currentStock.currentStockKg).coerceAtLeast(0.0)
                        if (consumedKg <= 0.0) return@forEach

                        val newAmount = (currentStock.currentStockKg - consumedKg).coerceAtLeast(0.0)
                        val unitPrice = if (currentStock.lastPurchasePrice > 0) currentStock.lastPurchasePrice else selectedFeed.feed.pricePerKg
                        val newValue = newAmount * unitPrice
                        db.stockDao().updateStock(currentStock.id, newAmount, newValue, now)

                        val daysRemaining = if (currentStock.dailyUsageKg > 0) {
                            kotlin.math.ceil(newAmount / currentStock.dailyUsageKg).toInt().coerceAtLeast(0)
                        } else 0
                        db.stockDao().updateUsageEstimate(currentStock.id, currentStock.dailyUsageKg, daysRemaining)

                        db.stockDao().insertTransaction(
                            StockTransaction(
                                stockItemId = currentStock.id,
                                feedName = currentStock.feedName,
                                feedCategory = currentStock.feedCategory,
                                type = TransactionType.USAGE,
                                amountKg = -consumedKg,
                                pricePerKg = unitPrice,
                                totalCost = consumedKg * unitPrice,
                                stockAfter = newAmount,
                                date = now,
                                notes = "Rasyon bitir: $rationName"
                            )
                        )
                    }
                }

                binding.etRationName.setText("")
                selectedFeeds.clear()
                selectedFeedAdapter.notifyDataSetChanged()
                updateFeedCount()
                refreshPreparationUi()

                Toast.makeText(requireContext(), "Rasyon tamamlandi. Stok ve rapor verileri guncellendi", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.util.Log.e("RationFragment", "Finish ration failed", e)
                val msg = e.message ?: e.toString()
                Toast.makeText(requireContext(), "Hata: $msg", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}