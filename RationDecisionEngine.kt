package com.nutripulse.app.data.solver

import com.nutripulse.app.data.model.Feed
import kotlin.math.abs
import kotlin.math.max

/**
 * Shadow-price benzeri karar sinyali ve yem skorlama yardimcisi.
 */
object RationDecisionEngine {

    enum class ProblemType {
        ENERGY,
        PROTEIN,
        NDF,
        MINERAL_CA,
        MINERAL_P,
        MINERAL_MG,
        NONE
    }

    data class Signal(
        val type: ProblemType,
        val label: String,
        val target: Double,
        val actual: Double,
        val gapRatio: Double,
        val shadowPrice: Double,
        val unit: String,
        val deficit: Boolean
    )

    fun buildSignals(
        nelTarget: Double,
        nelActual: Double,
        cpTarget: Double,
        cpActual: Double,
        ndfMinTarget: Double,
        ndfActual: Double,
        caTarget: Double,
        caActual: Double,
        pTarget: Double,
        pActual: Double,
        mgTarget: Double,
        mgActual: Double
    ): List<Signal> {
        val energyGap = ratioGap(nelTarget, nelActual)
        val cpGap = ratioGap(cpTarget, cpActual)
        val ndfGap = ratioGap(ndfMinTarget, ndfActual)
        val caGap = ratioGap(caTarget, caActual)
        val pGap = ratioGap(pTarget, pActual)
        val mgGap = ratioGap(mgTarget, mgActual)

        return listOf(
            signal(ProblemType.ENERGY, "Enerji", nelTarget, nelActual, energyGap, "MJ"),
            signal(ProblemType.PROTEIN, "Protein", cpTarget, cpActual, cpGap, "g"),
            signal(ProblemType.NDF, "NDF", ndfMinTarget, ndfActual, ndfGap, "%"),
            signal(ProblemType.MINERAL_CA, "Kalsiyum", caTarget, caActual, caGap, "g"),
            signal(ProblemType.MINERAL_P, "Fosfor", pTarget, pActual, pGap, "g"),
            signal(ProblemType.MINERAL_MG, "Magnezyum", mgTarget, mgActual, mgGap, "g")
        ).sortedByDescending { abs(it.gapRatio) }
    }

    fun selectPrimarySignal(signals: List<Signal>, thresholdRatio: Double = 0.02): Signal {
        val selected = signals.firstOrNull { abs(it.gapRatio) >= thresholdRatio } ?: return Signal(
            type = ProblemType.NONE,
            label = "Denge",
            target = 0.0,
            actual = 0.0,
            gapRatio = 0.0,
            shadowPrice = 0.0,
            unit = "",
            deficit = true
        )
        return selected
    }

    fun scoreFeedForSignal(feed: Feed, type: ProblemType, dmCost: Double): Double {
        val density = densityFor(feed, type)
        if (density <= 0.0) return 0.0
        return density / max(0.1, dmCost)
    }

    fun instabilityPercent(previous: DoubleArray, current: DoubleArray): Double {
        val prevTotal = previous.sum().coerceAtLeast(1e-6)
        val delta = previous.indices.sumOf { idx -> abs(previous[idx] - current.getOrElse(idx) { 0.0 }) }
        return delta / prevTotal * 100.0
    }

    fun densityFor(feed: Feed, type: ProblemType): Double = when (type) {
        ProblemType.ENERGY -> max(feed.nel, feed.me * 0.62)
        ProblemType.PROTEIN -> feed.cp * 10.0
        ProblemType.NDF -> feed.ndf
        ProblemType.MINERAL_CA -> feed.ca * 10.0
        ProblemType.MINERAL_P -> feed.p * 10.0
        ProblemType.MINERAL_MG -> feed.mg * 10.0
        ProblemType.NONE -> 0.0
    }

    private fun signal(
        type: ProblemType,
        label: String,
        target: Double,
        actual: Double,
        gapRatio: Double,
        unit: String
    ): Signal {
        val severity = abs(gapRatio)
        // Proxy shadow: gap buyuklugu arttikca maliyet baskisi hizla artar.
        val shadow = severity * 12.0
        return Signal(
            type = type,
            label = label,
            target = target,
            actual = actual,
            gapRatio = gapRatio,
            shadowPrice = shadow,
            unit = unit,
            deficit = gapRatio > 0
        )
    }

    private fun ratioGap(target: Double, actual: Double): Double {
        if (target <= 1e-6) return 0.0
        return (target - actual) / target
    }
}
