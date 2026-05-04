package com.nutripulse.app.data

import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories

object FeedSelectionDefaults {

    fun buildStarterSelection(
        feeds: List<Feed>,
        limit: Int = 20,
        categoryCaps: Map<String, Int>? = null,
        requiredCategories: List<String> = emptyList()
    ): List<Feed> {
        if (feeds.isEmpty() || limit <= 0) return emptyList()

        val caps = categoryCaps.orEmpty()
        val selectedIds = linkedSetOf<Int>()
        val selectedByCategory = mutableMapOf<String, Int>()
        val result = mutableListOf<Feed>()

        fun addFeed(feed: Feed) {
            val currentCategoryCount = selectedByCategory[feed.category] ?: 0
            val categoryCap = caps[feed.category]
            if (result.size >= limit) return
            if (selectedIds.contains(feed.id)) return
            if (categoryCap != null && currentCategoryCount >= categoryCap) return
            selectedIds.add(feed.id)
            selectedByCategory[feed.category] = currentCategoryCount + 1
            result.add(feed)
        }

        fun score(feed: Feed): Double {
            val energy = maxOf(feed.nel, feed.me * 0.55, 0.0)
            val protein = feed.cp * 0.12 + feed.lys * 0.05 + feed.met * 0.05
            val fiber = feed.ndf * 0.06 + feed.adf * 0.03
            val minerals = (feed.ca + feed.p + feed.mg + feed.na + feed.k + feed.s) * 0.08
            val fat = feed.fat * 0.08
            val pricePenalty = if (feed.pricePerKg > 0) feed.pricePerKg * 0.16 else 0.0
            val categoryBias = when (feed.category) {
                FeedCategories.ROUGHAGE_WET -> 1.0
                FeedCategories.ROUGHAGE_DRY -> 1.1
                FeedCategories.GRAIN -> 1.0
                FeedCategories.PROTEIN -> 1.2
                FeedCategories.BYPRODUCT -> 0.9
                FeedCategories.FAT -> 0.8
                FeedCategories.MINERAL, FeedCategories.PREMIKS, FeedCategories.VITAMIN -> 1.25
                FeedCategories.ADDITIVE -> 0.6
                FeedCategories.AQUA -> 1.15
                else -> 0.8
            }
            return ((energy + protein + fiber + minerals + fat) * categoryBias) - pricePenalty
        }

        fun sortedByScore(items: List<Feed>) = items.sortedWith(
            compareByDescending<Feed> { score(it) }
                .thenBy { if (it.pricePerKg > 0) it.pricePerKg else Double.MAX_VALUE }
                .thenBy { it.name }
        )

        val categoryOrder = listOf(
            FeedCategories.ROUGHAGE_WET,
            FeedCategories.ROUGHAGE_DRY,
            FeedCategories.GRAIN,
            FeedCategories.PROTEIN,
            FeedCategories.BYPRODUCT,
            FeedCategories.FAT,
            FeedCategories.MINERAL,
            FeedCategories.PREMIKS,
            FeedCategories.VITAMIN,
            FeedCategories.ADDITIVE,
            FeedCategories.AQUA
        )

        val grouped = feeds.groupBy { it.category }
        val targetLimit = minOf(limit, feeds.size)

        // 1) Zorunlu görülen kategorilerden en iyi adayı al.
        requiredCategories.forEach { category ->
            if (result.size >= targetLimit) return@forEach
            val best = sortedByScore(grouped[category].orEmpty()).firstOrNull()
            if (best != null) addFeed(best)
        }

        // 2) Kategori kotalarına göre tur tur ekle; böylece tek gruba yığılmaz.
        val categoriesWithCandidates = categoryOrder.filter { grouped[it].orEmpty().isNotEmpty() }
        var progressed: Boolean
        do {
            progressed = false
            for (category in categoriesWithCandidates) {
                if (result.size >= targetLimit) break
                val categoryItems = sortedByScore(grouped[category].orEmpty())
                val currentCount = selectedByCategory[category] ?: 0
                val categoryCap = caps[category] ?: when (category) {
                    FeedCategories.ROUGHAGE_WET -> 2
                    FeedCategories.ROUGHAGE_DRY -> 3
                    FeedCategories.GRAIN -> 3
                    FeedCategories.PROTEIN -> 3
                    FeedCategories.BYPRODUCT -> 2
                    FeedCategories.FAT -> 1
                    FeedCategories.MINERAL -> 2
                    FeedCategories.PREMIKS -> 1
                    FeedCategories.VITAMIN -> 1
                    FeedCategories.ADDITIVE -> 1
                    FeedCategories.AQUA -> 2
                    else -> 1
                }

                if (currentCount >= categoryCap) continue

                val next = categoryItems.firstOrNull { it.id !in selectedIds }
                if (next != null) {
                    addFeed(next)
                    progressed = true
                }
            }
        } while (progressed && result.size < targetLimit)

        // 3) Hâlâ boşluk varsa, kalan en iyi adaylarla doldur ama kategori başına yumuşak tavanı koru.
        if (result.size < targetLimit) {
            sortedByScore(feeds).forEach { feed ->
                if (result.size >= targetLimit) return@forEach
                val categoryCap = caps[feed.category] ?: Int.MAX_VALUE
                if ((selectedByCategory[feed.category] ?: 0) >= categoryCap) return@forEach
                addFeed(feed)
            }
        }

        // 4) Kritik mikronutrient gruplarından en az bir temsilci olsun.
        val criticalCats = setOf(FeedCategories.MINERAL, FeedCategories.PREMIKS, FeedCategories.VITAMIN)
        if (result.none { it.category in criticalCats }) {
            val replacement = sortedByScore(feeds.filter { it.category in criticalCats }).firstOrNull()
            if (replacement != null) {
                if (result.size < targetLimit) {
                    addFeed(replacement)
                } else {
                    val removableIndex = result.indexOfLast { it.category !in criticalCats }
                    if (removableIndex >= 0) {
                        val removed = result.removeAt(removableIndex)
                        selectedIds.remove(removed.id)
                        selectedByCategory[removed.category] = (selectedByCategory[removed.category] ?: 1) - 1
                        addFeed(replacement)
                    }
                }
            }
        }

        return result
    }
}

// Bu dosya artık kullanılmıyor. Yem seçimi motoru FeedSelectionEngine ile yapılacak. Koddan kaldırıldı.