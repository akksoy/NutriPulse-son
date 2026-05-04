package com.nutripulse.app.data

import android.content.Context
import kotlin.math.round
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PriceSyncManager {

    data class SyncResult(
        val totalFeeds: Int,
        val updatedFeeds: Int,
        val referencePriceCount: Int,
        val source: String,
        val success: Boolean
    )

    suspend fun syncAllFeedPrices(context: Context): SyncResult = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getDatabase(context).feedDao()
        val feeds = dao.getAllFeedsSnapshot()
        if (feeds.isEmpty()) {
            return@withContext SyncResult(0, 0, 0, "No Data", false)
        }

        val fetchResult = TmoFetcher.fetchPrices()
        val refs = TmoFetcher.getCurrent2026Prices().toMutableMap()
        refs.putAll(fetchResult.prices)

        val suggestions = FeedPriceDefaults.suggestPrices(feeds, refs)
        var updated = 0

        feeds.forEach { feed ->
            val suggested = suggestions[feed.id] ?: 0.0
            if (suggested > 0.0) {
                val rounded = round(suggested * 10.0) / 10.0
                if (feed.pricePerKg != rounded) {
                    dao.updatePrice(feed.id, rounded)
                    updated += 1
                }
            }
        }

        SyncResult(
            totalFeeds = feeds.size,
            updatedFeeds = updated,
            referencePriceCount = refs.size,
            source = fetchResult.source,
            success = fetchResult.success
        )
    }
}