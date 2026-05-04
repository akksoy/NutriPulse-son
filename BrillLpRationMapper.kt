package com.nutripulse.app.data.solver.impl

import com.nutripulse.app.data.model.AnimalProfile
import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories
import com.nutripulse.app.data.model.RationItem
import kotlin.math.max

/**
 * Brill LP çözümünü (x = kgDM/gün) RationItem listesine çevirir.
 * Bu dosya sadece "map" işini yapar ki RationOptimizer içine gömmek zorunda kalmayalım.
 */
object BrillLpRationMapper {

    data class Mapped(
        val items: List<RationItem>,
        val totalCost: Double,
        val message: String
    )

    fun mapSolutionToItems(
        feeds: List<Feed>,
        xKgDm: DoubleArray,
        dmTargetKg: Double,
        practicalInclusionDmKg: Double = 0.01
    ): Mapped {
        require(feeds.size == xKgDm.size) { "feeds ve x boyutu ayni olmali" }

        fun dmCost(f: Feed): Double = BrillUnits.dmCostTlPerKgDm(f)

        val actDm = xKgDm.sum().coerceAtLeast(1e-6)

        val items = feeds.indices.mapNotNull { i ->
            val f = feeds[i]

            val inclusionThreshold = when (f.category) {
                FeedCategories.MINERAL,
                FeedCategories.PREMIKS,
                FeedCategories.VITAMIN,
                FeedCategories.ADDITIVE,
                FeedCategories.FAT -> 0.002
                else -> practicalInclusionDmKg
            }

            val dm = xKgDm[i]
            if (dm < inclusionThreshold) return@mapNotNull null

            val freshKg = dm / (f.dm / 100.0).coerceAtLeast(1e-6)
            RationItem(
                feedId = f.id,
                feedName = f.name,
                feedCategory = f.category,
                amountKg = r3(freshKg),
                amountDmKg = r3(dm),
                dmPct = r2(dm / actDm * 100.0),
                costPerDay = r2(dm * dmCost(f)),
                pricePerKg = f.pricePerKg
            )
        }

        val totalCost = feeds.indices.sumOf { i -> xKgDm[i] * dmCost(feeds[i]) }

        val msg = buildString {
            append("Brill LP cozum: ${items.size} yem, ")
            append("KM ${r2(actDm)} kg (hedef ${r2(dmTargetKg)}), ")
            append("maliyet ${r2(totalCost)} TL/gun")
        }

        return Mapped(
            items = items,
            totalCost = r2(totalCost),
            message = msg
        )
    }

    private fun r2(x: Double) = kotlin.math.round(x * 100.0) / 100.0
    private fun r3(x: Double) = kotlin.math.round(x * 1000.0) / 1000.0
}