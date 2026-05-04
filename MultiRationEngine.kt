package com.nutripulse.app.data.solver

import com.nutripulse.app.data.model.AnimalProfile
import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.RationItem
import com.nutripulse.app.data.solver.highs.NutriPulseHiGHS
import com.nutripulse.app.data.solver.impl.RationOptimizer
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * NutriPulse — Çoklu Rasyon Motoru (Brill Formulation Seviyesi)
 *
 * Brill'in "Multi-Group Optimization" özelliğini uygular.
 *
 * ─── İKİ MOD ─────────────────────────────────────────────────────────
 *
 * Mod 1 — Bağımsız Optimizasyon (INDEPENDENT):
 *   Her hayvan grubu kendi LP'siyle ayrı ayrı optimize edilir.
 *   Aynı yem havuzu kullanılır, miktarlar grup bazında farklı olabilir.
 *   → Kullanım: Karma yemleme yapılmıyorsa (farklı gruplar farklı TMR alıyorsa)
 *
 * Mod 2 — Ortak TMR Optimizasyonu (SHARED_TMR):
 *   Tüm gruplar aynı TMR karışım oranlarını paylaşır (y[j] = oran %),
 *   sadece toplam miktar (DM hedefi) gruba göre değişir.
 *   Bu bir tek LP'ye indirgenir: y değişkenler paylaşımlı.
 *   → Kullanım: Tek miks üretilip farklı miktarda dağıtılıyorsa
 *
 * Mod 3 — Stacked LP (STACKED):
 *   Tüm grupları tek bir büyük LP'ye yığar.
 *   NutriPulseHiGHS.solveLp() ile tek seferde çözülür.
 *   Gruplar arası bağlantı kısıtları eklenebilir (örn. mineral kısıtı).
 *   → Kullanım: Gruplar arası optimizasyon yapılacaksa
 *
 * ─── ÇIKTI ────────────────────────────────────────────────────────────
 *   Her grup için RationResult + sürü özeti
 */
object MultiRationEngine {

    enum class OptMode { INDEPENDENT, SHARED_TMR, STACKED }

    // ─────────────────────────── Data Classes ───────────────────────────

    data class AnimalGroup(
        val groupId: String,
        val label: String,                       // "Yüksek Süt İnekleri"
        val animal: AnimalProfile,
        val headCount: Int = 1,
        val feedPool: List<Feed>,                // bu gruba özel havuz (boşsa ortak havuz)
        val customConstraints: GroupConstraints = GroupConstraints()
    )

    data class GroupConstraints(
        val ndfMinPct:      Double = 0.0,
        val ndfMaxPct:      Double = 0.0,
        val roughageMinPct: Double = 0.0,
        val cpMaxPct:       Double = 0.0,
        val fatMaxPct:      Double = 0.0,
        val starchMaxPct:   Double = 0.0,
        val maxFeedShare:   Double = 0.0,
        val tolerancePct:   Double = 0.0
    )

    data class MultiRationInput(
        val groups: List<AnimalGroup>,
        val sharedFeedPool: List<Feed>,          // ortak yem havuzu
        val milkPrice: Double = 18.0,
        val mode: OptMode = OptMode.INDEPENDENT,
        val rationMode: String = "TMR"
    )

    data class GroupResult(
        val groupId: String,
        val label: String,
        val headCount: Int,
        val rationResult: RationOptimizer.RationResult,
        val costPerHead: Double,
        val costTotal: Double,                   // headCount * costPerHead
        val milkRevenuePerHead: Double,
        val netProfitPerHead: Double,
        val netProfitTotal: Double
    )

    data class MultiRationResult(
        val mode: OptMode,
        val groups: List<GroupResult>,
        val herdSummary: HerdSummary,
        val sharedTmrRatios: Map<String, Double>?,  // Mod 2: paylaşımlı oran %
        val warnings: List<String>
    )

    data class HerdSummary(
        val totalHeads:      Int,
        val totalDailyCost:  Double,
        val totalMonthlyCost:Double,
        val avgCostPerHead:  Double,
        val totalMilkRevenue:Double,
        val totalNetProfit:  Double,
        val monthlyNetProfit:Double,
        val feedBreakdown:   List<FeedBreakdown>
    )

    data class FeedBreakdown(
        val feedId:    Int,
        val feedName:  String,
        val totalKgAsFed: Double,          // tüm sürü için as-fed kg/gün
        val totalCost: Double
    )

    // ─────────────────────────── Ana Fonksiyon ──────────────────────────

    fun optimize(inp: MultiRationInput): MultiRationResult {
        if (inp.groups.isEmpty()) return emptyResult(inp.mode)

        return when (inp.mode) {
            OptMode.INDEPENDENT -> optimizeIndependent(inp)
            OptMode.SHARED_TMR  -> optimizeSharedTmr(inp)
            OptMode.STACKED     -> optimizeStacked(inp)
        }
    }

    // ─────────────────────────── Mod 1: Bağımsız ────────────────────────

    private fun optimizeIndependent(inp: MultiRationInput): MultiRationResult {
        val warnings = mutableListOf<String>()
        val results  = inp.groups.map { group ->
            val pool = group.feedPool.ifEmpty { inp.sharedFeedPool }
            if (pool.isEmpty()) {
                warnings.add("${group.label}: Yem havuzu boş, atlandı")
                return@map null
            }
            val c  = group.customConstraints
            val optInp = RationOptimizer.OptimizationInput(
                animal              = group.animal,
                feeds               = pool,
                milkPrice           = inp.milkPrice,
                customNdfMin        = c.ndfMinPct,
                customNdfMax        = c.ndfMaxPct,
                customCpMaxPct      = c.cpMaxPct,
                customRoughageMinPct= c.roughageMinPct,
                rationMode          = inp.rationMode,
                customFatMaxPct     = c.fatMaxPct,
                customStarchMaxPct  = c.starchMaxPct,
                maxFeedSharePct     = c.maxFeedShare,
                nutrientTolerancePct= c.tolerancePct
            )
            val res = RationOptimizer.optimize(optInp)
            if (res.status == "ERROR") warnings.add("${group.label}: ${res.message}")
            buildGroupResult(group, res, inp.milkPrice)
        }.filterNotNull()

        return MultiRationResult(
            mode         = OptMode.INDEPENDENT,
            groups       = results,
            herdSummary  = buildHerdSummary(results),
            sharedTmrRatios = null,
            warnings     = warnings
        )
    }

    // ─────────────────────────── Mod 2: Ortak TMR ───────────────────────

    /**
     * Tüm gruplar için tek bir TMR karışım oranı (y) optimize edilir.
     * x[g,j] = DM_g * y[j] olduğundan, değişkenler:
     *   y[j] = j. yemin KM içindeki oranı (%)
     *
     * Problem:
     *   min  Σ_j cost[j] * y[j] * Σ_g DM_g    (toplam maliyet)
     *   s.t. Σ_j nel[j]  * y[j] >= NEL_g / DM_g   ∀g  (enerji yoğunluğu)
     *        Σ_j cp[j]   * y[j] >= CP_g  / DM_g   ∀g  (protein yoğunluğu)
     *        Σ_j ndf[j]  * y[j] >= NDF_min          (NDF)
     *        Σ_j y[j] = 100 (veya 1)
     *        y[j] >= 0
     *
     * En kısıtlayıcı grup kısıtları alınır (en yüksek NDF_min, en yüksek CP/kg).
     */
    private fun optimizeSharedTmr(inp: MultiRationInput): MultiRationResult {
        val pool = inp.sharedFeedPool
        if (pool.isEmpty()) return emptyResult(inp.mode)
        val n    = pool.size
        val warnings = mutableListOf<String>()

        // En kısıtlayıcı grup parametrelerini bul
        val maxNelPerDm = inp.groups.maxOf { g ->
            val req = RationOptimizer.resolveAnimalRequirements(g.animal)
            if (req.reqDmKg > 0) req.reqNelMj / req.reqDmKg else 6.2
        }
        val maxCpPerDm = inp.groups.maxOf { g ->
            val req = RationOptimizer.resolveAnimalRequirements(g.animal)
            if (req.reqDmKg > 0) req.reqCpG / req.reqDmKg / 10.0 else 16.0  // % olarak
        }
        val maxNdfMin = inp.groups.maxOf { g ->
            g.customConstraints.ndfMinPct.takeIf { it > 0 }
                ?: RationOptimizer.resolveAnimalRequirements(g.animal).reqNdfPct.takeIf { it > 0 }
                ?: 28.0
        }

        // LP: min Σ cost[j] * y[j]
        val cost = DoubleArray(n) { j -> pool[j].pricePerKg / (pool[j].dm / 100.0).coerceAtLeast(0.01) }
        val rows = mutableListOf<NutriPulseHiGHS.LpRow>()

        // NEL kısıtı: Σ nel[j]*y[j] >= maxNelPerDm
        val nelRow = DoubleArray(n) { j -> pool[j].nel }
        rows.add(NutriPulseHiGHS.LpRow(nelRow, maxNelPerDm, NutriPulseHiGHS.ConstraintType.GE))

        // CP kısıtı: Σ cp[j]*y[j] >= maxCpPerDm
        val cpRow = DoubleArray(n) { j -> pool[j].cp }
        rows.add(NutriPulseHiGHS.LpRow(cpRow, maxCpPerDm, NutriPulseHiGHS.ConstraintType.GE))

        // NDF kısıtı: Σ ndf[j]*y[j] >= maxNdfMin
        val ndfRow = DoubleArray(n) { j -> pool[j].ndf }
        rows.add(NutriPulseHiGHS.LpRow(ndfRow, maxNdfMin, NutriPulseHiGHS.ConstraintType.GE))

        // Toplam = 1 (normalize oran)
        val sumRow = DoubleArray(n) { 1.0 }
        rows.add(NutriPulseHiGHS.LpRow(sumRow, 1.0, NutriPulseHiGHS.ConstraintType.EQ))

        // Yem payı kısıtı: y[j] <= 0.60
        for (j in 0 until n) {
            val c = DoubleArray(n).also { it[j] = 1.0 }
            rows.add(NutriPulseHiGHS.LpRow(c, 0.60, NutriPulseHiGHS.ConstraintType.LE))
        }

        val prob = NutriPulseHiGHS.LpProblem(n, cost, rows)
        val sol  = NutriPulseHiGHS.solveLp(prob)

        if (sol.status != NutriPulseHiGHS.LpSolution.Status.OPTIMAL) {
            warnings.add("Ortak TMR çözümü bulunamadı: ${sol.message}. Bağımsız moda geçiliyor.")
            return optimizeIndependent(inp.copy(mode = OptMode.INDEPENDENT))
        }

        // Paylaşımlı oran haritası
        val sharedRatios = pool.mapIndexed { j, feed ->
            feed.name to r2(sol.x[j] * 100.0)
        }.filter { it.second > 0.01 }.toMap()

        // Her grup için bu oranla miktar hesapla
        val results = inp.groups.map { group ->
            val req = RationOptimizer.resolveAnimalRequirements(group.animal)
            val dm  = max(1.0, req.reqDmKg)
            val manualFeeds = pool.mapIndexedNotNull { j, feed ->
                val ratio  = sol.x.getOrElse(j) { 0.0 }
                val dmKg   = dm * ratio
                if (dmKg < 1e-4) return@mapIndexedNotNull null
                val asFed  = dmKg / (feed.dm / 100.0).coerceAtLeast(0.01)
                RationOptimizer.ManualFeedInput(feed, asFed)
            }
            val res = RationOptimizer.evaluateManual(
                RationOptimizer.ManualInput(group.animal, manualFeeds, inp.milkPrice)
            )
            buildGroupResult(group, res, inp.milkPrice)
        }

        return MultiRationResult(
            mode            = OptMode.SHARED_TMR,
            groups          = results,
            herdSummary     = buildHerdSummary(results),
            sharedTmrRatios = sharedRatios,
            warnings        = warnings
        )
    }

    // ─────────────────────────── Mod 3: Stacked LP ──────────────────────

    /**
     * Tüm grupları tek bir büyük LP'ye yığar.
     * Değişkenler: x[g*n + j] = grup g, yem j'nin kg KM miktarı
     * Kısıtlar: her grup için besin kısıtları (blok-diagonal yapı)
     *
     * Toplam değişken: G * n
     * Toplam kısıt:    G * K  (K = grup başı kısıt sayısı)
     */
    private fun optimizeStacked(inp: MultiRationInput): MultiRationResult {
        val pool = inp.sharedFeedPool
        if (pool.isEmpty()) return emptyResult(inp.mode)

        val G = inp.groups.size
        val n = pool.size
        val N = G * n   // toplam değişken sayısı
        val warnings = mutableListOf<String>()

        // Maliyet: cost[g*n + j] = cost[j]  (tüm gruplar aynı fiyat)
        val cost = DoubleArray(N) { idx ->
            val j = idx % n
            val feed = pool[j]
            feed.pricePerKg / (feed.dm / 100.0).coerceAtLeast(0.01)
        }

        val rows = mutableListOf<NutriPulseHiGHS.LpRow>()

        for (g in 0 until G) {
            val group = inp.groups[g]
            val req   = RationOptimizer.resolveAnimalRequirements(group.animal)
            val dm    = max(1.0, req.reqDmKg)
            val c     = group.customConstraints

            val nelTarget = if (req.reqNelMj > 0) req.reqNelMj else dm * 6.2
            val cpTarget  = if (req.reqCpG   > 0) req.reqCpG   else dm * 160.0
            val caTarget  = if (req.reqCaG   > 0) req.reqCaG   else dm * 4.5
            val pTarget   = if (req.reqPG    > 0) req.reqPG    else dm * 2.8
            val ndfMin    = c.ndfMinPct.takeIf { it > 0 }
                            ?: req.reqNdfPct.takeIf { it > 0 } ?: 28.0

            fun groupRow(selector: (Feed) -> Double): DoubleArray {
                val r = DoubleArray(N)
                for (j in 0 until n) r[g * n + j] = selector(pool[j])
                return r
            }

            // KM eşitliği: Σ_j x[g,j] = DM_g
            rows.add(NutriPulseHiGHS.LpRow(groupRow { 1.0 }, dm, NutriPulseHiGHS.ConstraintType.EQ))

            // NEL: Σ_j nel[j]*x[g,j] >= NEL_g
            rows.add(NutriPulseHiGHS.LpRow(groupRow { it.nel }, nelTarget, NutriPulseHiGHS.ConstraintType.GE))

            // CP: Σ_j cp[j]*x[g,j]*10 >= CP_g (g/gün)
            rows.add(NutriPulseHiGHS.LpRow(groupRow { it.cp * 10.0 }, cpTarget, NutriPulseHiGHS.ConstraintType.GE))

            // NDF: Σ_j ndf[j]*x[g,j] >= ndfMin * dm
            rows.add(NutriPulseHiGHS.LpRow(groupRow { it.ndf }, ndfMin / 100.0 * dm, NutriPulseHiGHS.ConstraintType.GE))

            // Ca
            rows.add(NutriPulseHiGHS.LpRow(groupRow { it.ca * 10.0 }, caTarget, NutriPulseHiGHS.ConstraintType.GE))

            // P
            rows.add(NutriPulseHiGHS.LpRow(groupRow { it.p * 10.0 }, pTarget, NutriPulseHiGHS.ConstraintType.GE))

            // Bireysel yem payı: x[g,j] <= maxShare * dm
            val shareMax = c.maxFeedShare.takeIf { it > 0 } ?: 60.0
            for (j in 0 until n) {
                val r = DoubleArray(N).also { it[g * n + j] = 1.0 }
                rows.add(NutriPulseHiGHS.LpRow(r, shareMax / 100.0 * dm, NutriPulseHiGHS.ConstraintType.LE))
            }
        }

        val prob = NutriPulseHiGHS.LpProblem(N, cost, rows)
        val sol  = NutriPulseHiGHS.solveLp(prob)

        if (sol.status != NutriPulseHiGHS.LpSolution.Status.OPTIMAL) {
            warnings.add("Stacked LP çözümü bulunamadı: ${sol.message}. Bağımsız moda geçiliyor.")
            return optimizeIndependent(inp.copy(mode = OptMode.INDEPENDENT))
        }

        // Her grup için sonucu çöz
        val results = inp.groups.mapIndexed { g, group ->
            val manualFeeds = pool.mapIndexedNotNull { j, feed ->
                val dmKg  = sol.x.getOrElse(g * n + j) { 0.0 }
                if (dmKg < 1e-4) return@mapIndexedNotNull null
                val asFed = dmKg / (feed.dm / 100.0).coerceAtLeast(0.01)
                RationOptimizer.ManualFeedInput(feed, asFed)
            }
            val res = RationOptimizer.evaluateManual(
                RationOptimizer.ManualInput(group.animal, manualFeeds, inp.milkPrice)
            )
            buildGroupResult(group, res, inp.milkPrice)
        }

        return MultiRationResult(
            mode            = OptMode.STACKED,
            groups          = results,
            herdSummary     = buildHerdSummary(results),
            sharedTmrRatios = null,
            warnings        = warnings
        )
    }

    // ─────────────────────────── Yardımcılar ────────────────────────────

    private fun buildGroupResult(
        group: AnimalGroup,
        res: RationOptimizer.RationResult,
        @Suppress("UNUSED_PARAMETER") milkPrice: Double
    ): GroupResult {
        val costPerHead     = res.nutrient.totalCost
        val milkRevPerHead  = res.yield.milkRevenue
        val netPerHead      = res.yield.netProfit
        return GroupResult(
            groupId            = group.groupId,
            label              = group.label,
            headCount          = group.headCount,
            rationResult       = res,
            costPerHead        = r2(costPerHead),
            costTotal          = r2(costPerHead * group.headCount),
            milkRevenuePerHead = r2(milkRevPerHead),
            netProfitPerHead   = r2(netPerHead),
            netProfitTotal     = r2(netPerHead * group.headCount)
        )
    }

    private fun buildHerdSummary(results: List<GroupResult>): HerdSummary {
        val totalHeads   = results.sumOf { it.headCount }
        val totalCostDay = results.sumOf { it.costTotal }
        val totalRevDay  = results.sumOf { it.milkRevenuePerHead * it.headCount }
        val totalNetDay  = results.sumOf { it.netProfitTotal }

        // Yem dökümü: tüm gruplardaki yemleri birleştir
        val feedMap = mutableMapOf<Int, FeedBreakdown>()
        for (gr in results) {
            for (item in gr.rationResult.rationItems) {
                val existing = feedMap[item.feedId]
                val totalKg  = item.amountKg * gr.headCount
                val totalC   = item.costPerDay * gr.headCount
                feedMap[item.feedId] = if (existing == null) {
                    FeedBreakdown(item.feedId, item.feedName, r2(totalKg), r2(totalC))
                } else {
                    existing.copy(
                        totalKgAsFed = r2(existing.totalKgAsFed + totalKg),
                        totalCost    = r2(existing.totalCost    + totalC)
                    )
                }
            }
        }

        return HerdSummary(
            totalHeads       = totalHeads,
            totalDailyCost   = r2(totalCostDay),
            totalMonthlyCost = r2(totalCostDay * 30.0),
            avgCostPerHead   = r2(if (totalHeads > 0) totalCostDay / totalHeads else 0.0),
            totalMilkRevenue = r2(totalRevDay),
            totalNetProfit   = r2(totalNetDay),
            monthlyNetProfit = r2(totalNetDay * 30.0),
            feedBreakdown    = feedMap.values.sortedByDescending { it.totalKgAsFed }
        )
    }

    private fun emptyResult(mode: OptMode) = MultiRationResult(
        mode, emptyList(),
        HerdSummary(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, emptyList()),
        null, listOf("Hayvan grubu tanımlanmamış")
    )

    private fun r2(v: Double) = kotlin.math.round(v * 100.0) / 100.0
}
