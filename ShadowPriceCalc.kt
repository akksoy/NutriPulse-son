package com.nutripulse.app.data.solver

/**
 * NutriPulse — Gölge Fiyat Hesaplama Yardımcısı
 * Simplex dual değerlerini yorumlar ve öneri üretir.
 */
object ShadowPriceCalc {

    data class ShadowPriceAnalysis(
        val nutrientCode: String,
        val nutrientName: String,
        val shadowPrice: Double,       // TL/birim
        val unit: String,
        val isBinding: Boolean,        // kısıt bağlayıcı mı?
        val costImpact: String,        // maliyet etkisi açıklaması
        val recommendation: String,    // kullanıcıya öneri
        val priority: Int              // öncelik sırası (1=en kritik)
    )

    /**
     * Gölge fiyatları analiz eder ve öneri listesi üretir.
     * @param shadowPrices Simplex dual değerleri (besin sırasına göre)
     * @param nutrientValues Rasyondaki gerçek besin değerleri
     * @param lowerBounds Besin alt sınırları
     * @param totalCost Toplam maliyet
     */
    fun analyze(
        shadowPrices: DoubleArray,
        nutrientValues: DoubleArray,
        lowerBounds: DoubleArray,
        totalCost: Double
    ): List<ShadowPriceAnalysis> {

        val nutrients = listOf(
            Triple("NEL",  "净Enerji Laktasyon", "MJ/kg KM"),
            Triple("ME",   "Metabolik Enerji",    "MJ/kg KM"),
            Triple("CP",   "Ham Protein",         "g/kg KM"),
            Triple("RDP",  "Rumen Yıkılan Protein","g/kg KM"),
            Triple("RUP",  "Bypass Protein",       "g/kg KM"),
            Triple("NDF",  "NDF (Lif)",            "%KM"),
            Triple("CA",   "Kalsiyum",             "g/kg KM"),
            Triple("P",    "Fosfor",               "g/kg KM"),
            Triple("MG",   "Magnezyum",            "g/kg KM"),
            Triple("NA",   "Sodyum",               "g/kg KM"),
            Triple("K",    "Potasyum",             "g/kg KM"),
            Triple("LYS",  "Lizin",                "g/kg KM"),
            Triple("MET",  "Metiyonin",            "g/kg KM")
        )

        val results = mutableListOf<ShadowPriceAnalysis>()

        shadowPrices.forEachIndexed { idx, sp ->
            if (idx >= nutrients.size) return@forEachIndexed
            val (code, name, unit) = nutrients[idx]
            val lb = lowerBounds.getOrElse(idx) { 0.0 }
            val actual = nutrientValues.getOrElse(idx) { 0.0 }
            val isBinding = Math.abs(actual - lb) < 0.001 && lb > 0

            val costImpact = when {
                sp == 0.0  -> "Bu kısıt mevcut rasyonu etkilemiyor"
                sp < -0.01 -> {
                    val saved = Math.abs(sp)
                    val pct = if (totalCost > 0) saved / totalCost * 100 else 0.0
                    "1 birim artışı maliyeti ₺${String.format("%.3f", saved)} artırır (maliyet %${String.format("%.1f", pct)})"
                }
                else -> "Bu kısıt bağlayıcı değil, gevşetilebilir"
            }

            val recommendation = when {
                !isBinding     -> "✅ Bu kısıt karşılanıyor, değiştirmeye gerek yok"
                sp < -0.50     -> "⚠ Kritik! $name kısıtı rasyonu en çok pahalılaştırıyor. Ucuz $name kaynağı ekleyin."
                sp < -0.10     -> "💡 $name kısıtını biraz gevşetmek maliyeti düşürebilir"
                else           -> "ℹ $name kısıtı hafif bağlayıcı, mevcut yemler yeterli"
            }

            val priority = when {
                sp < -0.50 -> 1
                sp < -0.10 -> 2
                isBinding  -> 3
                else       -> 4
            }

            results.add(
                ShadowPriceAnalysis(
                    code, name, sp, unit, isBinding,
                    costImpact, recommendation, priority
                )
            )
        }

        return results.filter { it.isBinding || it.shadowPrice != 0.0 }
            .sortedBy { it.priority }
    }

    /**
     * En kritik kısıtı (en yüksek gölge fiyat) bulur.
     */
    fun findMostBindingConstraint(analyses: List<ShadowPriceAnalysis>): ShadowPriceAnalysis? {
        return analyses.minByOrNull { it.shadowPrice }
    }

    /**
     * Toplam potansiyel tasarrufu hesaplar.
     * Eğer tüm bağlayıcı kısıtlar %10 gevşetilseydi ne olurdu?
     */
    fun estimatePotentialSaving(analyses: List<ShadowPriceAnalysis>): Double {
        return analyses
            .filter { it.shadowPrice < 0 }
            .sumOf { Math.abs(it.shadowPrice) * 0.1 }  // %10 gevşetme
    }
}
