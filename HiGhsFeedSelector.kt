package com.nutripulse.app.engine

import com.nutripulse.app.data.FeedPriceDefaults
import com.nutripulse.app.data.model.AnimalProfile
import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.solver.HiGhsSolver
import com.nutripulse.app.data.solver.HiGhsSolver.Constraint
import com.nutripulse.app.data.solver.HiGhsSolver.SolverConfig
import com.nutripulse.app.data.solver.HiGhsSolver.Variable
import kotlin.math.min

/**
 * HiGHS tabanlı yem seçimi optimizasyonu.
 * OR-Tools yerine HiGHS solver kullanır.
 */
object HiGhsFeedSelector {

    fun optimize(
        scoredFeeds: List<Pair<Feed, Double>>,
        constraints: FeedSelectionConstraints,
        objectives: FeedSelectionObjectives
    ): List<Feed> {
        if (scoredFeeds.isEmpty()) throw IllegalArgumentException("Optimize edilecek yem yok")

        val maxFeeds = min(14, scoredFeeds.size)

        val variables = scoredFeeds.mapIndexed { i, pair ->
            Variable(
                name = "x_$i",
                cost = -pair.second,
                lb = 0.0,
                ub = 1.0,
                isInteger = true
            )
        }

        val solverConstraints = mutableListOf<Constraint>()

        solverConstraints.add(
            Constraint(
                name = "feed_count",
                min = 1.0,
                max = maxFeeds.toDouble(),
                coefficients = variables.associate { it.name to 1.0 }
            )
        )

        val config = SolverConfig(
            method = HiGhsSolver.Method.REVISED_SIMPLEX,
            timeLimitSec = 15,
            enablePresolve = true,
            enableParallel = true,
            degeneracyRecovery = true,
            numericalScaling = true
        )

        val solution = HiGhsSolver.solveMILP(variables, solverConstraints, config = config)

        if (solution.status != HiGhsSolver.Status.OPTIMAL && solution.status != HiGhsSolver.Status.SUBOPTIMAL) {
            throw IllegalStateException("HiGHS ile çözüm üretilemedi: status=${solution.status}, message=${solution.message}")
        }

        return scoredFeeds.indices.filter { i ->
            val varName = "x_$i"
            solution.variableValues[varName]?.let { it > 0.5 } == true
        }.map { scoredFeeds[it].first }
    }

    fun optimizeWithFeedLimit(
        feeds: List<Feed>,
        animalProfile: AnimalProfile,
        maxFeedCount: Int,
        constraints: FeedSelectionConstraints,
        objectives: FeedSelectionObjectives
    ): List<Feed> {
        val filtered = feeds.filter { constraints.isAllowed(it, animalProfile) }
        if (filtered.isEmpty()) throw IllegalArgumentException("Kısıtları sağlayan yem yok")

        val scored = filtered.map { it to objectives.score(it, animalProfile) }

        val variables = scored.mapIndexed { i, pair ->
            Variable(
                name = "feed_$i",
                cost = -pair.second,
                lb = 0.0,
                ub = 1.0,
                isInteger = true
            )
        }

        val solverConstraints = mutableListOf<Constraint>()

        solverConstraints.add(
            Constraint(
                name = "max_feeds",
                min = 1.0,
                max = maxFeedCount.toDouble(),
                coefficients = variables.associate { it.name to 1.0 }
            )
        )

        val config = SolverConfig(
            method = HiGhsSolver.Method.REVISED_SIMPLEX,
            timeLimitSec = 20,
            enablePresolve = true,
            enableParallel = true,
            degeneracyRecovery = true,
            numericalScaling = true
        )

        val solution = HiGhsSolver.solveMILP(
            variables = variables,
            constraints = solverConstraints,
            maxFeedCount = maxFeedCount,
            config = config
        )

        if (solution.status != HiGhsSolver.Status.OPTIMAL && solution.status != HiGhsSolver.Status.SUBOPTIMAL) {
            throw IllegalStateException("HiGHS MILP ile çözüm üretilemedi: status=${solution.status}")
        }

        return scored.indices.filter { i ->
            solution.variableValues["feed_$i"]?.let { it > 0.5 } == true
        }.map { scored[it].first }
    }
}
