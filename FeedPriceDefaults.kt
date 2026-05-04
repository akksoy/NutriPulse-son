package com.nutripulse.app.data

import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories
import java.util.Locale

object FeedPriceDefaults {

    // Türkiye 2025-2026 piyasa fiyatları (TL/kg taze ağırlık)
    // Kaynak: Türkiye Yem Sanayicileri Birliği, al-ver.com, tarimziraat.com
    private val categoryDefaultPrices = mapOf(
        FeedCategories.ROUGHAGE_WET to 4.00,   // Sulu kaba yemler (silaj vb.)
        FeedCategories.ROUGHAGE_DRY to 6.00,    // Kuru kaba yemler (ot, saman)
        FeedCategories.GRAIN to 10.00,          // Tahıllar
        FeedCategories.PROTEIN to 15.00,        // Protein kaynakları
        FeedCategories.BYPRODUCT to 7.00,         // Yan ürünler
        FeedCategories.FAT to 30.00,            // Yağ kaynakları
        FeedCategories.MINERAL to 10.00,         // Mineral kaynakları
        FeedCategories.VITAMIN to 100.00,       // Vitamin kaynakları
        FeedCategories.PREMIKS to 45.00,        // Premiksler
        FeedCategories.ADDITIVE to 70.00,       // Katkı maddeleri
        FeedCategories.AQUA to 25.00            // Su ürünleri
    )

    // Yem adına göre spesifik fiyatlar (TL/kg) - 2025-2026 Türkiye Piyasa Fiyatları
    // Kaynak: Türkiye Yem Sanayicileri Birliği, al-ver.com, tarimziraat.com
    // Not: Fiyatlar ton başına verilmiştir, TL/kg'a çevrilmiştir
    private val specificFeedPrices = mapOf(
        // KABA YEMLER - SULU (TL/kg)
        "Misir Silaji" to 3.50,
        "Yonca Silaji" to 5.00,
        "Sorgum Silaji" to 3.00,
        // KABA YEMLER - KURU
        "Yonca Kuru Otu Kalite1" to 8.50,
        "Yonca Kuru Otu Kalite2" to 7.00,
        "Yonca Kuru Otu Kalite3" to 5.50,
        "Bugday Saman" to 2.50,
        "Arpa Saman" to 2.50,
        
        // TANE YEMLERİ (TL/kg) - 2025 mart fiyatları
        "Arpa" to 9.50,
        "Misir Dane" to 10.00,
        "Misir Kirmasi" to 9.50,
        "Bugday" to 10.00,
        "Yulaf" to 11.00,
        "Sorgum Dane" to 9.00,
        
        // PROTEİN YEMLERİ
        "Soya Kuspesi 44" to 17.00,
        "Soya Kuspesi 48" to 18.00,
        "Aycicek Kuspesi 36" to 12.50,
        "Pamuk Tohumu Kuspesi" to 13.00,
        "Kanola Kuspesi" to 14.00,
        "Kan Unu" to 25.00,
        
        // YAN ÜRÜNLER (Fabrika için en önemli)
        "Misir DDG" to 8.50,
        "DDGS Misir" to 9.00,
        "Misir Gluten Yemi" to 8.00,
        "Melas Seker Pancari" to 6.00,
        "Bira Posasi Kuru" to 6.50,
        "Seker Pancari Posasi Kuru" to 4.50,
        "Bugday Kepegi" to 5.80,
        
        // YAĞLAR
        "Aycicek Yagi" to 32.00,
        "Misir Yagi" to 32.00,
        "Soya Yagi" to 30.00,
        
        // MİNERALLER
        "DCP" to 22.00,
        "Dikalsiyum Fosfat" to 24.00,
        "Kirec Tasi CaCO3" to 3.50,
        "Kirec Tasi Unu" to 3.50,
        "Mermer Tozu" to 3.00,
        "Sodyum Klorur Tuz" to 4.00,
        "Sodyum Bikarbonat" to 10.00,
        "Magnezyum Oksit" to 18.00,
        
        // PREMİKSLER
        "Sut Inegi Premiksi" to 45.00,
        "Gecis Donemi Premiksi" to 48.00,
        "Sut Inegi Mineral Blogu" to 40.00,
        
        // KATKI MADDELERİ
        "Toksin Baglayici" to 65.00,
        "Besleyici Maya" to 22.00
    )
    
    // Yüksek Ca/P içeren yemler için ceza fiyatı (TL/kg)
    private val highMineralFeedPenalty = mapOf(
        "Tavuk Gubres Islenmis" to 6.00,
        "Tavuk Pislik Islenmis" to 6.00,
        "Tavuk Guibres Islenmis Peleti" to 6.00,
        "Tuz Ruhu Kalsiyum" to 5.00
    )

    fun getDefaultPriceForFeed(feed: Feed): Double {
        // Önce spesifik fiyata bak
        specificFeedPrices[feed.name]?.let { return it }
        
        // Yüksek mineral ceza kontrolü
        highMineralFeedPenalty.entries.find { feed.name.contains(it.key, ignoreCase = true) }?.let { 
            return it.value 
        }
        
        // Sonra kategori varsayılanına bak
        categoryDefaultPrices[feed.category]?.let { return it }
        
        // Hiçbiri yoksa genel varsayılan
        return 12.00
    }

    fun getPricePerKgDm(feed: Feed): Double {
        val pricePerKg = if (feed.pricePerKg > 0.0) {
            feed.pricePerKg
        } else {
            getDefaultPriceForFeed(feed)
        }
        val dmFrac = (feed.dm / 100.0).coerceAtLeast(0.01)
        return pricePerKg / dmFrac
    }

    fun suggestPrices(feeds: List<Feed>, refs: Map<String, Double>): Map<Int, Double> {
        val normalizedRefs = refs.entries.associate { normalizeName(it.key) to it.value }
        val byCategory = mutableMapOf<String, MutableList<Double>>()

        fun findRef(name: String): Double? {
            return refs[name] ?: normalizedRefs[normalizeName(name)]
        }

        feeds.forEach { feed ->
            val price = findRef(feed.name)
            if (price != null && price > 0.0) {
                byCategory.getOrPut(feed.category) { mutableListOf() }.add(price)
            }
        }

        val categoryAvg = byCategory.mapValues { (_, prices) -> prices.average() }
        val globalAvg = refs.values.filter { it > 0.0 }.average().takeIf { !it.isNaN() }

        return feeds.associate { feed ->
            val suggested = when {
                feed.pricePerKg > 0.0 -> feed.pricePerKg
                else -> findRef(feed.name)
                    ?: specificFeedPrices[feed.name]
                    ?: categoryAvg[feed.category]
                    ?: categoryDefaultPrices[feed.category]
                    ?: globalAvg
                    ?: 10.0
            }
            feed.id to suggested
        }
    }

    private fun normalizeName(raw: String): String {
        return raw.lowercase(Locale.ROOT)
            .replace("ı", "i")
            .replace("ğ", "g")
            .replace("ş", "s")
            .replace("ç", "c")
            .replace("ö", "o")
            .replace("ü", "u")
            .replace("â", "a")
            .replace("î", "i")
            .replace("û", "u")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
    }
}
