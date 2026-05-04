package com.nutripulse.app.ui.ration

import com.nutripulse.app.data.model.AnimalProfile
import com.nutripulse.app.data.model.AnimalSpecies
import com.nutripulse.app.data.model.FeedCategories
import com.nutripulse.app.data.solver.impl.RationOptimizer.RationResult
import kotlin.math.roundToInt

/**
 * Brill-benzeri kalite kapısı.
 *
 * Amaç:
 * - Rasyonun sadece çözülebilir olmasını değil,
 *   aynı zamanda çeşitlilik ve besin temsili açısından da dengeli kalmasını sağlamak.
 * - Aynı kural setini optimize akışında ve sonuç ekranında tekrar kullanmak.
 */
data class BrillAssessment(
    val passes: Boolean,
    val score: Int,
    val feedCount: Int,
    val requiredRange: IntRange,
    val missingCoreCategories: List<String>,
    val limitingNutrient: String,
    val nutrientRatios: Map<String, Double>,
    val reasons: List<String>
) {
    fun summaryText(): String = when {
        passes -> "Brill kalite: UYGUN | skor $score/100 | $feedCount yem"
        else -> "Brill kalite: ZAYIF | skor $score/100 | $feedCount yem"
    }

    fun detailText(): String = when {
        passes -> "Aralık uygun, ana gruplar temsil ediliyor, en kısıtlayıcı besin: $limitingNutrient"
        reasons.isEmpty() -> "Kalite kontrolü geçti"
        else -> reasons.joinToString(" • ")
    }
}

object BrillQualityGate {

    fun assess(result: RationResult, animal: AnimalProfile): BrillAssessment {
        val usedFeedCount = result.rationItems.size
        val requiredRange = 8..14
        val groupedCategories = result.rationItems.map { it.feedCategory }.toSet()
        val requiredCore = coreCategoriesForSpecies(animal.species)
        val missingCore = requiredCore.filter { it !in groupedCategories }

        // Hard constraint check: eğer status CONSTRAINT_FAILED ise zaten başarısız
        val isHardConstraintFailed = result.status in listOf("CONSTRAINT_FAILED", "MANUAL_CONSTRAINT_FAILED")

        val n = result.nutrient
        fun ratio(actual: Double, target: Double): Double = if (target > 0) actual / target else 1.0

        val nutrientRatios = linkedMapOf(
            "NEL" to ratio(n.nelMj, n.nelTarget),
            "ME" to ratio(n.meMj, n.meTarget),
            "Ham Protein" to ratio(n.cpG, n.cpTarget),
            "Kalsiyum" to ratio(n.caG, n.caTarget),
            "Fosfor" to ratio(n.pG, n.pTarget),
            "Magnezyum" to ratio(n.mgG, n.mgTarget)
        )

        val reasons = mutableListOf<String>()

        // Hard constraint ihlalleri başında belirt
        if (isHardConstraintFailed) {
            reasons.add("❌ NRC hard kısıtlamaları ihlal edildi")
        }

        if (usedFeedCount !in requiredRange) {
            reasons.add("Yem sayisi ${usedFeedCount}; hedef ${requiredRange.first}-${requiredRange.last}")
        }
        if (missingCore.isNotEmpty()) {
            reasons.add("Ana grup eksigi: ${missingCore.joinToString(", ")}")
        }

        val cpRatio = nutrientRatios["Ham Protein"] ?: 1.0
        val caRatio = nutrientRatios["Kalsiyum"] ?: 1.0
        val pRatio = nutrientRatios["Fosfor"] ?: 1.0
        val mgRatio = nutrientRatios["Magnezyum"] ?: 1.0

        if (cpRatio > 1.25) reasons.add("HP fazla (%${fmt(cpRatio * 100)})")
        if (caRatio > 1.45) reasons.add("Ca fazla (%${fmt(caRatio * 100)})")
        if (pRatio > 1.45) reasons.add("P fazla (%${fmt(pRatio * 100)})")
        if (mgRatio > 1.45) reasons.add("Mg fazla (%${fmt(mgRatio * 100)})")

        if ((nutrientRatios["NEL"] ?: 1.0) < 0.95) reasons.add("NEL hedefin altinda")
        if ((nutrientRatios["ME"] ?: 1.0) < 0.95) reasons.add("ME hedefin altinda")

        if (n.dmTarget > 0 && (n.dmKg < n.dmTarget * 0.95 || n.dmKg > n.dmTarget * 1.05)) {
            reasons.add("KM hedef bandi disinda (${fmt(n.dmKg)} / ${fmt(n.dmTarget)} kg)")
        }

        if (n.ndfMin > 0 && n.ndfPct < n.ndfMin * 0.97) {
            reasons.add("NDF dusuk (%${fmt(n.ndfPct)} < %${fmt(n.ndfMin)})")
        }

        if (n.caPRatio > 0) {
            if (n.caPRatio < 1.4) reasons.add("Ca:P dusuk (${fmt(n.caPRatio)}:1)")
            if (n.caPRatio > 3.0) reasons.add("Ca:P yuksek (${fmt(n.caPRatio)}:1)")
        }

        val score = computeScore(
            feedCount = usedFeedCount,
            requiredRange = requiredRange,
            missingCoreCount = missingCore.size,
            nutrientRatios = nutrientRatios,
            reasons = reasons,
            isHardConstraintFailed = isHardConstraintFailed
        )

        val limitingNutrient = when {
            n.meTarget > 0 && n.meMj < n.meTarget * 0.97 -> "ME"
            n.nelTarget > 0 && n.nelMj < n.nelTarget * 0.97 -> "NEL"
            n.cpTarget > 0 && n.cpG < n.cpTarget * 0.97 -> "Ham Protein"
            n.mgTarget > 0 && n.mgG < n.mgTarget * 0.97 -> "Magnezyum"
            n.caTarget > 0 && n.caG < n.caTarget * 0.97 -> "Kalsiyum"
            n.pTarget > 0 && n.pG < n.pTarget * 0.97 -> "Fosfor"
            else -> result.yield.limitingNutrient.ifBlank { "Kısıtlar" }
        }

        return BrillAssessment(
            passes = reasons.isEmpty(),
            score = score,
            feedCount = usedFeedCount,
            requiredRange = requiredRange,
            missingCoreCategories = missingCore,
            limitingNutrient = limitingNutrient,
            nutrientRatios = nutrientRatios,
            reasons = reasons
        )
    }

    fun coreCategoriesForSpecies(species: String): Set<String> = when (species) {
        AnimalSpecies.SIGIR,
        AnimalSpecies.MANDA -> setOf(
            FeedCategories.ROUGHAGE_WET,
            FeedCategories.ROUGHAGE_DRY,
            FeedCategories.GRAIN,
            FeedCategories.PROTEIN,
            FeedCategories.MINERAL
        )

        AnimalSpecies.KOYUN,
        AnimalSpecies.KECI -> setOf(
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

    private fun computeScore(
        feedCount: Int,
        requiredRange: IntRange,
        missingCoreCount: Int,
        nutrientRatios: Map<String, Double>,
        reasons: List<String>,
        isHardConstraintFailed: Boolean = false
    ): Int {
        var score = 100.0

        // Hard constraint ihlali büyük penalti
        if (isHardConstraintFailed) {
            score -= 50.0
        }

        if (feedCount !in requiredRange) {
            val distance = when {
                feedCount < requiredRange.first -> requiredRange.first - feedCount
                feedCount > requiredRange.last -> feedCount - requiredRange.last
                else -> 0
            }
            score -= 18 + (distance * 4)
        }

        score -= missingCoreCount * 18.0

        nutrientRatios.forEach { (name, value) ->
            when {
                value < 0.95 -> score -= (0.95 - value) * 140.0
                value > 1.45 && name in setOf("Kalsiyum", "Fosfor", "Magnezyum") -> score -= (value - 1.45) * 60.0
                value > 1.25 && name == "Ham Protein" -> score -= (value - 1.25) * 50.0
                value < 0.80 -> score -= 10.0
            }
        }

        if (reasons.isNotEmpty()) score -= reasons.size * 2.0
        return score.roundToInt().coerceIn(0, 100)
    }

    private fun fmt(value: Double): String = String.format("%.1f", value)
}