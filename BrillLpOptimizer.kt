package com.nutripulse.app.data.solver.impl

import com.nutripulse.app.data.model.AnimalProfile
import com.nutripulse.app.data.model.AnimalSpecies
import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.solver.highs.NutriPulseHiGHS

/**
 * BrillLpOptimizer — BrillLpModel'i kurar ve pure Kotlin solver ile çözer.
 *
 * ARTIK PYTHON BRIDGE GEREKTİRMEZ.
 * NutriPulseHiGHS (pure Kotlin Revised Simplex) kullanır.
 *
 * EKLENTİLER:
 * - rdpTargetG  : RDP (rumende yıkılan protein) LP kısıtı için
 * - rupTargetG  : RUP (by-pass protein) LP kısıtı için
 * - lysTargetG  : Lizin LP kısıtı için
 * - metTargetG  : Metiyonin LP kısıtı için
 * - Shadow prices / dual values (NutriPulseHiGHS dual çıkarımı)
 * - Numerical stability
 */
object BrillLpOptimizer {

    data class SolveResult(
        val status: Status,
        val xKgDm: DoubleArray,
        val cost: Double,
        val duals: DoubleArray,
        val message: String,
        val nutrientNames: List<String>,
        val feeds: List<Feed>,
        val shadowPrices: Map<String, Double> = emptyMap(),
        val iterations: Int = 0,
        val solveTimeMs: Long = 0
    ) {
        enum class Status { OPTIMAL, INFEASIBLE, ERROR }
    }

    fun solveCattleBuffalo(
        animal: AnimalProfile,
        feeds: List<Feed>,
        dmTargetKg: Double,
        dmTolPct: Double = 2.0,
        nutrientTolPct: Double,
        ndfMinPct: Double,
        ndfMaxPct: Double,
        roughageMinPct: Double,
        cpTargetG: Double,
        nelTargetMj: Double,
        caTargetG: Double,
        pTargetG: Double,
        rdpTargetG: Double = 0.0,
        rupTargetG: Double = 0.0,
        lysTargetG: Double = 0.0,
        metTargetG: Double = 0.0,
        fatMaxPct: Double,
        starchMaxPct: Double,
        nfcMaxPct: Double,
        maxFeedSharePct: Double,
        includeNelUpperBand: Boolean = true,
        includeCpUpperBand: Boolean = true,
        categoryMinConstraints: List<BrillLpModel.CategoryMinConstraint> = emptyList()
    ): SolveResult {

        if (animal.species != AnimalSpecies.SIGIR && animal.species != AnimalSpecies.MANDA) {
            return SolveResult(
                status          = SolveResult.Status.ERROR,
                xKgDm           = DoubleArray(0),
                cost            = 0.0,
                duals           = DoubleArray(0),
                message         = "BrillLpOptimizer sadece SIGIR / MANDA için aktif.",
                nutrientNames   = emptyList(),
                feeds           = emptyList()
            )
        }

        val model = BrillLpModel.buildForCattleBuffalo(
            animal                  = animal,
            feeds                   = feeds,
            dmTargetKg              = dmTargetKg,
            dmTolPct                = dmTolPct,
            nutrientTolPct          = nutrientTolPct,
            ndfMinPct               = ndfMinPct,
            ndfMaxPct               = ndfMaxPct,
            roughageMinPct          = roughageMinPct,
            cpTargetG               = cpTargetG,
            nelTargetMj             = nelTargetMj,
            caTargetG               = caTargetG,
            pTargetG                = pTargetG,
            rdpTargetG              = rdpTargetG,
            rupTargetG              = rupTargetG,
            lysTargetG              = lysTargetG,
            metTargetG              = metTargetG,
            fatMaxPct               = fatMaxPct,
            starchMaxPct            = starchMaxPct,
            nfcMaxPct               = nfcMaxPct,
            maxFeedSharePct         = maxFeedSharePct,
            includeNelUpperBand     = includeNelUpperBand,
            includeCpUpperBand      = includeCpUpperBand,
            categoryMinConstraints  = categoryMinConstraints
        )

        // ── NutriPulseHiGHS LpRow formatına çevir ──────────────────────────────
        // BrillLpModel: Ax <= b formatında üretir
        // GE kısıtları: satır ve RHS negatif işaretli geliyor (addGeq kuralı)

        val rows = mutableListOf<NutriPulseHiGHS.LpRow>()

        for (i in model.A.indices) {
            val rhs   = model.b[i]
            val coeff = model.A[i].copyOf()

            // BrillLpModel'de addGeq: satır * -1, RHS * -1 olarak kaydedildi
            // Yani rhs < 0 ise orijinal >= kısıtı demek
            if (rhs < 0) {
                rows.add(NutriPulseHiGHS.LpRow(
                    coeff = DoubleArray(model.n) { -coeff[it] },
                    rhs   = -rhs,
                    type  = NutriPulseHiGHS.ConstraintType.GE
                ))
            } else {
                rows.add(NutriPulseHiGHS.LpRow(
                    coeff = coeff,
                    rhs   = rhs,
                    type  = NutriPulseHiGHS.ConstraintType.LE
                ))
            }
        }

        val prob = NutriPulseHiGHS.LpProblem(
            n       = model.n,
            cost    = model.cost,
            rows    = rows
        )

        val sol = NutriPulseHiGHS.solveLp(prob)

        val st = when (sol.status) {
            NutriPulseHiGHS.LpSolution.Status.OPTIMAL -> SolveResult.Status.OPTIMAL
            NutriPulseHiGHS.LpSolution.Status.INFEASIBLE -> SolveResult.Status.INFEASIBLE
            else -> SolveResult.Status.ERROR
        }

        // Shadow prices: constraint isimlerine eşle
        val shadowMap = mutableMapOf<String, Double>()
        for ((i, name) in model.nutrientNames.withIndex()) {
            shadowMap[name] = sol.duals.getOrElse(i) { 0.0 }
        }

        return SolveResult(
            status          = st,
            xKgDm           = sol.x,
            cost            = sol.obj,
            duals           = model.nutrientNames.map { shadowMap[it] ?: 0.0 }.toDoubleArray(),
            message         = sol.message,
            nutrientNames   = model.nutrientNames,
            feeds           = model.feeds,
            shadowPrices    = shadowMap,
            iterations      = sol.iterations,
            solveTimeMs     = 0
        )
    }
}
