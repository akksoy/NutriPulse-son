package com.nutripulse.app.data.solver.impl

import com.nutripulse.app.data.model.AnimalProfile
import com.nutripulse.app.data.model.AnimalSpecies
import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories
import com.nutripulse.app.data.model.RationItem
import kotlin.math.max

/**
 * NRC standartlarına uygun HARD CONSTRAINT ENGINE.
 *
 * Tüm besin (nutrient), fiziksel, ve rumen sağlığı kısıtlamalarını
 * zorunlu (hard) olarak uygular. Solver tarafından kullanılır.
 */
object ConstraintEngine {

    data class NutrientBounds(
        // Enerji
        val nelMin: Double = 0.0, val nelMax: Double = Double.MAX_VALUE,
        val meMin: Double = 0.0, val meMax: Double = Double.MAX_VALUE,
        // Protein
        val cpMin: Double = 0.0, val cpMax: Double = Double.MAX_VALUE,
        val rdpMin: Double = 0.0, val rdpMax: Double = Double.MAX_VALUE,
        val rupMin: Double = 0.0, val rupMax: Double = Double.MAX_VALUE,
        // Fiber
        val ndfMin: Double = 0.0, val ndfMax: Double = Double.MAX_VALUE,
        val adfMin: Double = 0.0, val adfMax: Double = Double.MAX_VALUE,
        // Minerals
        val caMin: Double = 0.0, val caMax: Double = Double.MAX_VALUE,
        val pMin: Double = 0.0, val pMax: Double = Double.MAX_VALUE,
        val mgMin: Double = 0.0, val mgMax: Double = Double.MAX_VALUE,
        val naMin: Double = 0.0, val naMax: Double = Double.MAX_VALUE,
        val kMin: Double = 0.0, val kMax: Double = Double.MAX_VALUE,
        // Carbohydrates & Rumen Health
        val nfcMin: Double = 0.0, val nfcMax: Double = Double.MAX_VALUE,
        val starchMin: Double = 0.0, val starchMax: Double = Double.MAX_VALUE,
        val sugarMin: Double = 0.0, val sugarMax: Double = Double.MAX_VALUE,
        val fatMin: Double = 0.0, val fatMax: Double = Double.MAX_VALUE,
        // Mineral Ratio
        val caPRatioMin: Double = 0.0, val caPRatioMax: Double = Double.MAX_VALUE,
        // pH Risk Proxy (DCAD, peNDF)
        val dcadMin: Double = Double.NEGATIVE_INFINITY,
        val peNdfMin: Double = 0.0,
        // Physical constraints
        val roughageMinPct: Double = 0.0,
        val roughageMaxPct: Double = 100.0,
        val concentrateMaxPct: Double = 100.0,
        val dmMin: Double = 0.0, val dmMax: Double = Double.MAX_VALUE
    )

    /**
     * Hayvan profili + standardlara gore hard constraint'leri derle.
     */
    fun generateBoundsForAnimal(
        animal: AnimalProfile,
        balanceMode: SpeciesOptimizationProfile.BalanceMode = SpeciesOptimizationProfile.BalanceMode.BALANCED
    ): NutrientBounds {
        val req = animal  // NRC hesapli gereksinimler bekleniyor
        val profile = SpeciesOptimizationProfiles.forAnimal(animal)
        val standards = RationStandards.constraintsFor(animal.species, balanceMode)

        // NRC hedefin üzerine tolerans ekle
        val tol = profile.toleranceFor(balanceMode) / 100.0

        return NutrientBounds(
            // NEL hedefi (düşük tolerans, enerji kritik)
            nelMin = max(req.reqNelMj * 0.94, req.reqNelMj - 2.0),
            nelMax = req.reqNelMj * (1.0 + tol * 0.5),

            // ME hedefi
            meMin = max(req.reqMeMj * 0.93, req.reqMeMj - 3.0),
            meMax = req.reqMeMj * (1.0 + tol * 0.6),

            // CP hedefi (üst sınır katı)
            cpMin = req.reqCpG * 0.95,
            cpMax = req.reqCpG * 1.15,

            // RDP hedefi
            rdpMin = req.reqRdpG * 0.85,
            rdpMax = req.reqRdpG * 1.25,

            // RUP hedefi
            rupMin = req.reqRupG * 0.80,
            rupMax = req.reqRupG * 1.30,

            // NDF (rumen sağlığı kritik)
            ndfMin = standards.ndfMinPct,
            ndfMax = standards.ndfMaxPct,

            // ADF (fiber kalitesi)
            adfMin = 0.0,
            adfMax = (standards.ndfMaxPct - 4.0).coerceAtLeast(20.0),

            // Kalsiyum (1:1 ile Ca:P oranı için)
            caMin = req.reqCaG * 0.92,
            caMax = req.reqCaG * 1.45,

            // Fosfor (Ca:P 1.5:1 ile 2.5:1 arasında olacak)
            pMin = req.reqPG * 0.90,
            pMax = req.reqPG * 1.45,

            // Magnezyum
            mgMin = req.reqMgG * 0.90,
            mgMax = req.reqMgG * 1.50,

            // Sodyum
            naMin = req.reqNaG * 0.80,
            naMax = req.reqNaG * 1.50,

            // Potasyum
            kMin = req.reqKG * 0.80,
            kMax = req.reqKG * 1.50,

            // NFC (non-fiber carbs) - rumen sağlığı
            nfcMin = 0.0,
            nfcMax = standards.nfcMaxPct.takeIf { it > 0 } ?: 40.0,

            // Nişasta
            starchMin = 0.0,
            starchMax = standards.starchMaxPct.takeIf { it > 0 } ?: 30.0,

            // Şeker
            sugarMin = 0.0,
            sugarMax = standards.sugarMaxPct.takeIf { it > 0 } ?: 10.0,

            // Yağ
            fatMin = 0.0,
            fatMax = standards.fatMaxPct.takeIf { it > 0 } ?: 7.0,

            // Ca:P oranı (NRC: 1.4 ile 3.0 arasında, hayvan türüne göre)
            caPRatioMin = profile.caPRatioMin,
            caPRatioMax = profile.caPRatioMax,

            // Rumen pH riski (DCAD proxy; negatif değerler düşük pH)
            dcadMin = standards.dcadProxyMin,

            // Fiziksel etkili NDF (peNDF; rumen mukoza uyarımı)
            peNdfMin = standards.peNdfProxyMinPct,

            // Fiziksel kısıtlamalar
            roughageMinPct = standards.roughageMinPct,
            roughageMaxPct = profile.wetRoughageMaxPct.coerceAtLeast(65.0),
            concentrateMaxPct = (100.0 - standards.roughageMinPct).coerceAtMost(65.0),

            // DM bandı
            dmMin = req.reqDmKg * 0.93,
            dmMax = req.reqDmKg * (1.0 + tol)
        )
    }

    /**
     * Verilen rasyon için tüm kısıtlamaları kontrol et.
     * Violation varsa liste dön; boş liste = başarılı.
     */
    fun checkViolations(
        rationItems: List<RationItem>,
        animal: AnimalProfile,
        bounds: NutrientBounds,
        allFeeds: List<Feed>
    ): List<String> {
        val violations = mutableListOf<String>()

        if (rationItems.isEmpty()) {
            violations.add("Rasyon boş")
            return violations
        }

        val feedMap = allFeeds.associateBy { it.id }
        val feeds = rationItems.mapNotNull { feedMap[it.feedId] }
        if (feeds.size != rationItems.size) {
            violations.add("Bazı yemler veritabanında bulunamadı")
            return violations
        }

        // Toplam besinleri hesapla
        val dmTotal: Double = rationItems.sumOf { it.amountDmKg }
        val nelTotal: Double = (0 until feeds.size).sumOf { i: Int -> rationItems[i].amountDmKg * feeds[i].nel }
        val meTotal: Double = (0 until feeds.size).sumOf { i: Int ->
            rationItems[i].amountDmKg * (if (feeds[i].me > 0) feeds[i].me else feeds[i].nel / 0.62)
        }
        val cpTotal: Double = (0 until feeds.size).sumOf { i: Int -> rationItems[i].amountDmKg * feeds[i].cp * 10.0 }
        val rdpTotal: Double = (0 until feeds.size).sumOf { i: Int -> rationItems[i].amountDmKg * feeds[i].rdp * 10.0 }
        val rupTotal: Double = (0 until feeds.size).sumOf { i: Int -> rationItems[i].amountDmKg * feeds[i].rup * 10.0 }
        val ndfTotal: Double = (0 until feeds.size).sumOf { i: Int -> rationItems[i].amountDmKg * feeds[i].ndf } / dmTotal
        val adfTotal: Double = (0 until feeds.size).sumOf { i: Int -> rationItems[i].amountDmKg * feeds[i].adf } / dmTotal
        val caTotal: Double = (0 until feeds.size).sumOf { i: Int -> rationItems[i].amountDmKg * feeds[i].ca * 10.0 }
        val pTotal: Double = (0 until feeds.size).sumOf { i: Int -> rationItems[i].amountDmKg * feeds[i].p * 10.0 }
        val mgTotal: Double = (0 until feeds.size).sumOf { i: Int -> rationItems[i].amountDmKg * feeds[i].mg * 10.0 }
        val naTotal: Double = (0 until feeds.size).sumOf { i: Int -> rationItems[i].amountDmKg * feeds[i].na * 10.0 }
        val kTotal: Double = (0 until feeds.size).sumOf { i: Int -> rationItems[i].amountDmKg * feeds[i].k * 10.0 }
        val nfcTotal: Double = (0 until feeds.size).sumOf { i: Int ->
            rationItems[i].amountDmKg * ((100.0 - (feeds[i].cp + feeds[i].ndf + feeds[i].fat + feeds[i].ash)) / 100.0) * 100.0
        } / dmTotal
        val starchTotal: Double = (0 until feeds.size).sumOf { i: Int -> rationItems[i].amountDmKg * feeds[i].starch } / dmTotal
        val sugarTotal: Double = (0 until feeds.size).sumOf { i: Int -> rationItems[i].amountDmKg * feeds[i].sugar } / dmTotal
        val fatTotal: Double = (0 until feeds.size).sumOf { i: Int -> rationItems[i].amountDmKg * feeds[i].fat } / dmTotal
        val dcadTotal: Double = (0 until feeds.size).sumOf { i: Int ->
            rationItems[i].amountDmKg * (feeds[i].na * 434.98 + feeds[i].k * 255.74 - feeds[i].s * 624.15)
        } / dmTotal
        val peNdfTotal: Double = (0 until feeds.size).sumOf { i: Int ->
            val e = when (feeds[i].category) {
                FeedCategories.ROUGHAGE_DRY -> 1.00
                FeedCategories.ROUGHAGE_WET -> 0.85
                FeedCategories.BYPRODUCT -> 0.45
                FeedCategories.GRAIN, FeedCategories.PROTEIN -> 0.25
                else -> 0.20
            }
            rationItems[i].amountDmKg * feeds[i].ndf * e
        } / dmTotal

        // ═══ BESIN KONTROLLERI ═══
        if (nelTotal < bounds.nelMin) violations.add("NEL yetersiz: ${fmt(nelTotal)} < ${fmt(bounds.nelMin)} MJ")
        if (nelTotal > bounds.nelMax) violations.add("NEL fazla: ${fmt(nelTotal)} > ${fmt(bounds.nelMax)} MJ")

        if (meTotal < bounds.meMin) violations.add("ME yetersiz: ${fmt(meTotal)} < ${fmt(bounds.meMin)} MJ")
        if (meTotal > bounds.meMax) violations.add("ME fazla: ${fmt(meTotal)} > ${fmt(bounds.meMax)} MJ")

        if (cpTotal < bounds.cpMin) violations.add("CP yetersiz: ${fmt(cpTotal)} < ${fmt(bounds.cpMin)} g")
        if (cpTotal > bounds.cpMax) violations.add("CP fazla: ${fmt(cpTotal)} > ${fmt(bounds.cpMax)} g")

        if (rdpTotal < bounds.rdpMin) violations.add("RDP yetersiz: ${fmt(rdpTotal)} < ${fmt(bounds.rdpMin)} g")
        if (rdpTotal > bounds.rdpMax) violations.add("RDP fazla: ${fmt(rdpTotal)} > ${fmt(bounds.rdpMax)} g")

        if (rupTotal < bounds.rupMin) violations.add("RUP yetersiz: ${fmt(rupTotal)} < ${fmt(bounds.rupMin)} g")

        // ═══ FİBER KONTROLLERI (RUMEN SAĞLIĞI) ═══
        if (ndfTotal < bounds.ndfMin) violations.add("NDF yetersiz: %${fmt(ndfTotal)} < %${fmt(bounds.ndfMin)}")
        if (ndfTotal > bounds.ndfMax) violations.add("NDF fazla: %${fmt(ndfTotal)} > %${fmt(bounds.ndfMax)}")

        if (adfTotal > bounds.adfMax) violations.add("ADF fazla: %${fmt(adfTotal)} > %${fmt(bounds.adfMax)}")

        // ═══ MİNERAL KONTROLLERI ═══
        if (caTotal < bounds.caMin) violations.add("Ca yetersiz: ${fmt(caTotal)} < ${fmt(bounds.caMin)} g")
        if (caTotal > bounds.caMax) violations.add("Ca fazla: ${fmt(caTotal)} > ${fmt(bounds.caMax)} g")

        if (pTotal < bounds.pMin) violations.add("P yetersiz: ${fmt(pTotal)} < ${fmt(bounds.pMin)} g")
        if (pTotal > bounds.pMax) violations.add("P fazla: ${fmt(pTotal)} > ${fmt(bounds.pMax)} g")

        if (mgTotal < bounds.mgMin) violations.add("Mg yetersiz: ${fmt(mgTotal)} < ${fmt(bounds.mgMin)} g")
        if (mgTotal > bounds.mgMax) violations.add("Mg fazla: ${fmt(mgTotal)} > ${fmt(bounds.mgMax)} g")

        if (naTotal < bounds.naMin) violations.add("Na yetersiz: ${fmt(naTotal)} < ${fmt(bounds.naMin)} g")

        if (kTotal < bounds.kMin) violations.add("K yetersiz: ${fmt(kTotal)} < ${fmt(bounds.kMin)} g")

        // ═══ Ca:P ORANI ═══
        if (pTotal > 0) {
            val ratio = caTotal / pTotal
            if (ratio < bounds.caPRatioMin) violations.add("Ca:P oranı düşük: ${fmt(ratio)}:1 < ${fmt(bounds.caPRatioMin)}:1")
            if (ratio > bounds.caPRatioMax) violations.add("Ca:P oranı yüksek: ${fmt(ratio)}:1 > ${fmt(bounds.caPRatioMax)}:1")
        }

        // ═══ KARBOHIDRAT KONTROLLERI (RUMEN SAĞLIĞI) ═══
        if (nfcTotal > bounds.nfcMax) violations.add("NFC fazla: %${fmt(nfcTotal)} > %${fmt(bounds.nfcMax)}")
        if (starchTotal > bounds.starchMax) violations.add("Nişasta fazla: %${fmt(starchTotal)} > %${fmt(bounds.starchMax)}")
        if (sugarTotal > bounds.sugarMax) violations.add("Şeker fazla: %${fmt(sugarTotal)} > %${fmt(bounds.sugarMax)}")
        if (fatTotal > bounds.fatMax) violations.add("Yağ fazla: %${fmt(fatTotal)} > %${fmt(bounds.fatMax)}")

        // ═══ RUMEN pH RİSKİ ═══
        if (dcadTotal < bounds.dcadMin) {
            violations.add("DCAD düşük (pH riski): ${fmt(dcadTotal)} < ${fmt(bounds.dcadMin)}")
        }
        if (peNdfTotal < bounds.peNdfMin && bounds.peNdfMin > 0) {
            violations.add("peNDF yetersiz (rumen uyarımı): %${fmt(peNdfTotal)} < %${fmt(bounds.peNdfMin)}")
        }

        // ═══ FİZİKSEL KONTROLLER ═══
        if (dmTotal < bounds.dmMin) violations.add("DM yetersiz: ${fmt(dmTotal)} < ${fmt(bounds.dmMin)} kg")
        if (dmTotal > bounds.dmMax) violations.add("DM fazla: ${fmt(dmTotal)} > ${fmt(bounds.dmMax)} kg")

        val roughCats = setOf(FeedCategories.ROUGHAGE_WET, FeedCategories.ROUGHAGE_DRY)
        val roughTotal: Double = (0 until feeds.size)
            .filter { feeds[it].category in roughCats }
            .sumOf { i: Int -> rationItems[i].amountDmKg }
        val roughPct: Double = if (dmTotal > 0) roughTotal / dmTotal * 100.0 else 0.0
        if (roughPct < bounds.roughageMinPct) {
            violations.add("Kaba yem %'si düşük: %${fmt(roughPct)} < %${fmt(bounds.roughageMinPct)}")
        }
        if (roughPct > bounds.roughageMaxPct) {
            violations.add("Kaba yem %'si yüksek: %${fmt(roughPct)} > %${fmt(bounds.roughageMaxPct)}")
        }

        return violations
    }

    private fun fmt(v: Double): String =
        String.format("%.2f", v)
}
