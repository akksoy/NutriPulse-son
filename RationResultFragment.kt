package com.nutripulse.app.ui.ration

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.nutripulse.app.R
import com.nutripulse.app.data.AppDatabase
import com.nutripulse.app.data.model.AnimalProfile
import com.nutripulse.app.data.model.AnimalSpecies
import com.nutripulse.app.data.model.FeedCategories
import com.nutripulse.app.data.model.Ration
import com.nutripulse.app.data.solver.SpeciesNutrientConfig
import com.nutripulse.app.data.solver.impl.RationOptimizer
import com.nutripulse.app.data.solver.impl.RationOptimizer.NutrientAnalysisRow
import com.nutripulse.app.data.solver.impl.RationOptimizer.RationResult
import com.nutripulse.app.data.solver.impl.RationOptimizer.ShadowRow
import com.nutripulse.app.databinding.FragmentRationResultBinding
import com.nutripulse.app.databinding.ItemNutrientBarBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RationResultFragment : Fragment() {

    data class AlternativeRationSummary(
        val title: String,
        val costPerDay: Double,
        val netProfit: Double,
        val coveragePct: Double,
        val status: String
    )

    private var _binding: FragmentRationResultBinding? = null
    private val binding get() = _binding!!

    companion object {
        private var pendingResult: RationResult? = null
        private var pendingAnimal: AnimalProfile? = null
        private var pendingName: String = ""
        private var pendingAlternatives: List<AlternativeRationSummary> = emptyList()

        fun newInstance(
            result: RationResult,
            name: String,
            animal: AnimalProfile,
            alternatives: List<AlternativeRationSummary> = emptyList()
        ): RationResultFragment {
            pendingResult = result
            pendingAnimal = animal
            pendingName = name
            pendingAlternatives = alternatives
            return RationResultFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRationResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val result = pendingResult ?: run {
            parentFragmentManager.popBackStack(); return
        }
        val animal = pendingAnimal ?: run {
            parentFragmentManager.popBackStack(); return
        }

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnSaveRation.setOnClickListener { saveRation(result, animal) }
        binding.btnCompare.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, RationCompareFragment())
                .addToBackStack(null).commit()
        }

        // Mevcut UI kullan
        displayResult(result, animal)
    }

    private fun displayResult(r: RationResult, animal: AnimalProfile) {
        val ns = r.nutrient
        val ye = r.yield

        // Hayvan türüne göre besin konfigürasyonu
        val speciesName = SpeciesNutrientConfig.getSpeciesDisplayName(animal.species)
        val relevantNutrients = SpeciesNutrientConfig.getNutrientsForSpecies(animal.species)

        // Başlık & durum - hayvan türünü ekle
        binding.tvRationTitle.text = "$speciesName - $pendingName"
        binding.tvSolverStatus.text = when (r.status) {
            "OPTIMAL" -> "✅ Optimal çözüm bulundu"
            "OPTIMAL_HIGHS" -> "✅ HiGHS ile optimal çözüm bulundu"
            "APPROXIMATE" -> "⚠ Yaklaşık çözüm üretildi"
            "APPROXIMATE_HIGHS" -> "⚠ HiGHS ile yaklaşık çözüm üretildi"
            "CONSTRAINT_FAILED" -> "❌ NRC hard kısıtlamalar ihlal edildi"
            "INFEASIBLE" -> "❌ Çözüm yok — kısıtları gevşetin"
            "MANUAL_OK" -> "✅ Manuel rasyon hesaplandı"
            "MANUAL_GAP" -> "⚠ Manuel rasyonda eksik/fazla noktalar var"
            "MANUAL_CONSTRAINT_FAILED" -> "❌ Manuel rasyon NRC kısıtlamalarını ihlal ediyor"
            else -> "⚠ ${r.message}"
        }
        binding.tvSolverStatus.setTextColor(
            when (r.status) {
                "OPTIMAL", "OPTIMAL_HIGHS", "MANUAL_OK" -> Color.parseColor("#00FF88")
                "CONSTRAINT_FAILED", "MANUAL_CONSTRAINT_FAILED", "INFEASIBLE" -> Color.parseColor("#FF0000")
                else -> Color.parseColor("#FF7A00")
            }
        )

        // Ust ozet kutulari
        val costRatio = if (ye.milkRevenue > 0.0) (ns.totalCost / ye.milkRevenue * 100.0) else 0.0
        binding.tvTopCoverageSummary.text = "%${String.format("%.1f", ye.coveragePct)}"
        binding.tvTopCoverageNote.text = "Durum: ${CoverageProgressHelper.statusLabel(ye.coveragePct)}"
        binding.tvCostPercent.text = "%${String.format("%.1f", costRatio)}"
        binding.tvTotalCost.text = "₺${String.format("%.2f", ns.totalCost)} / gün"

        val profit = ye.netProfit
        binding.tvNetProfit.text = "Net kar: ₺${String.format("%.2f", profit)} / gün"
        binding.tvNetProfit.setTextColor(
            if (profit >= 0) Color.parseColor("#00FF88") else Color.parseColor("#FF7A00")
        )

        val grouped = r.rationItems.groupBy { it.feedCategory }
        val dmCoverage = if (ns.dmTarget > 0.0) (ns.dmKg / ns.dmTarget * 100.0) else 0.0
        binding.tvUsedFeedCount.text = "%${String.format("%.1f", dmCoverage)}"
        binding.tvUsedCategoryCount.text = "KM ${fmt(ns.dmKg)} / ${fmt(ns.dmTarget)} kg"

        // Kategori ozeti - hangi yem gruplari kullanildi
        val usedFeedCount = r.rationItems.size
        // Yem listesi + yem bazli shadow etki (sonuç ekranı için basit adapter)
        val feedShadowMap = buildFeedShadowProxy(r)
        val sortedItems = r.rationItems.sortedByDescending { it.amountDmKg }
        val feedAdapter = RationFeedResultAdapter(sortedItems, feedShadowMap)
        binding.recyclerRationFeeds.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRationFeeds.adapter = feedAdapter



        // Hayvan türüne göre besin progress barları - en önemli besinler
        // Sığır/Manda: NEL, ME, HP, NDF, Ca, P, Mg, Na
        val topNutrients = relevantNutrients.take(8)
        
        setupBar(binding.barDm,  "KM",  ns.dmKg,   ns.dmTarget,  "kg")
        if (topNutrients.any { it.key == "nel" }) {
            setupBar(binding.barNel, "NEL", ns.nelMj, ns.nelTarget, "MJ")
        }
        if (topNutrients.any { it.key == "me" }) {
            setupBar(binding.barMe,  "ME",  ns.meMj,  ns.meTarget,  "MJ")
        }
        if (topNutrients.any { it.key == "cp" }) {
            setupBar(binding.barCp,  "HP",  ns.cpG,   ns.cpTarget,  "g")
        }
        if (topNutrients.any { it.key == "ndf" }) {
            setupBar(binding.barNdf, "NDF", ns.ndfPct, ns.ndfMin,    "%")
        }
        if (topNutrients.any { it.key == "ca" }) {
            setupBar(binding.barCa,  "Ca",  ns.caG,   ns.caTarget,  "g")
        }
        if (topNutrients.any { it.key == "p" }) {
            setupBar(binding.barP,   "P",   ns.pG,    ns.pTarget,   "g")
        }
        if (topNutrients.any { it.key == "mg" }) {
            setupBar(binding.barMg,  "Mg",  ns.mgG,   ns.mgTarget,  "g")
        }

        // Verim tahmini
        binding.tvCoveragePct.text = "%${ye.coveragePct}"
        binding.tvLimitingNutrient.text = ye.limitingNutrient.ifEmpty { "Yok" }
        binding.tvEstMilk.text =
            if (ye.estMilkL > 0) "${ye.estMilkL} L/gün"
            else "${ye.estGainG} g/gün GCAA"
        binding.tvMilkRevenue.text = "₺${String.format("%.2f", ye.milkRevenue)}"
        binding.tvHerdCost.text = "₺${String.format("%.2f", ye.herdCost)} (${animal.animalCount} baş)"
        binding.tvHerdMonthly.text = "₺${String.format("%.0f", ye.herdMonthNet)}"

        // Gölge fiyatlar
        val shadowAdapter = ShadowAdapter(r.shadow)
        binding.recyclerShadowPrices.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerShadowPrices.adapter = shadowAdapter

        setupAnalysisAndSuggestions(r, feedShadowMap, animal)

        // Uyarılar
        val warningLines = buildList {
            addAll(r.warnings)
        }

        if (warningLines.isNotEmpty()) {
            binding.layoutWarnings.visibility = View.VISIBLE
            binding.tvWarnings.text = warningLines.joinToString("\n")
        } else {
            binding.layoutWarnings.visibility = View.GONE
        }
    }

    private fun setupAnalysisAndSuggestions(r: RationResult, feedShadowMap: Map<String, Double>, animal: AnimalProfile) {
        val rows = if (r.analysisRows.isNotEmpty()) r.analysisRows else fallbackAnalysisRows(r, animal)
        
        binding.recyclerNutrientAnalysis.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerNutrientAnalysis.adapter = NutrientAnalysisAdapter(rows)
        
        val suggestionLines = buildList {
            addAll(buildAiSuggestions(r, rows, feedShadowMap))
            if (isEmpty()) add("✅ Sistem yorumu: rasyon dengeli gorunuyor")
        }
        binding.tvSmartSuggestions.text = suggestionLines.distinct().joinToString("\n\n") { it }

        binding.tvCostOptimization.text = buildCostOptimizationText(r, feedShadowMap)
        
        binding.tvShadowSuggestions.text = buildShadowSuggestions(r, feedShadowMap)

        val usedFeedCount = r.rationItems.size
        binding.tvUsedFeedCountDetail.text = "$usedFeedCount yem"

        val alternatives = if (pendingAlternatives.isNotEmpty()) {
            pendingAlternatives
        } else {
            listOf(
                AlternativeRationSummary(
                    title = "Mevcut Cozum",
                    costPerDay = r.nutrient.totalCost,
                    netProfit = r.yield.netProfit,
                    coveragePct = r.yield.coveragePct,
                    status = r.status
                )
            )
        }
        binding.recyclerAlternativeRations.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerAlternativeRations.adapter = AlternativeRationAdapter(alternatives)

        bindRadarChart(rows)
        bindPieChart(r)
        bindBarChart(r)
    }

    private fun buildCostOptimizationText(r: RationResult, feedShadowMap: Map<String, Double>): String {
        val ns = r.nutrient
        val sb = StringBuilder()
        
        sb.appendLine("Toplam Maliyet: ₺${fmt(ns.totalCost)}/gun")
        
        val totalCost = ns.totalCost
        val topExpensiveFeeds = r.rationItems.sortedByDescending { it.costPerDay }.take(3)
        if (topExpensiveFeeds.isNotEmpty()) {
            sb.appendLine("\nEn Yuksek Maliyetli Yemler:")
            topExpensiveFeeds.forEach { item ->
                val costShare = if (totalCost > 0) (item.costPerDay / totalCost * 100) else 0.0
                sb.appendLine("• ${item.feedName}: ₺${fmt(item.costPerDay)} (%${fmt(costShare)})")
                if (costShare > 15) {
                    val alt = suggestAlternative(item.feedCategory)
                    sb.appendLine("  → Alternatif: $alt")
                }
            }
        }
        
        sb.appendLine("\nNet Kar: ₺${fmt(r.yield.netProfit)}/gun")
        sb.appendLine("Verim: ${r.yield.estMilkL} L/gun")
        
        return sb.toString()
    }
    
    private fun buildShadowSuggestions(r: RationResult, feedShadowMap: Map<String, Double>): String {
        val sb = StringBuilder()
        
        feedShadowMap.entries.sortedByDescending { it.value }.take(4).forEach { (feedName, shadow) ->
            if (shadow > 0.1) {
                val item = r.rationItems.find { it.feedName == feedName }
                if (item != null) {
                    val costShare = if (r.nutrient.totalCost > 0) (item.costPerDay / r.nutrient.totalCost * 100) else 0.0
                    sb.appendLine("📊 $feedName:")
                    sb.appendLine("   Gölge: ₺${fmt(shadow)} | Pay: %${fmt(costShare)}")
                    if (shadow > 1.0 && costShare > 15) {
                        sb.appendLine("   → Fiyat dusurse rasyon maliyeti azalabilir")
                    }
                }
            }
        }
        
        if (sb.isEmpty()) {
            sb.appendLine("✅ Tüm yemler dengeli fiyatlandırılmış")
        }
        
        return sb.toString().trimEnd()
    }

    private fun bindRadarChart(rows: List<NutrientAnalysisRow>) {
        val base = rows.take(6)
        val entries = base.map { row ->
            val ratio = if (row.target > 0) (row.actual / row.target).coerceIn(0.0, 1.4) else 1.0
            RadarEntry((ratio * 100.0).toFloat())
        }

        val dataSet = RadarDataSet(entries, "Besin Denge").apply {
            color = Color.parseColor("#00B4FF")
            fillColor = Color.parseColor("#00B4FF")
            setDrawFilled(true)
            fillAlpha = 90
            valueTextColor = Color.parseColor("#9ED8FF")
            valueTextSize = 8f
        }

        binding.chartRadar.apply {
            data = RadarData(dataSet)
            xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(base.map { it.name })
            xAxis.textColor = Color.parseColor("#A0B9C8")
            yAxis.textColor = Color.parseColor("#A0B9C8")
            description.isEnabled = false
            legend.textColor = Color.parseColor("#8BD3FF")
            invalidate()
        }
    }

    private fun bindPieChart(r: RationResult) {
        val entries = r.rationItems
            .sortedByDescending { it.amountDmKg }
            .take(6)
            .map { PieEntry(it.amountDmKg.toFloat(), it.feedName.take(12)) }

        val dataSet = PieDataSet(entries, "Yem Dagilimi").apply {
            colors = listOf(
                Color.parseColor("#00C896"),
                Color.parseColor("#00B4FF"),
                Color.parseColor("#FFB100"),
                Color.parseColor("#FF7A59"),
                Color.parseColor("#A77BFF"),
                Color.parseColor("#4DD0E1")
            )
            valueTextColor = Color.WHITE
            valueTextSize = 9f
        }

        binding.chartPie.apply {
            data = PieData(dataSet)
            setUsePercentValues(false)
            description.isEnabled = false
            legend.textColor = Color.parseColor("#A0B9C8")
            setEntryLabelColor(Color.WHITE)
            invalidate()
        }
    }

    private fun bindBarChart(r: RationResult) {
        val top = r.rationItems.sortedByDescending { it.costPerDay }.take(6)
        val entries = top.mapIndexed { idx, item -> BarEntry(idx.toFloat(), item.costPerDay.toFloat()) }
        val labels = top.map { it.feedName.take(10) }

        val dataSet = BarDataSet(entries, "Yem Katkisi (TL/gun)").apply {
            color = Color.parseColor("#00FF88")
            valueTextColor = Color.parseColor("#B2FFD9")
            valueTextSize = 8f
        }

        binding.chartBar.apply {
            data = BarData(dataSet)
            xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.textColor = Color.parseColor("#A0B9C8")
            axisLeft.textColor = Color.parseColor("#A0B9C8")
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.textColor = Color.parseColor("#8BD3FF")
            invalidate()
        }
    }

    private fun fallbackAnalysisRows(r: RationResult, animal: AnimalProfile): List<NutrientAnalysisRow> {
        val n = r.nutrient
        val result = mutableListOf<NutrientAnalysisRow>()
        
        result.add(NutrientAnalysisRow("KM", n.dmTarget, n.dmKg, n.dmKg - n.dmTarget, 0.0, "kg", statusFromRatio(n.dmKg, n.dmTarget)))
        result.add(NutrientAnalysisRow("ME", n.meTarget, n.meMj, n.meMj - n.meTarget, 0.0, "MJ", statusFromRatio(n.meMj, n.meTarget)))
        result.add(NutrientAnalysisRow("NEL", n.nelTarget, n.nelMj, n.nelMj - n.nelTarget, 0.0, "MJ", statusFromRatio(n.nelMj, n.nelTarget)))
        result.add(NutrientAnalysisRow("HP", n.cpTarget, n.cpG, n.cpG - n.cpTarget, 0.0, "g", statusFromRatio(n.cpG, n.cpTarget)))
        result.add(NutrientAnalysisRow("Ca", n.caTarget, n.caG, n.caG - n.caTarget, 0.0, "g", statusFromRatio(n.caG, n.caTarget)))
        result.add(NutrientAnalysisRow("P", n.pTarget, n.pG, n.pG - n.pTarget, 0.0, "g", statusFromRatio(n.pG, n.pTarget)))
        result.add(NutrientAnalysisRow("Mg", n.mgTarget, n.mgG, n.mgG - n.mgTarget, 0.0, "g", statusFromRatio(n.mgG, n.mgTarget)))
        
        // NDF süt ineği için önemli - her zaman göster
        val ndfTarget = if (n.ndfMin > 0) n.ndfMin else n.dmKg * 0.35
        result.add(NutrientAnalysisRow("NDF", ndfTarget, n.ndfPct, n.ndfPct - ndfTarget, 0.0, "%", statusFromRatio(n.ndfPct, ndfTarget)))
        
        // Na ve K süt ineği için
        if (n.naTarget > 0 || animal.species == "SIGIR") {
            val naT = if (n.naTarget > 0) n.naTarget else n.dmKg * 0.0015
            result.add(NutrientAnalysisRow("Na", naT, n.naG, n.naG - naT, 0.0, "g", statusFromRatio(n.naG, naT)))
        }
        if (n.kTarget > 0 || animal.species == "SIGIR") {
            val kT = if (n.kTarget > 0) n.kTarget else n.dmKg * 0.01
            result.add(NutrientAnalysisRow("K", kT, n.kG, n.kG - kT, 0.0, "g", statusFromRatio(n.kG, kT)))
        }
        if (n.rdpTarget > 0) {
            result.add(NutrientAnalysisRow("RDP", n.rdpTarget, n.rdpG, n.rdpG - n.rdpTarget, 0.0, "g", statusFromRatio(n.rdpG, n.rdpTarget)))
        }
        
        return result
    }

    private fun statusFromRatio(actual: Double, target: Double): String {
        if (target <= 0.0) return "OK"
        val ratio = actual / target
        return when {
            ratio in 0.97..1.03 -> "OK"
            ratio in 0.92..1.08 -> "NEAR"
            else -> "ALERT"
        }
    }

    private fun signedNum(v: Double): String = if (v >= 0) "+${shortNum(v)}" else shortNum(v)

    private fun shortNum(v: Double): String = if (kotlin.math.abs(v) >= 100) {
        String.format("%.1f", v)
    } else {
        String.format("%.2f", v)
    }

    // Hayvan türüne göre besin değerlerini al
    private fun getNutrientValues(
        ns: RationOptimizer.NutrientActual,
        key: String
    ): Pair<Double, Double> {
        return when (key) {
            "nel" -> Pair(ns.nelMj, ns.nelTarget)
            "me" -> Pair(ns.meMj, ns.meTarget)
            "cp" -> Pair(ns.cpG, ns.cpTarget)
            "ndf" -> Pair(ns.ndfPct, ns.ndfMin)
            "adf" -> Pair(ns.adfPct, ns.adfPct * 0.8)
            "ca" -> Pair(ns.caG, ns.caTarget)
            "p" -> Pair(ns.pG, ns.pTarget)
            "mg" -> Pair(ns.mgG, ns.mgTarget)
            "k" -> Pair(ns.kG, ns.kTarget)
            "na" -> Pair(ns.naG, ns.naTarget)
            "lys" -> Pair(ns.lysG, ns.lysTarget)
            "met" -> Pair(ns.metG, ns.metTarget)
            "rdp" -> Pair(ns.rdpG, ns.rdpTarget)
            "rup" -> Pair(ns.rupG, ns.rupTarget)
            "dm" -> Pair(ns.dmKg, ns.dmTarget)
            else -> Pair(0.0, 0.0)
        }
    }

    private fun setupBar(
        b: ItemNutrientBarBinding, label: String,
        actual: Double, target: Double, unit: String
    ) {
        b.tvBarLabel.text = label
        val rawPct = if (target > 0) (actual / target * 100).toInt() else 100
        val pct = rawPct.coerceIn(0, 300)
        b.progressBar.progress = pct
        b.tvBarPct.text = "%$rawPct"
        b.tvBarValue.text = "${fmt(actual)}/${fmt(target)} $unit"

        val color = when {
            rawPct > 130 -> "#FF7A00"  // aşırı fazla
            rawPct >= 98 -> "#00FF88"  // tamam
            rawPct >= 88 -> "#FFC400"  // yakın
            else      -> "#FF5A5A"  // yetersiz
        }
        b.tvBarPct.setTextColor(Color.parseColor(color))
        b.progressBar.progressTintList =
            android.content.res.ColorStateList.valueOf(Color.parseColor(color))
        b.tvBarStatus.text = when {
            rawPct > 130 -> "HIGH"
            rawPct >= 95 -> "OK"
            rawPct >= 85 -> "NEAR"
            else -> "LOW"
        }
    }

    private fun fmt(v: Double) =
        if (v >= 10) String.format("%.1f", v) else String.format("%.2f", v)

    private fun buildFeedShadowProxy(r: RationResult): Map<String, Double> {
        if (r.rationItems.isEmpty()) return emptyMap()
        
        val totalCost = r.rationItems.sumOf { it.costPerDay }.coerceAtLeast(1e-6)
        val ns = r.nutrient
        
        return r.rationItems.associate { item ->
            val costShare = (item.costPerDay / totalCost).coerceIn(0.0, 1.0)
            val dmShare = (item.amountDmKg / ns.dmKg.coerceAtLeast(1e-6)).coerceIn(0.0, 1.0)
            
            val shadowValue = when {
                item.costPerDay > 0 && costShare > 0.15 -> costShare * item.costPerDay * 0.3
                dmShare > 0.25 -> dmShare * ns.totalCost * 0.1
                else -> costShare * ns.totalCost * 0.05
            }
            
            item.feedName to shadowValue
        }
    }

    private fun buildAiSuggestions(
        r: RationResult,
        rows: List<NutrientAnalysisRow>,
        feedShadowMap: Map<String, Double>
    ): List<String> {
        val suggestions = mutableListOf<String>()
        
        rows.forEach { row ->
            val ratio = if (row.target > 0) (row.actual / row.target * 100) else 100.0
            when {
                ratio < 90 -> {
                    val deficit = row.target - row.actual
                    val action = suggestFeedForDeficiency(row.name, deficit, row.unit, r)
                    suggestions += "⚠️ ${row.name} eksik (${fmt(ratio)}%): $action"
                }
                ratio > 115 -> {
                    val excess = row.actual - row.target
                    suggestions += "⚠️ ${row.name} fazla (${fmt(ratio)}%): ${suggestReduction(row.name)}"
                }
            }
        }
        
        if (suggestions.isEmpty()) suggestions += "✅ Besin dengesi iyi"
        
        return suggestions
    }
    
    private fun suggestFeedForDeficiency(nutrient: String, deficit: Double, unit: String, r: RationResult): String {
        return when (nutrient) {
            "HP" -> {
                val proteinFeeds = r.rationItems.filter { it.feedCategory == FeedCategories.PROTEIN }
                if (proteinFeeds.isNotEmpty()) {
                    "Soya kuspesi veya aycicek kuspesi oranini artirin (~${fmt(deficit/10)} kg KM eklenebilir)"
                } else {
                    "Protein kaynagi ekleyin: Soya Kuspesi önerilir"
                }
            }
            "NEL", "ME" -> {
                val grainFeeds = r.rationItems.filter { it.feedCategory == FeedCategories.GRAIN }
                if (grainFeeds.isNotEmpty()) {
                    "Misir dane veya arpa oranini artirin (~${fmt(deficit/5)} kg KM)"
                } else {
                    "Enerji kaynagi ekleyin: Misir Dane veya Arpa önerilir"
                }
            }
            "Ca" -> {
                if (r.nutrient.caG > r.nutrient.caTarget * 0.5) {
                    "Kirec Tasi veya dikalsiyum fosfat oranini artirin"
                } else {
                    "Kalsiyum kaynagi ekleyin: Kirec Tasi önerilir"
                }
            }
            "P" -> "Dikalsiyum fosfat veya monokalsiyum fosfat oranini artirin"
            "Mg" -> "Magnezyum oksit veya Mg sulfat ekleyin"
            "NDF" -> {
                val roughage = r.rationItems.filter { 
                    it.feedCategory == FeedCategories.ROUGHAGE_WET || it.feedCategory == FeedCategories.ROUGHAGE_DRY 
                }
                if (roughage.isNotEmpty()) {
                    "Kaba yem (silaj/ot/saman) oranini artirin"
                } else {
                    "Kaba yem ekleyin: Yonca Silaji veya Misir Silaji önerilir"
                }
            }
            else -> "${nutrient} ${fmt(deficit)} ${unit} ek besin kaynagi gerekli"
        }
    }
    
    private fun suggestReduction(nutrient: String): String {
        return when (nutrient) {
            "Ca" -> "Mineral kaynagi oranini azaltin veya daha dusuk Ca'li yem secin"
            "P" -> "Fosfor icerigi yuksuk yemleri azaltin"
            "HP" -> "Protein orani yuksuk, enerji/protein dengesini ayarlayin"
            "NDF" -> "Kaba yem oranini azaltin, daha az lifli konsantre ekleyin"
            else -> "Bu besini iceren yemlerin oranini azaltin"
        }
    }
    
    private fun suggestAlternative(category: String): String {
        return when (category) {
            FeedCategories.PROTEIN -> "Protein kaynagi: Aycicek Kuspesi, Kanola Kuspesi"
            FeedCategories.GRAIN -> "Tahil: Bugday, Yulaf, Misir Dane"
            FeedCategories.ROUGHAGE_WET -> "Sulu Kaba: Yonca Silaji, Sorgum Silaji"
            FeedCategories.ROUGHAGE_DRY -> "Kuru Kaba: Yonca Kuru, Bugday Saman"
            FeedCategories.MINERAL -> "Mineral: DKP, Kirec Tasi"
            else -> "Alternatif yem"
        }
    }
    
    private fun suggestPriceReduction(feedName: String, currentPrice: Double, shadowValue: Double, currentAmount: Double): String {
        val potentialSaving = shadowValue * currentAmount * 0.3
        return if (potentialSaving > 0.5) {
            "Fiyat %10 dusurse ~₺${fmt(potentialSaving)}/gun tasarruf potansiyeli"
        } else {
            "Bu yemin rasyondaki payi optimum gorunuyor"
        }
    }

    private fun saveRation(r: RationResult, animal: AnimalProfile) {
        val ns = r.nutrient; val ye = r.yield
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val ration = Ration(
                name = pendingName,
                animalProfileId = animal.id,
                animalCategory  = animal.category,
                animalCount     = animal.animalCount,
                totalCostPerDay = ns.totalCost,
                totalCostHerd   = ye.herdCost,
                coveragePct     = ye.coveragePct,
                limitingNutrient= ye.limitingNutrient,
                estimatedYield  = ye.estMilkL,
                netProfitPerDay = ye.netProfit,
                solverStatus    = r.status
            )
            val id = withContext(Dispatchers.IO) { db.rationDao().insert(ration) }
            val items = r.rationItems.map { it.copy(rationId = id.toInt()) }
            withContext(Dispatchers.IO) { db.rationDao().insertItems(items) }
            Toast.makeText(requireContext(), "✅ Rasyon kaydedildi!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requiredCoreCategories(species: String): Set<String> = when (species) {
        AnimalSpecies.SIGIR, AnimalSpecies.MANDA -> setOf(
            FeedCategories.ROUGHAGE_WET,
            FeedCategories.ROUGHAGE_DRY,
            FeedCategories.GRAIN,
            FeedCategories.PROTEIN,
            FeedCategories.MINERAL
        )
        AnimalSpecies.KOYUN, AnimalSpecies.KECI -> setOf(
            FeedCategories.ROUGHAGE_DRY,
            FeedCategories.GRAIN,
            FeedCategories.PROTEIN,
            FeedCategories.MINERAL
        )
        else -> setOf(
            FeedCategories.ROUGHAGE_DRY,
            FeedCategories.GRAIN,
            FeedCategories.PROTEIN,
            FeedCategories.MINERAL
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ── Gölge Fiyat Adapter ──────────────────────────────────────────
class ShadowAdapter(private val rows: List<ShadowRow>) :
    RecyclerView.Adapter<ShadowAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvNutrient: TextView = v.findViewById(R.id.tvShadowNutrient)
        val tvPrice: TextView = v.findViewById(R.id.tvShadowPrice)
        val tvAnalysis: TextView = v.findViewById(R.id.tvShadowAnalysis)
    }

    override fun onCreateViewHolder(parent: ViewGroup, vt: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shadow_price, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val row = rows[pos]
        h.tvNutrient.text = "${row.name} (${row.unit})"
        
        val price = if (row.price != 0.0) row.price else 0.0
        h.tvPrice.text = "₺${String.format("%.2f", price)}"
        
        val priceColor = when {
            price > 5.0 -> "#FF5A5A"
            price > 1.0 -> "#FFC400"
            else -> "#7ABFA0"
        }
        h.tvPrice.setTextColor(Color.parseColor(priceColor))
        
        h.tvAnalysis.text = row.note
        
        if (price > 1.0) {
            h.tvAnalysis.setTextColor(Color.parseColor("#FFC400"))
        } else {
            h.tvAnalysis.setTextColor(Color.parseColor("#7ABFA0"))
        }
    }

    override fun getItemCount() = rows.size
}

// ── Besin Analiz Satiri Adapter ─────────────────────────────────
class NutrientAnalysisAdapter(
    private val rows: List<NutrientAnalysisRow>
) : RecyclerView.Adapter<NutrientAnalysisAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvName)
        val tvStatus: TextView = v.findViewById(R.id.tvStatus)
        val tvTarget: TextView = v.findViewById(R.id.tvTarget)
        val tvActual: TextView = v.findViewById(R.id.tvActual)
        val tvDeviation: TextView = v.findViewById(R.id.tvDeviation)
        val tvShadow: TextView = v.findViewById(R.id.tvShadow)
        val segments: List<View> = listOf(
            v.findViewById(R.id.seg1),
            v.findViewById(R.id.seg2),
            v.findViewById(R.id.seg3),
            v.findViewById(R.id.seg4),
            v.findViewById(R.id.seg5),
            v.findViewById(R.id.seg6),
            v.findViewById(R.id.seg7),
            v.findViewById(R.id.seg8),
            v.findViewById(R.id.seg9),
            v.findViewById(R.id.seg10)
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nutrient_analysis_row, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val row = rows[position]
        h.tvName.text = row.name
        h.tvStatus.text = row.status.uppercase()
        h.tvTarget.text = "Hedef: ${formatNum(row.target)} ${row.unit}"
        h.tvActual.text = "Gercek: ${formatNum(row.actual)} ${row.unit}"
        h.tvDeviation.text = "Sapma: ${if (row.deviation >= 0) "+" else ""}${formatNum(row.deviation)} ${row.unit}"

        val ratio = if (row.target > 0.0) (row.actual / row.target * 100.0).coerceIn(0.0, 200.0) else 100.0
        h.tvShadow.text = "%${formatNum(ratio)}"
        val activeSegments = kotlin.math.ceil(ratio / 10.0).toInt().coerceIn(0, 10)

        val statusColor = when (row.status.uppercase()) {
            "OK" -> Color.parseColor("#00FF88")
            "NEAR" -> Color.parseColor("#FFC400")
            "ALERT" -> Color.parseColor("#FF5A5A")
            else -> Color.parseColor("#53A7FF")
        }
        h.tvName.setTextColor(statusColor)
        h.tvStatus.setTextColor(statusColor)
        h.tvDeviation.setTextColor(statusColor)

        h.segments.forEachIndexed { idx, seg ->
            val color = if (idx < activeSegments) {
                when {
                    ratio > 120.0 -> Color.parseColor("#53A7FF")
                    ratio >= 95.0 -> Color.parseColor("#00FF88")
                    ratio >= 85.0 -> Color.parseColor("#FFC400")
                    else -> Color.parseColor("#FF5A5A")
                }
            } else {
                Color.parseColor("#16222A")
            }
            seg.setBackgroundColor(color)
            seg.alpha = if (idx < activeSegments) 1.0f else 0.5f
        }
    }

    override fun getItemCount(): Int = rows.size

    private fun formatNum(v: Double): String = if (kotlin.math.abs(v) >= 100) {
        String.format("%.1f", v)
    } else {
        String.format("%.2f", v)
    }
}

// ── Alternatif Rasyon Kartı Adapter ─────────────────────────────
class AlternativeRationAdapter(
    private val items: List<RationResultFragment.AlternativeRationSummary>
) : RecyclerView.Adapter<AlternativeRationAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvAltTitle: TextView = v.findViewById(R.id.tvAltTitle)
        val tvAltMetrics: TextView = v.findViewById(R.id.tvAltMetrics)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alt_ration_card, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = items[position]
        h.tvAltTitle.text = item.title
        h.tvAltMetrics.text =
            "Maliyet: ₺${String.format("%.2f", item.costPerDay)}\n" +
                "Net Kar: ₺${String.format("%.2f", item.netProfit)}\n" +
                "Karsilama: %${String.format("%.1f", item.coveragePct)}\n" +
                "Durum: ${item.status}"
    }

    override fun getItemCount(): Int = items.size
}