package com.nutripulse.app.data.solver.impl

import com.nutripulse.app.data.model.AnimalProfile
import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories

/**
 * NutriPulse — Çözülebilirlik Kontrolü
 * Simplex çalışmadan önce rasyon kurulumunu kontrol eder.
 * Olası sorunları önceden tespit eder.
 */
object FeasibilityChecker {

    data class CheckResult(
        val isPossible: Boolean,
        val warnings: List<String>,
        val errors: List<String>,
        val suggestions: List<String>
    )

    fun check(animal: AnimalProfile, feeds: List<Feed>, rationMode: String = "TMR"): CheckResult {
        val warnings = mutableListOf<String>()
        val errors   = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        val profile = SpeciesOptimizationProfiles.forAnimal(animal)

        // ── 1. Yem listesi kontrolü ─────────────────────────
        if (feeds.isEmpty()) {
            errors.add("❌ Yem listesi boş! En az 5–10 yem seçin.")
            return CheckResult(false, warnings, errors, suggestions)
        }

        val minFeedGuidance = profile.minFeedCountWarning
        if (feeds.size < minFeedGuidance) {
            warnings.add("⚠ Yem çeşidi düşük (${feeds.size}). ${profile.key} için en az $minFeedGuidance yem önerilir.")
        }

        val categories = feeds.map { it.category }.toSet()
        fun requireCategory(cat: String, warning: String) {
            if (cat !in categories) warnings.add(warning)
        }

        when (profile.key) {
            "DAIRY" -> {
                // Fabrika modunda kaba yem kontrolü yapma
                if (rationMode != "FABRIKA") {
                    requireCategory(FeedCategories.ROUGHAGE_WET, "⚠ Süt rasyonu için sulu kaba yem eksik görünüyor.")
                    requireCategory(FeedCategories.ROUGHAGE_DRY, "⚠ Süt rasyonu için kuru kaba yem eksik görünüyor.")
                }
                requireCategory(FeedCategories.PROTEIN, "⚠ Süt rasyonu için protein kaynağı eksik görünüyor.")
                requireCategory(FeedCategories.GRAIN, "⚠ Süt rasyonu için tahıl/kesif kaynakları eksik görünüyor.")
            }
            "SMALL_RUMINANT" -> {
                requireCategory(FeedCategories.ROUGHAGE_DRY, "⚠ Küçükbaş rasyonu için kuru kaba yem eksik görünüyor.")
                requireCategory(FeedCategories.PROTEIN, "⚠ Küçükbaş rasyonu için protein kaynağı eksik görünüyor.")
            }
            "HORSE" -> {
                requireCategory(FeedCategories.ROUGHAGE_DRY, "⚠ At rasyonu için kuru kaba yem (ot/saman) olmalı.")
            }
            "POULTRY" -> {
                requireCategory(FeedCategories.GRAIN, "⚠ Kanatlı rasyonu için tahıl bazlı enerji kaynağı eksik.")
                requireCategory(FeedCategories.PROTEIN, "⚠ Kanatlı rasyonu için protein kaynağı eksik.")
                requireCategory(FeedCategories.MINERAL, "⚠ Kanatlı rasyonu için mineral kaynağı eksik.")
            }
            "AQUA" -> {
                requireCategory(FeedCategories.AQUA, "⚠ Su ürünleri için AQUA kategorisinden temel yem ekleyin.")
                requireCategory(FeedCategories.PROTEIN, "⚠ Su ürünleri için protein kaynağı eksik.")
            }
            "RABBIT" -> {
                requireCategory(FeedCategories.ROUGHAGE_DRY, "⚠ Tavşan rasyonu için lifli kuru kaba yem gerekli.")
            }
        }

        // ── 2. Hayvan ihtiyaçları kontrolü ──────────────────
        if (animal.reqDmKg <= 0) {
            errors.add("❌ Hayvan KM ihtiyacı hesaplanmamış! Önce NRC hesaplayın.")
            return CheckResult(false, warnings, errors, suggestions)
        }
        if (animal.reqNelMj <= 0 && animal.reqMeMj <= 0) {
            errors.add("❌ Enerji ihtiyacı (NEL/ME) sıfır! NRC hesaplamayı kontrol edin.")
        }

        // ── 3. Fiyat kontrolü ────────────────────────────────
        val feedsWithoutPrice = feeds.filter { it.pricePerKg <= 0 }
        if (feedsWithoutPrice.size > feeds.size / 2) {
            warnings.add("⚠ ${feedsWithoutPrice.size} yemin fiyatı girilmemiş. Optimizasyon varsayılan fiyatla çalışacak.")
            suggestions.add("💡 Fiyat Yönetimi'nden yem fiyatlarını güncelleyin.")
        }

        // KM birimi (0-1 fraksiyon) girilmisse cevirim kaynakli asiri kg riski olusur.
        val dmFractionFeeds = feeds.filter { it.dm in 0.0001..1.0 }
        if (dmFractionFeeds.isNotEmpty()) {
            warnings.add("⚠ ${dmFractionFeeds.size} yemde KM değeri 0-1 aralığında. Bu değerlerin yüzde mi fraksiyon mu olduğunu kontrol edin.")
            suggestions.add("💡 KM girişlerini genellikle yüzde formatında (örn. 87) kullanın.")
        }

        // ── 4. NEL kapasitesi kontrolü ───────────────────────
        val maxNelPerKg = feeds.maxOfOrNull { it.nel } ?: 0.0
        val requiredNelPerKgDm = if (animal.reqDmKg > 0) animal.reqNelMj / animal.reqDmKg else 0.0
        if (maxNelPerKg < requiredNelPerKgDm * 0.7) {
            warnings.add("⚠ Seçili yemlerin max NEL değeri (${"%.1f".format(maxNelPerKg)} MJ/kg) ihtiyacın altında.")
            suggestions.add("💡 Enerji açısından zengin yem ekleyin (mısır silajı, tahıl, yağ).")
        }

        // ── 5. Protein kapasitesi kontrolü ───────────────────
        val maxCp = feeds.maxOfOrNull { it.cp } ?: 0.0
        val requiredCpPct = if (animal.reqDmKg > 0) animal.reqCpG / (animal.reqDmKg * 10) else 0.0
        if (maxCp < requiredCpPct * 0.8 && requiredCpPct > 0) {
            warnings.add("⚠ Protein kaynağı yetersiz görünüyor. Max HP: ${"%.1f".format(maxCp)}%")
            suggestions.add("💡 Soya küspesi, balık unu veya başka protein kaynağı ekleyin.")
        }

        // ── 6. NDF kapasitesi ────────────────────────────────
        val roughageFeeds = feeds.filter { it.ndf >= 35.0 }
        if (roughageFeeds.isEmpty()) {
            warnings.add("⚠ Yüksek NDF'li kaba yem yok. NDF alt sınırı sağlanamayabilir.")
            suggestions.add("💡 Silaj, saman veya kuru ot ekleyin.")
        }

        // ── 7. Mineral kaynağı ───────────────────────────────
        val hasCaSource = feeds.any { it.ca >= 5.0 }
        if (!hasCaSource && animal.reqCaG > 30) {
            warnings.add("⚠ Yeterli Ca kaynağı yok. Kireçtaşı veya dikalsiyum fosfat ekleyin.")
        }

        // ── 8. Toplam max kapasite ───────────────────────────
        val totalMaxDm = feeds.sumOf { f ->
            when {
                f.maxDailyKg > 0 -> f.maxDailyKg * (f.dm / 100.0)
                f.maxDmPct > 0   -> animal.reqDmKg * f.maxDmPct / 100.0
                else             -> animal.reqDmKg  // kısıtsız
            }
        }
        if (totalMaxDm < animal.reqDmKg * 0.85) {
            errors.add("❌ Yem maksimum kısıtları toplam KM ihtiyacını karşılamıyor!")
            suggestions.add("💡 Yem max sınırlarını artırın veya daha fazla yem ekleyin.")
        }

        // ── 9. NEL kapasitesi ile roughage çelişkisi kontrolü ───────────
        val roughageByCat = feeds.filter { it.category == FeedCategories.ROUGHAGE_WET || it.category == FeedCategories.ROUGHAGE_DRY }
        val highNelFeeds = feeds.filter { it.nel > 7.0 }
        if (roughageByCat.isNotEmpty() && highNelFeeds.isEmpty()) {
            val avgRoughageNel = roughageByCat.map { it.nel }.average()
            val requiredNelPerKg = if (animal.reqDmKg > 0) animal.reqNelMj / animal.reqDmKg else 0.0
            if (avgRoughageNel < requiredNelPerKg * 0.9) {
                warnings.add("⚠ NEL hedefi ($requiredNelPerKg MJ/kg) sadece kaba yemlerle karşılanamaz. Enerji yemi ekleyin.")
                suggestions.add("💡 Arpa, mısır, kepek gibi enerji kaynakları ekleyin.")
            }
        }

        // ── 10. Protein-mineral dengesi kontrolü ─────────────────────────
        val proteinFeeds = feeds.filter { it.cp > 20.0 }
        val mineralFeeds = feeds.filter { it.ca > 5.0 || it.p > 5.0 }
        val reqCpPctForCheck = if (animal.reqDmKg > 0) animal.reqCpG / (animal.reqDmKg * 10) else 0.0
        if (reqCpPctForCheck > 16.0 && proteinFeeds.isEmpty()) {
            warnings.add("⚠ HP hedefi yüksek ($reqCpPctForCheck%) ama yeterli protein kaynağı yok.")
            suggestions.add("💡 Soya küspesi, ayçiçeği küspesi, pamuk tohumu ekleyin.")
        }

        // ── 11. Ca:P dengesi için mineral kaynağı ────────────────────────
        if (mineralFeeds.isEmpty() && (animal.reqCaG > 50 || animal.reqPG > 30)) {
            warnings.add("⚠ Kalsiyum/Fosfor ihtiyacı için mineral kaynağı yok.")
            suggestions.add("💡 Kireçtaşı, dikalsiyum fosfat veya premiks ekleyin.")
        }

        // ── 12. İnfeasibility erken uyarı sistemi ─────────────────────────
        val nelMax = feeds.maxOfOrNull { it.nel } ?: 0.0
        val requiredNelPerKg = if (animal.reqDmKg > 0) animal.reqNelMj / animal.reqDmKg else 0.0
        
        if (nelMax < requiredNelPerKg * 0.85) {
            errors.add("❌ NEL hedefi karşılanamaz! Max NEL: $nelMax MJ/kg, Hedef: $requiredNelPerKg MJ/kg")
            suggestions.add("💡 Daha yüksek enerjili yem ekleyin (mısır, arpa, yağ).")
        }

        val cpMax = feeds.maxOfOrNull { it.cp } ?: 0.0
        if (cpMax < requiredCpPct * 0.75 && requiredCpPct > 0) {
            errors.add("❌ HP hedefi karşılanamaz! Max HP: $cpMax%, Hedef: $requiredCpPct%")
            suggestions.add("💡 Protein kaynağı ekleyin veya mevcut yemlerin HP değerlerini kontrol edin.")
        }

        val isPossible = errors.isEmpty()
        return CheckResult(isPossible, warnings, errors, suggestions)
    }
}