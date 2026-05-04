package com.nutripulse.app.data.solver.impl

import com.nutripulse.app.data.model.AnimalProfile
import com.nutripulse.app.data.model.AnimalSpecies
import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories
import kotlin.math.max
import kotlin.math.min

/**
 * BrillLpModel — Sığır/Manda için LP kısıt matrisi oluşturur.
 *
 * EKLENTİLER (Brill seviyesine yaklaşmak için):
 * 1. RDP minimum kısıtı  — rumen mikrobiyel protein sentezi için
 * 2. RUP minimum kısıtı  — yüksek verimli ineklerde by-pass protein
 * 3. Lizin minimum kısıtı
 * 4. Metiyonin minimum kısıtı
 * 5. Ca:P oranı LP kısıtı (post-check'ten LP'ye taşındı)
 *
 * x[j] = kg KM / gün
 */
object BrillLpModel {

    data class Model(
        val n: Int,
        val feeds: List<Feed>,
        val cost: DoubleArray,
        val A: Array<DoubleArray>,
        val b: DoubleArray,
        val feedMin: DoubleArray,
        val feedMax: DoubleArray,
        val nutrientNames: List<String>
    )

    data class CategoryMinConstraint(
        val category: String,
        val minKgDm: Double,
        val minFeeds: Int = 0
    )

    @Suppress("LongParameterList")
    fun buildForCattleBuffalo(
        animal: AnimalProfile,
        feeds: List<Feed>,
        dmTargetKg: Double,
        dmTolPct: Double,
        nutrientTolPct: Double,
        ndfMinPct: Double,
        ndfMaxPct: Double,
        roughageMinPct: Double,
        cpTargetG: Double,
        nelTargetMj: Double,
        caTargetG: Double,
        pTargetG: Double,
        // ── YENİ PARAMETRELER ────────────────────────────────
        rdpTargetG: Double = 0.0,
        rupTargetG: Double = 0.0,
        lysTargetG: Double = 0.0,
        metTargetG: Double = 0.0,
        // ─────────────────────────────────────────────────────
        fatMaxPct: Double,
        starchMaxPct: Double,
        nfcMaxPct: Double,
        maxFeedSharePct: Double = 35.0,
        includeNelUpperBand: Boolean = true,
        includeCpUpperBand: Boolean = true,
        categoryMinConstraints: List<CategoryMinConstraint> = emptyList()
    ): Model {

        require(animal.species == AnimalSpecies.SIGIR || animal.species == AnimalSpecies.MANDA) {
            "Bu model sadece SIGIR / MANDA için build edilir."
        }

        val validFeeds = feeds.filter { it.dm in 0.1..100.0 }
        val n          = validFeeds.size
        require(n >= 2) { "En az 2 gecerli yem gerekli." }

        val cost = DoubleArray(n) { i -> BrillUnits.dmCostTlPerKgDm(validFeeds[i]) }

        // ── Yardımcı hesaplamalar ─────────────────────────────────────────────────

        fun isRoughage(f: Feed): Boolean =
            f.category == FeedCategories.ROUGHAGE_DRY || f.category == FeedCategories.ROUGHAGE_WET

        fun nfcPct(f: Feed): Double =
            (100.0 - (f.cp + f.ndf + f.fat + f.ash)).coerceIn(0.0, 100.0)

        fun feedMaxDm(f: Feed): Double {
            val byFeed = when {
                f.maxDailyKg > 0 -> f.maxDailyKg * (f.dm / 100.0)
                f.maxDmPct   > 0 -> dmTargetKg * (f.maxDmPct / 100.0)
                else             -> dmTargetKg * 0.35
            }
            val globalCap = dmTargetKg * (maxFeedSharePct / 100.0)
            val microCap  = when (f.category) {
                FeedCategories.MINERAL,
                FeedCategories.PREMIKS,
                FeedCategories.VITAMIN,
                FeedCategories.ADDITIVE -> 0.25
                FeedCategories.FAT      -> 0.50
                else                    -> Double.POSITIVE_INFINITY
            }
            return min(min(byFeed, globalCap), microCap).coerceAtLeast(0.001)
        }

        fun feedMinDm(f: Feed): Double {
            if (f.minDailyKg > 0) return max(0.0, f.minDailyKg * (f.dm / 100.0))
            return 0.0
        }

        val feedMin = DoubleArray(n) { i -> feedMinDm(validFeeds[i]) }
        val feedMax = DoubleArray(n) { i -> min(feedMaxDm(validFeeds[i]), dmTargetKg) }

        // ── Kısıt listesi (Ax <= b formunda) ─────────────────────────────────────

        val rows  = mutableListOf<DoubleArray>()
        val rhs   = mutableListOf<Double>()
        val names = mutableListOf<String>()

        fun addLeq(row: DoubleArray, b: Double, name: String) {
            rows.add(row); rhs.add(b); names.add(name)
        }

        fun addGeq(row: DoubleArray, b: Double, name: String) {
            val r = row.copyOf()
            for (j in r.indices) r[j] = -r[j]
            rows.add(r); rhs.add(-b); names.add(name)
        }

        val dmTol    = (dmTolPct        / 100.0).coerceIn(0.0, 0.20)
        val dmMin    = dmTargetKg * (1.0 - dmTol)
        val dmMax    = dmTargetKg * (1.0 + dmTol)

        // ── (1) DM bandı ─────────────────────────────────────────────────────────
        run {
            val dmLower = dmMin * 0.97
            val dmUpper = dmMax * 1.03
            val row     = DoubleArray(n) { 1.0 }
            addGeq(row, dmLower, "DM >= ${r2(dmLower)} kg")
            addLeq(row, dmUpper, "DM <= ${r2(dmUpper)} kg")
        }

        // ── (2) Yem üst/alt sınırları ────────────────────────────────────────────
        for (i in 0 until n) {
            val row = DoubleArray(n); row[i] = 1.0
            addLeq(row, feedMax[i], "Max ${validFeeds[i].name}")
        }
        for (i in 0 until n) {
            val L = feedMin[i]
            if (L > 0) {
                val row = DoubleArray(n); row[i] = 1.0
                addGeq(row, L, "Min ${validFeeds[i].name}")
            }
        }

        // ── (3) NEL band ─────────────────────────────────────────────────────────
        if (nelTargetMj > 0) {
            val row    = DoubleArray(n) { i -> validFeeds[i].nel }
            val nelMin = nelTargetMj * 0.97
            val nelMax = nelTargetMj * 1.03
            addGeq(row, nelMin, "NEL >= ${r2(nelMin)} MJ")
            if (includeNelUpperBand) addLeq(row, nelMax, "NEL <= ${r2(nelMax)} MJ")
        }

        // ── (3b) ME band ─────────────────────────────────────────────────────────
        if (nelTargetMj > 0) {
            val meTargetMj = nelTargetMj / 0.62
            val row        = DoubleArray(n) { i ->
                if (validFeeds[i].me > 0) validFeeds[i].me else (validFeeds[i].nel / 0.62)
            }
            addGeq(row, meTargetMj * 0.97, "ME >= ${r2(meTargetMj * 0.97)} MJ")
            addLeq(row, meTargetMj * 1.03, "ME <= ${r2(meTargetMj * 1.03)} MJ")
        }

        // ── (4) CP band ──────────────────────────────────────────────────────────
        if (cpTargetG > 0) {
            val row    = DoubleArray(n) { i -> validFeeds[i].cp * 10.0 }
            val cpMin  = cpTargetG * 0.97
            val cpMax  = cpTargetG * 1.03
            addGeq(row, cpMin, "CP >= ${r1(cpMin)} g")
            if (includeCpUpperBand) addLeq(row, cpMax, "CP <= ${r1(cpMax)} g")
        }

        // ── (5) Ca band ──────────────────────────────────────────────────────────
        if (caTargetG > 0) {
            val row = DoubleArray(n) { i -> BrillUnits.pctToGPerKgDm(validFeeds[i].ca) }
            addGeq(row, caTargetG * 0.95, "Ca >= ${r1(caTargetG * 0.95)} g")
            addLeq(row, caTargetG * 1.05, "Ca <= ${r1(caTargetG * 1.05)} g")
        }

        // ── (6) P band ───────────────────────────────────────────────────────────
        if (pTargetG > 0) {
            val row = DoubleArray(n) { i -> BrillUnits.pctToGPerKgDm(validFeeds[i].p) }
            addGeq(row, pTargetG * 0.95, "P >= ${r1(pTargetG * 0.95)} g")
            addLeq(row, pTargetG * 1.05, "P <= ${r1(pTargetG * 1.05)} g")
        }

        // ── (6b) Ca:P oranı LP kısıtı (YENİ — eskiden sadece post-check'teydi) ──
        // NRC: Ca:P = 1.4:1 ile 3.0:1 arası
        // Doğrusal form: Ca - 1.4*P >= 0  ve  Ca - 3.0*P <= 0
        if (caTargetG > 0 && pTargetG > 0) {
            // Ca >= 1.4 * P  →  Ca - 1.4*P >= 0
            val rowMin = DoubleArray(n) { i ->
                BrillUnits.pctToGPerKgDm(validFeeds[i].ca) - 1.4 * BrillUnits.pctToGPerKgDm(validFeeds[i].p)
            }
            addGeq(rowMin, 0.0, "Ca:P >= 1.4:1")

            // Ca <= 3.0 * P  →  Ca - 3.0*P <= 0
            val rowMax = DoubleArray(n) { i ->
                BrillUnits.pctToGPerKgDm(validFeeds[i].ca) - 3.0 * BrillUnits.pctToGPerKgDm(validFeeds[i].p)
            }
            addLeq(rowMax, 0.0, "Ca:P <= 3.0:1")
        }

        // ── (6c) Mg band ─────────────────────────────────────────────────────────
        if (dmTargetKg > 0) {
            val mgTargetPerKgDm = 1.8
            val mgMinG          = dmTargetKg * mgTargetPerKgDm * 0.90
            val mgMaxG          = dmTargetKg * mgTargetPerKgDm * 1.15
            val row             = DoubleArray(n) { i -> BrillUnits.pctToGPerKgDm(validFeeds[i].mg) }
            addGeq(row, mgMinG, "Mg >= ${r1(mgMinG)} g")
            addLeq(row, mgMaxG, "Mg <= ${r1(mgMaxG)} g")
        }

        // ── (7) NDF min/max ──────────────────────────────────────────────────────
        val ndfMinTol = if (ndfMinPct > 0) max(0.0, ndfMinPct - 1.0) else 0.0
        val ndfMaxTol = if (ndfMaxPct > 0) min(100.0, ndfMaxPct + 2.0) else 0.0
        if (ndfMinTol > 0) {
            val row = DoubleArray(n) { i -> (validFeeds[i].ndf - ndfMinTol) }
            addGeq(row, 0.0, "NDF >= %${r2(ndfMinTol)}")
        }
        if (ndfMaxTol > 0) {
            val row = DoubleArray(n) { i -> (validFeeds[i].ndf - ndfMaxTol) }
            addLeq(row, 0.0, "NDF <= %${r2(ndfMaxTol)}")
        }

        // ── (8) Roughage min ─────────────────────────────────────────────────────
        val roughMinTol = if (roughageMinPct > 0) max(0.0, roughageMinPct - 1.5) else 0.0
        if (roughMinTol > 0) {
            val roughMin = (roughMinTol / 100.0)
            val row      = DoubleArray(n) { i ->
                val coeff = if (isRoughage(validFeeds[i])) 1.0 else 0.0
                coeff - roughMin
            }
            addGeq(row, 0.0, "Roughage >= %${r2(roughMinTol)} DM")
        }

        // ── (8b) Kategori minimum DM kısıtlamaları ───────────────────────────────
        for (constraint in categoryMinConstraints) {
            val minKg = constraint.minKgDm.coerceAtLeast(0.1)
            val row   = DoubleArray(n) { i ->
                if (validFeeds[i].category == constraint.category) 1.0 else 0.0
            }
            addGeq(row, minKg, "${FeedCategories.displayName(constraint.category)} min ${r2(minKg)} kg KM")
        }

        // ── (8c) Kategori MAX kısıtlamaları ──────────────────────────────────────
        val categoryMaxPct = 0.50
        for ((category, _) in categoryMinConstraints) {
            val maxKg = dmTargetKg * categoryMaxPct
            val row   = DoubleArray(n) { i ->
                if (validFeeds[i].category == category) 1.0 else 0.0
            }
            addLeq(row, maxKg, "${FeedCategories.displayName(category)} max ${r2(maxKg)} kg KM")
        }

        // ── (9) Fat max ──────────────────────────────────────────────────────────
        val fatMaxTol = if (fatMaxPct > 0) min(100.0, fatMaxPct + 2.0) else 0.0
        if (fatMaxTol > 0) {
            val row = DoubleArray(n) { i -> (validFeeds[i].fat - fatMaxTol) }
            addLeq(row, 0.0, "Fat <= %${r2(fatMaxTol)}")
        }

        // ── (10) Starch max ──────────────────────────────────────────────────────
        val starchMaxTol = if (starchMaxPct > 0) min(100.0, starchMaxPct + 5.0) else 0.0
        if (starchMaxTol > 0) {
            val row = DoubleArray(n) { i -> (validFeeds[i].starch - starchMaxTol) }
            addLeq(row, 0.0, "Starch <= %${r2(starchMaxTol)}")
        }

        // ── (11) NFC max ─────────────────────────────────────────────────────────
        val nfcMaxTol = if (nfcMaxPct > 0) min(100.0, nfcMaxPct + 5.0) else 0.0
        if (nfcMaxTol > 0) {
            val row = DoubleArray(n) { i -> (nfcPct(validFeeds[i]) - nfcMaxTol) }
            addLeq(row, 0.0, "NFC <= %${r2(nfcMaxTol)}")
        }

        // ── (12) RDP minimum (YENİ) ──────────────────────────────────────────────
        // Rumen mikrobiyel protein sentezi için minimum RDP gereksinimi
        // Tolerans: %88 alt sınır (rumen dengesi için biraz esneklik)
        if (rdpTargetG > 0) {
            val rdpMin = rdpTargetG * 0.88
            val row    = DoubleArray(n) { i -> validFeeds[i].rdp * 10.0 }
            addGeq(row, rdpMin, "RDP >= ${r1(rdpMin)} g")
            // Üst sınır: hedefin %130'u (fazla RDP amonyak kaybına neden olur)
            val rdpMax = rdpTargetG * 1.30
            addLeq(row, rdpMax, "RDP <= ${r1(rdpMax)} g")
        }

        // ── (13) RUP minimum (YENİ) ──────────────────────────────────────────────
        // By-pass protein — özellikle yüksek verimli inek için kritik
        // Tolerans: %85 alt sınır
        if (rupTargetG > 0) {
            val rupMin = rupTargetG * 0.85
            val row    = DoubleArray(n) { i -> validFeeds[i].rup * 10.0 }
            addGeq(row, rupMin, "RUP >= ${r1(rupMin)} g")
        }

        // ── (14) Lizin minimum (YENİ) ────────────────────────────────────────────
        // NRC 2001: Lys metabolizable protein içinde %7.2 olmalı
        // LP'de gram hedef olarak: %90 tolerans
        if (lysTargetG > 5.0) {       // 5g altında feedlerin veri kalitesi düşük
            val lysMin = lysTargetG * 0.90
            val row    = DoubleArray(n) { i -> validFeeds[i].lys * 10.0 }
            addGeq(row, lysMin, "Lys >= ${r1(lysMin)} g")
        }

        // ── (15) Metiyonin minimum (YENİ) ────────────────────────────────────────
        // NRC 2001: Met metabolizable protein içinde %2.4 olmalı
        // Tolerans: %88
        if (metTargetG > 3.0) {       // 3g altında feedlerin veri kalitesi düşük
            val metMin = metTargetG * 0.88
            val row    = DoubleArray(n) { i -> validFeeds[i].met * 10.0 }
            addGeq(row, metMin, "Met >= ${r1(metMin)} g")
        }

        val A = rows.toTypedArray()
        val b = rhs.toDoubleArray()

        return Model(
            n             = n,
            feeds         = validFeeds,
            cost          = cost,
            A             = A,
            b             = b,
            feedMin       = feedMin,
            feedMax       = feedMax,
            nutrientNames = names
        )
    }

    private fun r2(x: Double) = kotlin.math.round(x * 100.0) / 100.0
    private fun r1(x: Double) = kotlin.math.round(x * 10.0)  / 10.0
}
