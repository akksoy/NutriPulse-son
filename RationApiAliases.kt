package com.nutripulse.app.data.solver

import com.nutripulse.app.data.model.AnimalProfile
import com.nutripulse.app.data.solver.impl.RationOptimizer as Impl
import com.nutripulse.app.data.solver.impl.BrillRationOptimizer as Brill

// Backward-compatible API aliases for tests and older call sites.
typealias OptimizationInput = Impl.OptimizationInput
typealias ManualInput = Impl.ManualInput
typealias ManualFeedInput = Impl.ManualFeedInput
typealias RationResult = Impl.RationResult
typealias NutrientActual = Impl.NutrientActual
typealias ShadowRow = Impl.ShadowRow
typealias YieldEstimate = Impl.YieldEstimate
typealias BalanceMode = com.nutripulse.app.data.solver.impl.SpeciesOptimizationProfile.BalanceMode

object RationOptimizer {
    fun resolveAnimalRequirements(animal: AnimalProfile): AnimalProfile =
        Impl.resolveAnimalRequirements(animal)

    fun optimize(inp: OptimizationInput): RationResult = Brill.optimize(inp)

    fun optimizeWithHiGHS(inp: OptimizationInput): RationResult =
        HiGhsFallbackOptimizer.tryOptimize(inp).rationResult ?: Brill.optimize(inp)

    fun evaluateManual(inp: ManualInput): RationResult = Impl.evaluateManual(inp)
}