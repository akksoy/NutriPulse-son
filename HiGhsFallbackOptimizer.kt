package com.nutripulse.app.data.solver

import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.solver.highs.NutriPulseHiGHS
import com.nutripulse.app.data.solver.impl.RationOptimizer
import kotlin.math.max

/**
 * NutriPulse — HiGHS Tabanlı Fallback Optimizer
 *
 * DEĞİŞİKLİK: Python ProcessBuilder tamamen kaldırıldı.
 * Artık NutriPulseHiGHS (pure Kotlin Revised Simplex) kullanılıyor.
 *
 * Tetiklenme senaryoları:
 *   1. RationOptimizer.optimize() → INFEASIBLE
 *   2. RationOptimizer.optimize() → ERROR
 *   3. RationStudioFragment → manuel fallback çağrısı
 *
 * Strateji: 5 kademeli kısıt gevşeme
 *   Adım 0 → Nominal
 *   Adım 1 → Tolerans +3%
 *   Adım 2 → NDF -10%, kaba yem -10%
 *   Adım 3 → NDF -20%, kaba yem -20%
 *   Adım 4 → Sadece NEL + CP (acil)
 */
object HiGhsFallbackOptimizer {

    data class FallbackResult(
        val rationResult: RationOptimizer.RationResult?,
        val succeeded:    Boolean,
        val stepUsed:     Int,
        val stepLabel:    String,
        val message:      String
    )

    fun tryOptimize(inp: RationOptimizer.OptimizationInput): FallbackResult {
        for ((idx, step) in STEPS.withIndex()) {
            val result = tryStep(inp, step) ?: continue
            return FallbackResult(result, true, idx, step.label,
                "Fallback basarili: ${step.label} | ${result.message}")
        }
        return FallbackResult(null, false, -1, "tum adimlar basarisiz",
            "Hicbir gevsetme adiminda cozum bulunamadi. Yem havuzunu genisletin.")
    }

    fun emergencyOptimize(inp: RationOptimizer.OptimizationInput) =
        tryStep(inp, STEPS.last())

    // ── Adım tablosu ──────────────────────────────────────────────────

    private data class RelaxStep(
        val label:       String,
        val ndfMult:     Double,
        val roughMult:   Double,
        val tolPct:      Double,
        val shareAdd:    Double,
        val onlyBasic:   Boolean = false
    )

    private val STEPS = listOf(
        RelaxStep("nominal",      1.00, 1.00, 0.0,  0.0),
        RelaxStep("tol+3",        1.00, 0.95, 3.0,  5.0),
        RelaxStep("ndf-10",       0.90, 0.90, 5.0, 10.0),
        RelaxStep("ndf-20",       0.80, 0.80, 8.0, 15.0),
        RelaxStep("sadece-temel", 0.70, 0.70, 10.0,20.0, true)
    )

    private val ROUGHAGE_CATS = setOf("Sulu Kaba Yemler","Kuru Kaba Yemler","ROUGHAGE_WET","ROUGHAGE_DRY")

    // ── Tek adım ─────────────────────────────────────────────────────

    private fun tryStep(
        inp: RationOptimizer.OptimizationInput,
        step: RelaxStep
    ): RationOptimizer.RationResult? {
        return try {
            val animal  = RationOptimizer.resolveAnimalRequirements(inp.animal)
            val dm      = (if (inp.dmTargetOverrideKg > 0.1) inp.dmTargetOverrideKg
                           else animal.reqDmKg).coerceAtLeast(1.0)
            val feeds   = inp.feeds
            if (feeds.isEmpty()) return null
            val n       = feeds.size
            val tol     = 1.0 - step.tolPct / 100.0

            val nelT = max(0.1, (if (animal.reqNelMj > 0) animal.reqNelMj else dm * 6.2) * tol)
            val cpT  = max(0.1, (when {
                inp.customCpMaxPct > 0 -> dm * inp.customCpMaxPct * 10.0
                animal.reqCpG > 0      -> animal.reqCpG
                else                   -> dm * 160.0
            }) * tol)
            val caT  = max(0.1, (if (animal.reqCaG > 0) animal.reqCaG else dm * 4.5) * tol)
            val pT   = max(0.1, (if (animal.reqPG  > 0) animal.reqPG  else dm * 2.8) * tol)
            val ndfMin = ((inp.customNdfMin.takeIf { it > 0 }
                          ?: animal.reqNdfPct.takeIf { it > 0 } ?: 28.0) * step.ndfMult)
            val share  = ((inp.maxFeedSharePct.takeIf { it > 0 } ?: 60.0) + step.shareAdd)
                          .coerceAtMost(85.0)

            val cost = DoubleArray(n) { j ->
                feeds[j].pricePerKg / (feeds[j].dm / 100.0).coerceAtLeast(0.01)
            }
            val rows = mutableListOf<NutriPulseHiGHS.LpRow>()

            // KM eşitliği
            rows.add(NutriPulseHiGHS.LpRow(DoubleArray(n) { 1.0 }, dm, NutriPulseHiGHS.ConstraintType.EQ))
            // NEL
            rows.add(NutriPulseHiGHS.LpRow(DoubleArray(n) { j -> feeds[j].nel }, nelT, NutriPulseHiGHS.ConstraintType.GE))
            // CP
            rows.add(NutriPulseHiGHS.LpRow(DoubleArray(n) { j -> feeds[j].cp * 10.0 }, cpT, NutriPulseHiGHS.ConstraintType.GE))

            if (!step.onlyBasic) {
                // NDF
                rows.add(NutriPulseHiGHS.LpRow(DoubleArray(n) { j -> feeds[j].ndf }, ndfMin / 100.0 * dm, NutriPulseHiGHS.ConstraintType.GE))
                // Ca
                rows.add(NutriPulseHiGHS.LpRow(DoubleArray(n) { j -> feeds[j].ca * 10.0 }, caT, NutriPulseHiGHS.ConstraintType.GE))
                // P
                rows.add(NutriPulseHiGHS.LpRow(DoubleArray(n) { j -> feeds[j].p * 10.0 }, pT, NutriPulseHiGHS.ConstraintType.GE))
                // Kaba yem
                val roughMask = DoubleArray(n) { j -> if (feeds[j].category in ROUGHAGE_CATS) 1.0 else 0.0 }
                val roughMin  = (inp.customRoughageMinPct.takeIf { it > 0 } ?: 30.0) * step.roughMult
                if (roughMask.any { it > 0 }) {
                    rows.add(NutriPulseHiGHS.LpRow(roughMask, roughMin / 100.0 * dm, NutriPulseHiGHS.ConstraintType.GE))
                }
            }

            // Bireysel yem pay limiti
            for (j in 0 until n) {
                val c = DoubleArray(n).also { arr -> arr[j] = 1.0 }
                rows.add(NutriPulseHiGHS.LpRow(c, share / 100.0 * dm, NutriPulseHiGHS.ConstraintType.LE))
            }

            val sol = NutriPulseHiGHS.solveLp(NutriPulseHiGHS.LpProblem(n, cost, rows))
            if (sol.status != NutriPulseHiGHS.LpSolution.Status.OPTIMAL) return null

            val manualFeeds = feeds.mapIndexedNotNull { j, feed ->
                val dmKg = sol.x.getOrElse(j) { 0.0 }
                if (dmKg < 1e-4) null
                else RationOptimizer.ManualFeedInput(feed, dmKg / (feed.dm / 100.0).coerceAtLeast(0.01))
            }
            if (manualFeeds.isEmpty()) return null

            val base = RationOptimizer.evaluateManual(
                RationOptimizer.ManualInput(inp.animal, manualFeeds, inp.milkPrice))
            val warn = if (step.onlyBasic)
                "Sadece temel kisitlarla (NEL+CP) cozum uretildi" else "Fallback: ${step.label}"

            base.copy(
                status   = "OPTIMAL_FALLBACK",
                warnings = base.warnings + warn,
                message  = "HiGHS Fallback ${step.label} | ${sol.message}"
            )
        } catch (e: Exception) { null }
    }
}
