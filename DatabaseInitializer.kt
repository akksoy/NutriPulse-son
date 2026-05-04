package com.nutripulse.app.data

import com.nutripulse.app.data.dao.FeedDao

object DatabaseInitializer {
    suspend fun populateFeeds(feedDao: FeedDao) {
        val seedFeeds = listOf(
            FeedData1.getFeeds(), FeedData2.getFeeds(), FeedData3.getFeeds(),
            FeedData4.getFeeds(), FeedData5.getFeeds(), FeedData6.getFeeds(),
            FeedData7.getFeeds(), FeedData8.getFeeds(), FeedData9.getFeeds(),
            FeedData10.getFeeds()
        ).flatten()

        fun key(name: String, category: String): String =
            "${name.trim().lowercase()}|${category.trim()}"

        val existing = feedDao.getAllFeedsSnapshot()

        // Keep first row per logical feed key, remove accidental duplicates.
        val seen = mutableSetOf<String>()
        existing.forEach { row ->
            val k = key(row.name, row.category)
            if (!seen.add(k)) {
                feedDao.deleteFeed(row)
            }
        }

        val existingKeys = feedDao.getAllFeedsSnapshot()
            .asSequence()
            .map { key(it.name, it.category) }
            .toHashSet()

        val missing = seedFeeds.filter { key(it.name, it.category) !in existingKeys }
        if (missing.isNotEmpty()) {
            feedDao.insertAll(missing)
        }
    }
}