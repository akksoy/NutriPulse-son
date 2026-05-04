package com.nutripulse.app.engine

import com.nutripulse.app.data.FeedPriceDefaults
import com.nutripulse.app.data.model.AnimalProfile
import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories

/**
 * NutriPulse — Yem Seçim Motoru (Saf Kotlin, Android Uyumlu)
 *
 * DEĞIŞIKLIK: OrToolsFeedSelector (native .so bağımlılığı) ve
 * HiGhsFallbackOptimizer (Python subprocess) tamamen kaldırıldı.
 *
 * Yeni yaklaşım — NutriPulseFeedSelector:
 *   1. Kısıt filtresi  : İzin verilmeyen yemleri ele
 *   2. Çok kriterli skor: Besin yoğunluğu / fiyat
 *   3. Kategori güvencesi: Kaba yem + tahıl + protein zorunlu
 *   4. LP'ye en uygun havuz: Max MAX_TOTAL yem döner
 *
 * Bu seçilen yemler doğrudan RationOptimizer.optimize() →
 * BrillLpOptimizer → SimplexSolver zinciriyle çözülür.
 * Yem seçimi ile miktar hesabı aynı LP çağrısında gerçekleşir
 * (Brill Formulation yaklaşımı).
 */
class FeedSelectionEngine(
    private val feeds: List<Feed>,
    private val animalProfile: AnimalProfile,
    private val constraints: FeedSelectionConstraints,
    private val objectives: FeedSelectionObjectives
) {
    /**
     * Yem seçimi: kısıt filtresi → puanlama → kategori dengeli seçim.
     * Hiçbir native kütüphane gerektirmez.
     */
    fun selectFeeds(): List<Feed> {
        val filtered = feeds.filter { constraints.isAllowed(it, animalProfile) }
        if (filtered.isEmpty()) return emptyList()

        val scored = filtered
            .map { feed -> feed to objectives.score(feed, animalProfile) }
            .sortedByDescending { it.second }

        return NutriPulseFeedSelector.selectOptimal(scored, animalProfile)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
/**
 * Brill seviyesi saf Kotlin yem seçici.
 *
 * Algoritma (3 aşama):
 *   Aşama 1 — Kategori güvencesi:
 *     Kaba yem (yaş/kuru), Tahıl ve Protein kategorilerinden
 *     en az 1'er yem zorunlu olarak seçilir.
 *
 *   Aşama 2 — Mineral / Premiks zorunluluğu:
 *     Makro mineral kaynağı yoksa en yüksek skorlu mineral eklenir.
 *
 *   Aşama 3 — Kalan slotları doldur:
 *     Skor sıralamasına göre MAX_TOTAL'a kadar yem eklenir.
 *     Bir kategoride MAX_PER_CATEGORY'den fazla yem olmaz.
 *
 * Çıktı: BrillLpOptimizer'a verilecek yem havuzu (15 yem kadar).
 */
object NutriPulseFeedSelector {

    /** Havuza alınacak maksimum yem sayısı */
    private const val MAX_TOTAL = 15

    /** Bir kategoriden alınabilecek maksimum yem sayısı */
    private const val MAX_PER_CATEGORY = 3

    /**
     * Zorunlu kategori grupları.
     * Her gruptan en az 1 yem seçilmezse LP büyük ihtimalle INFEASIBLE döner.
     */
    private val MANDATORY_GROUPS: List<Set<String>> = listOf(
        setOf(FeedCategories.ROUGHAGE_WET, FeedCategories.ROUGHAGE_DRY),   // Kaba yem
        setOf(FeedCategories.GRAIN),                                         // Tahıl/Enerji
        setOf(FeedCategories.PROTEIN)                                        // Protein kaynağı
    )

    /**
     * Mineral/Premiks olması tercih edilen gruplar (zorunlu değil, ama varsa ekle).
     */
    private val PREFERRED_GROUPS: List<Set<String>> = listOf(
        setOf(FeedCategories.MINERAL),
        setOf(FeedCategories.PREMIKS)
    )

    /**
     * Ana seçim fonksiyonu.
     *
     * @param scored  (Feed, score) çiftleri, azalan skor sırasında
     * @param animal  Hayvan profili (tür bazlı ayarlamalar için)
     * @return        BrillLpOptimizer'a verilecek yem listesi
     */
    fun selectOptimal(
        scored: List<Pair<Feed, Double>>,
        animal: AnimalProfile
    ): List<Feed> {
        val result   = mutableListOf<Feed>()
        val catCount = mutableMapOf<String, Int>()

        // ── Aşama 1: Zorunlu kategoriler ────────────────────────────────
        for (group in MANDATORY_GROUPS) {
            val best = scored
                .filter { (feed, _) -> feed.category in group && feed !in result }
                .maxByOrNull { it.second }
            if (best != null) {
                result.add(best.first)
                catCount[best.first.category] = (catCount[best.first.category] ?: 0) + 1
            }
        }

        // ── Aşama 2: Tercih edilen kategoriler (mineral/premiks) ─────────
        for (group in PREFERRED_GROUPS) {
            if (result.size >= MAX_TOTAL) break
            val any = result.any { it.category in group }
            if (!any) {
                val best = scored
                    .filter { (feed, _) -> feed.category in group && feed !in result }
                    .maxByOrNull { it.second }
                if (best != null) {
                    result.add(best.first)
                    catCount[best.first.category] = (catCount[best.first.category] ?: 0) + 1
                }
            }
        }

        // ── Aşama 3: Kalan slotlar (skor sırası, kategori limiti) ────────
        for ((feed, _) in scored) {
            if (result.size >= MAX_TOTAL) break
            if (feed in result) continue
            val catUsed = catCount[feed.category] ?: 0
            if (catUsed >= MAX_PER_CATEGORY) continue
            result.add(feed)
            catCount[feed.category] = catUsed + 1
        }

        return result
    }
}

// ─────────────────────────────────────────────────────────────────────────────

/**
 * Kısıt kontrol arayüzü.
 * Uygulama: hangi yemler bu hayvan için geçerli?
 */
interface FeedSelectionConstraints {
    fun isAllowed(feed: Feed, animal: AnimalProfile): Boolean
}

/**
 * Çok kriterli puanlama arayüzü.
 * Uygulama: 0.0 – 1.0 (veya daha yüksek) arası skor döner.
 *
 * Örnek implementasyon (FeedSelectionDefaults.kt içinde kullanılabilir):
 *   - Besin yoğunluğu / fiyat oranı
 *   - Hayvan türüne göre NEL, HP, NDF ağırlıklı skor
 */
interface FeedSelectionObjectives {
    fun score(feed: Feed, animal: AnimalProfile): Double
}
