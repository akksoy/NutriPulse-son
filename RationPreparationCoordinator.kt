package com.nutripulse.app.ui.ration

import com.nutripulse.app.data.model.AnimalProfile
import com.nutripulse.app.data.model.AnimalSpecies
import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories
import com.nutripulse.app.data.solver.SmartFeedSelector

class RationPreparationCoordinator {

    fun buildStepHint(animal: AnimalProfile?, candidateCount: Int, finalCount: Int): String {
        if (animal == null) return "Adim 1/4: hayvan bilgisi sec"
        return when {
            candidateCount <= 0 -> "Adim 2/4: yem havuzunu YEMLER ile olustur"
            finalCount in 8..14 -> "Adim 3/4: rasyonu cozdur ve analiz ekranina gec"
            finalCount < 8 -> "Adim 3/4: final rasyon icin 8-14 yem sec"
            else -> "Adim 3/4: final rasyon fazla buyuk, 8-14'e indir"
        }
    }

    fun buildStarterFeeds(animal: AnimalProfile, allFeeds: List<Feed>, limit: Int = 40): List<Feed> {
        if (allFeeds.isEmpty()) return emptyList()

        val plan = starterPlanForSpecies(animal.species)
        val allowedCategories = plan.categoryCaps.keys.toSet()
        val candidates = allFeeds.filter { it.category in allowedCategories }
        if (candidates.isEmpty()) return emptyList()

        return SmartFeedSelector.buildCandidatePool(
            animal = animal,
            allFeeds = candidates,
            config = SmartFeedSelector.SelectionConfig(
                limit = limit.coerceAtLeast(plan.minItems),
                attempt = 0,
                allowedCategories = allowedCategories,
                requiredCategories = plan.requiredCategories.toSet(),
                categoryCaps = plan.categoryCaps
            )
        )
    }

    fun isFinalSelectionReady(selectedFeedCount: Int): Boolean = selectedFeedCount in 8..14

    fun buildSelectionSummary(selectedFeeds: List<Feed>): String {
        if (selectedFeeds.isEmpty()) return "Aday havuz bos"

        val names = selectedFeeds.take(6).joinToString(", ") { it.name }
        val suffix = if (selectedFeeds.size > 6) " +${selectedFeeds.size - 6} daha" else ""
        val categories = selectedFeeds.groupBy { it.category }.size
        return "${selectedFeeds.size} yem / $categories grup: $names$suffix"
    }

    fun buildOptimizationCandidatePool(
        animal: AnimalProfile,
        allFeeds: List<Feed>,
        attempt: Int,
        limit: Int = 16
    ): List<Feed> {
        if (allFeeds.isEmpty()) return emptyList()

        val allowed = allowedCategoriesForSpecies(animal.species)
        val filtered = allFeeds.filter { it.category in allowed }
        if (filtered.isEmpty()) return emptyList()

        val profile = starterPlanForSpecies(animal.species)
        return SmartFeedSelector.buildCandidatePool(
            animal = animal,
            allFeeds = filtered,
            config = SmartFeedSelector.SelectionConfig(
                limit = limit,
                attempt = attempt,
                allowedCategories = allowed,
                requiredCategories = profile.requiredCategories.toSet(),
                categoryCaps = profile.categoryCaps
            )
        )
    }

    fun buildOptimizationCandidateAttempts(
        animal: AnimalProfile,
        allFeeds: List<Feed>,
        attempts: Int = 3,
        limit: Int = 16
    ): List<List<Feed>> {
        return (0 until attempts)
            .map { buildOptimizationCandidatePool(animal, allFeeds, it, limit) }
            .distinctBy { it.map { f -> f.id }.sorted() }
            .filter { it.isNotEmpty() }
    }

    private fun allowedCategoriesForSpecies(species: String): Set<String> {
        val plan = starterPlanForSpecies(species)
        return plan.categoryCaps.filterValues { it > 0 }.keys
    }

    private fun mainCategoriesForSpecies(species: String): Set<String> = when (species) {
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
        AnimalSpecies.KANATLI -> setOf(
            FeedCategories.GRAIN,
            FeedCategories.PROTEIN,
            FeedCategories.MINERAL
        )
        AnimalSpecies.AT -> setOf(
            FeedCategories.ROUGHAGE_WET,
            FeedCategories.ROUGHAGE_DRY,
            FeedCategories.GRAIN,
            FeedCategories.PROTEIN,
            FeedCategories.MINERAL
        )
        AnimalSpecies.SU -> setOf(
            FeedCategories.AQUA,
            FeedCategories.PROTEIN,
            FeedCategories.FAT,
            FeedCategories.MINERAL
        )
        AnimalSpecies.TAVSAN -> setOf(
            FeedCategories.ROUGHAGE_DRY,
            FeedCategories.GRAIN,
            FeedCategories.PROTEIN,
            FeedCategories.MINERAL
        )
        else -> setOf(
            FeedCategories.GRAIN,
            FeedCategories.PROTEIN,
            FeedCategories.MINERAL
        )
    }

    private fun starterPlanForSpecies(species: String): StarterPlan = when (species) {
        AnimalSpecies.SIGIR, AnimalSpecies.MANDA -> StarterPlan(
            minItems = 10,
            maxItems = 14,
            requiredCategories = listOf(
                FeedCategories.ROUGHAGE_WET,
                FeedCategories.ROUGHAGE_DRY,
                FeedCategories.GRAIN,
                FeedCategories.PROTEIN,
                FeedCategories.MINERAL
            ),
            categoryCaps = linkedMapOf(
                FeedCategories.ROUGHAGE_WET to 3,
                FeedCategories.ROUGHAGE_DRY to 3,
                FeedCategories.GRAIN to 3,
                FeedCategories.PROTEIN to 3,
                FeedCategories.BYPRODUCT to 0,
                FeedCategories.FAT to 1,
                FeedCategories.MINERAL to 2,
                FeedCategories.PREMIKS to 1,
                FeedCategories.VITAMIN to 1,
                FeedCategories.ADDITIVE to 1
            )
        )
        AnimalSpecies.KOYUN, AnimalSpecies.KECI -> StarterPlan(
            minItems = 10,
            maxItems = 14,
            requiredCategories = listOf(
                FeedCategories.ROUGHAGE_DRY,
                FeedCategories.GRAIN,
                FeedCategories.PROTEIN,
                FeedCategories.MINERAL
            ),
            categoryCaps = linkedMapOf(
                FeedCategories.ROUGHAGE_WET to 2,
                FeedCategories.ROUGHAGE_DRY to 3,
                FeedCategories.GRAIN to 3,
                FeedCategories.PROTEIN to 3,
                FeedCategories.BYPRODUCT to 2,
                FeedCategories.FAT to 2,
                FeedCategories.MINERAL to 2,
                FeedCategories.PREMIKS to 2,
                FeedCategories.VITAMIN to 2,
                FeedCategories.ADDITIVE to 2
            )
        )
        AnimalSpecies.KANATLI -> StarterPlan(
            minItems = 8,
            maxItems = 12,
            requiredCategories = listOf(
                FeedCategories.GRAIN,
                FeedCategories.PROTEIN,
                FeedCategories.MINERAL
            ),
            categoryCaps = linkedMapOf(
                FeedCategories.ROUGHAGE_WET to 0,
                FeedCategories.ROUGHAGE_DRY to 0,
                FeedCategories.GRAIN to 3,
                FeedCategories.PROTEIN to 3,
                FeedCategories.BYPRODUCT to 2,
                FeedCategories.FAT to 2,
                FeedCategories.MINERAL to 2,
                FeedCategories.PREMIKS to 2,
                FeedCategories.VITAMIN to 2,
                FeedCategories.ADDITIVE to 2
            )
        )
        AnimalSpecies.AT -> StarterPlan(
            minItems = 10,
            maxItems = 14,
            requiredCategories = listOf(
                FeedCategories.ROUGHAGE_WET,
                FeedCategories.ROUGHAGE_DRY,
                FeedCategories.GRAIN,
                FeedCategories.PROTEIN,
                FeedCategories.MINERAL
            ),
            categoryCaps = linkedMapOf(
                FeedCategories.ROUGHAGE_WET to 3,
                FeedCategories.ROUGHAGE_DRY to 3,
                FeedCategories.GRAIN to 3,
                FeedCategories.PROTEIN to 2,
                FeedCategories.BYPRODUCT to 2,
                FeedCategories.FAT to 1,
                FeedCategories.MINERAL to 2,
                FeedCategories.PREMIKS to 1,
                FeedCategories.VITAMIN to 1,
                FeedCategories.ADDITIVE to 1
            )
        )
        AnimalSpecies.SU -> StarterPlan(
            minItems = 8,
            maxItems = 12,
            requiredCategories = listOf(
                FeedCategories.AQUA,
                FeedCategories.PROTEIN,
                FeedCategories.MINERAL
            ),
            categoryCaps = linkedMapOf(
                FeedCategories.AQUA to 4,
                FeedCategories.PROTEIN to 3,
                FeedCategories.FAT to 2,
                FeedCategories.MINERAL to 2,
                FeedCategories.PREMIKS to 2,
                FeedCategories.VITAMIN to 1,
                FeedCategories.ADDITIVE to 2
            )
        )
        AnimalSpecies.TAVSAN -> StarterPlan(
            minItems = 8,
            maxItems = 12,
            requiredCategories = listOf(
                FeedCategories.ROUGHAGE_DRY,
                FeedCategories.GRAIN,
                FeedCategories.PROTEIN,
                FeedCategories.MINERAL
            ),
            categoryCaps = linkedMapOf(
                FeedCategories.ROUGHAGE_WET to 1,
                FeedCategories.ROUGHAGE_DRY to 3,
                FeedCategories.GRAIN to 3,
                FeedCategories.PROTEIN to 2,
                FeedCategories.BYPRODUCT to 2,
                FeedCategories.FAT to 1,
                FeedCategories.MINERAL to 2,
                FeedCategories.PREMIKS to 1,
                FeedCategories.VITAMIN to 1,
                FeedCategories.ADDITIVE to 1
            )
        )
        else -> StarterPlan(
            minItems = 8,
            maxItems = 12,
            requiredCategories = listOf(
                FeedCategories.GRAIN,
                FeedCategories.PROTEIN,
                FeedCategories.MINERAL
            ),
            categoryCaps = linkedMapOf(
                FeedCategories.GRAIN to 3,
                FeedCategories.PROTEIN to 3,
                FeedCategories.BYPRODUCT to 2,
                FeedCategories.FAT to 1,
                FeedCategories.MINERAL to 2,
                FeedCategories.PREMIKS to 1,
                FeedCategories.VITAMIN to 1,
                FeedCategories.ADDITIVE to 1
            )
        )
    }

    fun finalFeedRange(): IntRange = 8..14

    fun coreCategoriesForSpecies(species: String): Set<String> = mainCategoriesForSpecies(species)

    private data class StarterPlan(
        val minItems: Int,
        val maxItems: Int,
        val requiredCategories: List<String>,
        val categoryCaps: Map<String, Int>
    )
}