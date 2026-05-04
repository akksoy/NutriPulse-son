package com.nutripulse.app.ui.ration

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nutripulse.app.R
import com.nutripulse.app.data.AppDatabase
import com.nutripulse.app.data.model.AnimalProfile
import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories
import com.nutripulse.app.data.model.RationPreset
import com.nutripulse.app.data.model.RationPresets
import com.nutripulse.app.data.solver.FeasibilityChecker
import com.nutripulse.app.data.solver.OptimizationInput
import com.nutripulse.app.data.solver.RationOptimizer
import com.nutripulse.app.databinding.FragmentRationStudioBinding
import com.nutripulse.app.ui.animal.AnimalListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RationStudioFragment : Fragment() {

    private var _binding: FragmentRationStudioBinding? = null
    private val binding get() = _binding!!

    private var selectedAnimal: AnimalProfile? = null
    private var selectedFeeds = mutableListOf<SelectedFeedInput>()
    private var feedMinOverrides = mutableMapOf<Int, Double>()   // feedId → min kg
    private var feedMaxOverrides = mutableMapOf<Int, Double>()   // feedId → max kg
    private var activePreset: RationPreset? = null
    private var constraintsVisible = true
    private var rationMode: String = "TMR" // "TMR" veya "FABRIKA"

    // Preview adapter (sadece seçili yemler mini listesi)
    private lateinit var previewAdapter: SelectedFeedAdapter

    companion object {
        fun newInstance(animalId: Int = -1) = RationStudioFragment().apply {
            arguments = Bundle().apply { putInt("animal_id", animalId) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRationStudioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPreviewList()
        buildPresetCards()
        setupFeedPoolResult()
        setupAnimalPickerResult()
        setupButtons()
        setupConstraintToggle()

        val animalId = arguments?.getInt("animal_id", -1) ?: -1
        if (animalId > 0) loadAnimal(animalId)

        // Varsayılan preset: Dengeli
        selectPreset(RationPresets.MILK_BALANCED)

        // Mod seçici Spinner
        val modes = listOf("TMR", "Fabrika")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, modes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRationMode.adapter = adapter
        binding.spinnerRationMode.setSelection(0)
        binding.spinnerRationMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                rationMode = if (position == 0) "TMR" else "FABRIKA"
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    // ── Preset kartları ──────────────────────────────────────
    private fun buildPresetCards() {
        val species = selectedAnimal?.species ?: "SIGIR"
        val presets = RationPresets.forSpecies(species)

        binding.presetCardBar.removeAllViews()
        presets.forEach { preset ->
            val card = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_preset_card, binding.presetCardBar, false)

            card.findViewById<TextView>(R.id.tvPresetCardEmoji).text = preset.emoji
            card.findViewById<TextView>(R.id.tvPresetCardName).text  = preset.name

            val isActive = preset.id == activePreset?.id
            card.setBackgroundResource(
                if (isActive) R.drawable.bg_stat_green else R.drawable.bg_feed_item)
            card.findViewById<TextView>(R.id.tvPresetCardName).setTextColor(
                Color.parseColor(if (isActive) "#05080A" else "#C0E8D0"))
            card.findViewById<TextView>(R.id.tvPresetCardEmoji).setTextColor(
                Color.parseColor(if (isActive) "#05080A" else "#FFFFFF"))

            card.setOnClickListener { selectPreset(preset) }
            binding.presetCardBar.addView(card)
        }
    }

    private fun selectPreset(preset: RationPreset) {
        activePreset = preset

        // Kısıt alanlarını preset değerleriyle doldur
        binding.etNdfMin.setText(preset.ndfMin.toString())
        binding.etNdfMax.setText(preset.ndfMax.toString())
        binding.etPeNdfMin.setText(preset.peNdfMin.toString())
        binding.etCpMax.setText(preset.cpMax.toString())
        binding.etRoughageMin.setText(preset.roughageMinPct.toString())
        binding.etStarchMax.setText(preset.starchMax.toString())
        binding.etSugarMax.setText(preset.sugarMax.toString())
        binding.etFatMax.setText(preset.fatMax.toString())
        binding.etCaPMin.setText(preset.caPRatioMin.toString())
        binding.etCaPMax.setText(preset.caPRatioMax.toString())
        binding.etDcadMin.setText(preset.dcadMin.toString())
        binding.etUreaMax.setText(preset.ureaMaxKg.toString())

        // Preset bilgi kartı
        binding.layoutPresetInfo.visibility = View.VISIBLE
        binding.tvPresetName.text = "${preset.emoji}  ${preset.name}"
        binding.tvPresetName.setTextColor(Color.parseColor(preset.color))
        binding.tvPresetDesc.text = preset.description
        binding.tvPresetConstraints.text =
            "NDF: ${preset.ndfMin.toInt()}–${preset.ndfMax.toInt()}%  " +
            "HP max: ${preset.cpMax.toInt()}%  " +
            "Nişasta max: ${preset.starchMax.toInt()}%  " +
            "peNDF min: ${preset.peNdfMin.toInt()}%"
        binding.tvPresetCalibration.text = "Kalibrasyon: ${preset.calibrationVersion}"

        // Kartları yeniden çiz
        buildPresetCards()
    }

    // ── Yem havuzu sonucu ────────────────────────────────────
    private fun setupFeedPoolResult() {
        parentFragmentManager.setFragmentResultListener(
            FeedPoolFragment.RESULT_KEY, viewLifecycleOwner
        ) { _, bundle ->
            val feedIds = bundle.getIntArray("feed_ids") ?: return@setFragmentResultListener
            val mins    = bundle.getDoubleArray("feed_mins") ?: DoubleArray(feedIds.size)
            val maxes   = bundle.getDoubleArray("feed_maxes") ?: DoubleArray(feedIds.size) { -1.0 }

            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(requireContext())
                val feeds = withContext(Dispatchers.IO) {
                    val loaded = mutableListOf<Feed>()
                    feedIds.forEach { id ->
                        db.feedDao().getFeedById(id)?.let { loaded.add(it) }
                    }
                    loaded
                }
                selectedFeeds = feeds.map { SelectedFeedInput(feed = it) }.toMutableList()
                feedMinOverrides.clear()
                feedMaxOverrides.clear()
                feedIds.forEachIndexed { idx, id ->
                    if (mins.getOrElse(idx) { 0.0 } > 0) {
                        feedMinOverrides[id] = mins[idx]
                        selectedFeeds.find { it.feed.id == id }?.amountKg = mins[idx]
                    }
                    if (maxes.getOrElse(idx) { -1.0 } > 0) feedMaxOverrides[id] = maxes[idx]
                }
                updatePoolPreview()
                highlightStep(3)
            }
        }
    }

    // ── Hayvan seçimi sonucu ─────────────────────────────────
    private fun setupAnimalPickerResult() {
        parentFragmentManager.setFragmentResultListener(
            RationFragment.REQUEST_SELECT_ANIMAL, viewLifecycleOwner
        ) { _, bundle ->
            val id = bundle.getInt(RationFragment.KEY_ANIMAL_ID, -1)
            if (id > 0) loadAnimal(id)
        }
    }

    private fun loadAnimal(id: Int) {
        lifecycleScope.launch {
            val animal = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).animalProfileDao().getById(id)
            }
            animal?.let { setAnimal(it) }
        }
    }

    private fun setAnimal(animal: AnimalProfile) {
        selectedAnimal = animal
        val emoji = emojiFor(animal.species)
        binding.tvStudioAnimalEmoji.text = emoji
        binding.tvStudioAnimalName.text =
            animal.profileName.ifEmpty { categoryDisplayName(animal.category) }
        binding.tvStudioAnimalCategory.text =
            "${animal.animalCount} baş  •  ${animal.breed.ifEmpty { animal.species }}"

        if (animal.reqDmKg > 0) {
            binding.layoutNrcSummary.visibility = View.VISIBLE
            binding.tvNrcDm.text  = "${animal.reqDmKg} kg"
            binding.tvNrcNel.text = "${animal.reqNelMj} MJ"
            binding.tvNrcCp.text  = "${animal.reqCpG.toInt()} g"
            binding.tvNrcCa.text  = "${animal.reqCaG.toInt()} g"
            binding.tvNrcP.text   = "${animal.reqPG.toInt()} g"
        } else {
            binding.layoutNrcSummary.visibility = View.GONE
            binding.tvStudioAnimalCategory.text =
                binding.tvStudioAnimalCategory.text.toString() +
                "  ⚠ NRC hesaplanmamış"
        }

        // Hayvana uygun presetleri yeniden yükle
        buildPresetCards()
        highlightStep(1)

        // Varsayılan süt fiyatını çiftlik profilinden doldur
        lifecycleScope.launch {
            val farm = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).farmProfileDao().getProfileSync()
            }
            farm?.let { binding.etProductPrice.setText(it.defaultMilkPrice.toString()) }
        }
    }

    // ── Butonlar ─────────────────────────────────────────────
    private fun setupButtons() {
        binding.btnChangeAnimal.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AnimalListFragment.newInstance(selectionMode = true))
                .addToBackStack(null).commit()
        }
        binding.btnOpenPool.setOnClickListener {
            highlightStep(2)
            val selectedIds = selectedFeeds.map { it.feed.id }.toIntArray()
            val species = selectedAnimal?.species
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, FeedPoolFragment.newInstance(animalSpecies = species, selectedFeedIds = selectedIds, rationMode = rationMode))
                .addToBackStack(null).commit()
        }

        binding.btnRunOptimizer.setOnClickListener { runOptimize() }

        binding.btnManualMode.setOnClickListener {
            Toast.makeText(requireContext(),
                "Manuel mod — yakında!", Toast.LENGTH_SHORT).show()
        }

        binding.btnOptimizeMode.setOnClickListener { runOptimize() }
    }

    private fun setupConstraintToggle() {
        binding.btnToggleConstraints.setOnClickListener {
            constraintsVisible = !constraintsVisible
            binding.layoutConstraints.visibility =
                if (constraintsVisible) View.VISIBLE else View.GONE
            binding.btnToggleConstraints.text =
                if (constraintsVisible) "▲ GİZLE" else "▼ GÖR"
        }
    }

    // ── Pool preview ─────────────────────────────────────────
    private fun setupPreviewList() {
        previewAdapter = SelectedFeedAdapter(
            selectedFeeds,
            onRemove = { item ->
                selectedFeeds.remove(item)
                feedMinOverrides.remove(item.feed.id)
                feedMaxOverrides.remove(item.feed.id)
                previewAdapter.notifyDataSetChanged()
                updatePoolPreview()
            },
            onAmountChanged = { item ->
                if (item.amountKg > 0) feedMinOverrides[item.feed.id] = item.amountKg
                else feedMinOverrides.remove(item.feed.id)
            }
        )
        binding.recyclerPoolPreview.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPoolPreview.adapter = previewAdapter
    }

    private fun updatePoolPreview() {
        previewAdapter.notifyDataSetChanged()
        if (selectedFeeds.isEmpty()) {
            binding.tvPoolEmpty.visibility = View.VISIBLE
            binding.recyclerPoolPreview.visibility = View.GONE
            binding.tvPoolSummary.text = "0 yem"
        } else {
            binding.tvPoolEmpty.visibility = View.GONE
            binding.recyclerPoolPreview.visibility = View.VISIBLE
            binding.tvPoolSummary.text = "${selectedFeeds.size} yem seçildi"
            binding.tvPoolSummary.setTextColor(Color.parseColor("#00FF88"))
        }
    }

    // ── Adım vurgulama ───────────────────────────────────────
    private fun highlightStep(step: Int) {
        val steps = listOf(binding.stepAnimal, binding.stepPool, binding.stepConstraints, binding.stepSolution)
        steps.forEachIndexed { idx, layout ->
            if (idx < step) {
                layout.setBackgroundResource(R.drawable.bg_stat_green)
                (layout.getChildAt(1) as TextView).setTextColor(Color.parseColor("#00FF88"))
            } else if (idx == step - 1) {
                layout.setBackgroundResource(R.drawable.bg_module_yellow)
                (layout.getChildAt(1) as TextView).setTextColor(Color.parseColor("#FFC400"))
            } else {
                layout.setBackgroundResource(R.drawable.bg_feed_item)
                (layout.getChildAt(1) as TextView).setTextColor(Color.parseColor("#3A5A48"))
            }
        }
    }

    // ── Optimizasyon ─────────────────────────────────────────
    private fun runOptimize() {
        val animal = selectedAnimal
        if (animal == null) {
            Toast.makeText(requireContext(), "⚠ Hayvan grubu seçin!", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedFeeds.isEmpty()) {
            Toast.makeText(requireContext(), "⚠ Yem havuzu oluşturun!", Toast.LENGTH_SHORT).show()
            return
        }

        val feedsOnly = selectedFeeds.map { it.feed }

        // Önce feasibility kontrol
        val check = FeasibilityChecker.check(animal, feedsOnly, rationMode)
        if (!check.isPossible) {
            Toast.makeText(requireContext(),
                check.errors.firstOrNull() ?: "Çözüm mümkün değil",
                Toast.LENGTH_LONG).show()
            return
        }

        val preset = activePreset ?: RationPresets.MILK_BALANCED
        val productPrice = binding.etProductPrice.text.toString().toDoubleOrNull() ?: 18.0
        val rationName   = binding.etRationName.text.toString().trim()
            .ifEmpty { "${animal.profileName.ifEmpty { animal.category }} — ${preset.name}" }

        // Kısıtları oku
        val ndfMin     = binding.etNdfMin.text.toString().toDoubleOrNull()     ?: preset.ndfMin
        val ndfMax     = binding.etNdfMax.text.toString().toDoubleOrNull()     ?: preset.ndfMax
        val cpMax      = binding.etCpMax.text.toString().toDoubleOrNull()      ?: preset.cpMax
        // Fabrika modunda kaba yem kısıtlaması yok
        val roughMin   = if (rationMode == "FABRIKA") {
                            0.0
                        } else {
                            binding.etRoughageMin.text.toString().toDoubleOrNull() ?: preset.roughageMinPct
                        }

        // Per-yem min/max override'ları Feed entity'ye yaz
        val feedsWithOverrides = feedsOnly.map { f ->
            f.copy(
                minDailyKg = feedMinOverrides[f.id] ?: f.minDailyKg,
                maxDailyKg = feedMaxOverrides[f.id] ?: f.maxDailyKg
            )
        }

        highlightStep(4)
        binding.btnRunOptimizer.isEnabled = false
        (binding.btnRunOptimizer.getChildAt(0) as? TextView)?.text = "⏳ Hesaplanıyor..."

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    RationOptimizer.optimize(
                        OptimizationInput(
                            animal                = animal,
                            feeds                 = feedsWithOverrides,
                            milkPrice             = productPrice,
                            customNdfMin          = ndfMin,
                            customNdfMax          = ndfMax,
                            customCpMaxPct        = cpMax,
                            customRoughageMinPct  = roughMin,
                            rationMode            = rationMode
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    binding.btnRunOptimizer.isEnabled = true
                    (binding.btnRunOptimizer.getChildAt(0) as? TextView)?.text = "⚡ RASYONU BİTİR"

                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer,
                            RationResultFragment.newInstance(result, rationName, animal))
                        .addToBackStack(null).commit()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnRunOptimizer.isEnabled = true
                    (binding.btnRunOptimizer.getChildAt(0) as? TextView)?.text = "⚡ RASYONU BİTİR"
                    android.util.Log.e("RationStudioFragment", "Optimize failed", e)
                    val msg = e.message ?: e.toString()
                    Toast.makeText(requireContext(), "Hata: $msg", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ── Yardımcılar ──────────────────────────────────────────
    private fun emojiFor(species: String) = when (species) {
        "SIGIR" -> "🐄"; "MANDA" -> "🐃"; "KOYUN" -> "🐑"
        "KECI" -> "🐐"; "KANATLI" -> "🐔"; else -> "🐄"
    }

    private fun categoryDisplayName(cat: String) = when (cat) {
        "SUT_INEGI_ERKEN" -> "Sut Inegi – Erken Lakt."
        "SUT_INEGI_ORTA"  -> "Sut Inegi – Orta Lakt."
        "SUT_INEGI_GEC"   -> "Sut Inegi – Gec Lakt."
        "KURU_INEK_UZAK"  -> "Kuru Inek – Uzak"
        "KURU_INEK_YAKIN" -> "Kuru Inek – Yakin (Gecis)"
        "BESI_BASLANGIC"  -> "Besi – Baslangic"
        "BESI_BUYUTME"    -> "Besi – Buyutme"
        "BESI_BITIRME"    -> "Besi – Bitirme"
        else -> cat
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}