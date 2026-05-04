package com.nutripulse.app.data

import com.nutripulse.app.data.model.AnimalCategory
import com.nutripulse.app.data.model.AnimalProfile
import com.nutripulse.app.data.model.AnimalSpecies
import kotlin.math.*

/**
 * NRC standartlarına gore hayvan besin ihtiyaclarini hesaplar.
 * - Sut inegi: NRC 2001 (Nutrient Requirements of Dairy Cattle)
 * - Besi sigiri: NRC/NASEM 2016 (Beef Cattle)
 * - Koyun/Keci: NRC 2007 (Nutrient Requirements of Small Ruminants)
 * - Kanatli: NRC 1994
 * - At ve tek tirnaklilar: NRC 2007 (Nutrient Requirements of Horses)
 * - Su Urunleri: NRC 2011
 * - Diger (Tavsan): NRC 1997
 */
object NrcCalculator {

    data class NrcResult(
        val dmKg: Double,
        val meMj: Double,
        val nelMj: Double,
        val nemMj: Double,
        val negMj: Double,
        val cpG: Double,
        val rdpG: Double,
        val rupG: Double,
        val ndfPct: Double,
        val caG: Double,
        val pG: Double,
        val mgG: Double,
        val naG: Double,
        val kG: Double,
        val lysG: Double,
        val metG: Double,
        val thrG: Double = 0.0,
        val trpG: Double = 0.0,
        val sG: Double = 0.0,
        val notes: String = ""
    )

    private val dairyCattleCategories = setOf(
        AnimalCategory.SUT_INEGI_ERKEN,
        AnimalCategory.SUT_INEGI_ORTA,
        AnimalCategory.SUT_INEGI_GEC,
        AnimalCategory.KURU_INEK_UZAK,
        AnimalCategory.KURU_INEK_YAKIN,
        AnimalCategory.DUVE_0_6,
        AnimalCategory.DUVE_6_12,
        AnimalCategory.DUVE_12_24,
        AnimalCategory.BUZAGI,
        AnimalCategory.DANA,
        AnimalCategory.MANDA_SUT
    )

    private val beefCattleCategories = setOf(
        AnimalCategory.BESI_BASLANGIC,
        AnimalCategory.BESI_BUYUTME,
        AnimalCategory.BESI_BITIRME,
        AnimalCategory.MANDA_BESI
    )

    fun calculate(p: AnimalProfile): NrcResult {
        return when (p.species) {
            AnimalSpecies.SIGIR, AnimalSpecies.MANDA -> {
                when {
                    p.category in beefCattleCategories -> calcSigirBesi2016(p)
                    p.category in dairyCattleCategories -> calcSigirSut2001(p)
                    else -> calcSigirSut2001(p)
                }
            }
            AnimalSpecies.KOYUN -> calcKoyun(p)
            AnimalSpecies.KECI  -> calcKeci(p)
            // AnimalSpecies.KANATLI -> calcKanatli(p)
            // AnimalSpecies.AT    -> calcAt(p)
            // AnimalSpecies.SU    -> calcSuUrunleri(p)
            // AnimalSpecies.TAVSAN -> calcTavsan1997(p)
            else -> calcSigirSut2001(p)
        }
    }

    // ══════════════════════════════════════════════════════════
    // NRC 2001 — SÜT İNEĞİ
    // ══════════════════════════════════════════════════════════
    private fun calcSigirSut2001(p: AnimalProfile): NrcResult {
        val ca = p.bodyWeight
        val milk = p.milkYield
        val fat = p.milkFat
        val pro = p.milkProtein
        val week = p.lactationWeek
        val pregMonth = p.pregnancyMonth
        val lact = p.lactationNumber

        // Kullanici hafta girmediyse kategoriye gore makul laktasyon haftasi kullan.
        val effectiveWeek = when {
            week > 0 -> week.toDouble()
            p.category == AnimalCategory.SUT_INEGI_ERKEN -> 6.0
            p.category == AnimalCategory.SUT_INEGI_ORTA || p.category == AnimalCategory.MANDA_SUT -> 18.0
            p.category == AnimalCategory.SUT_INEGI_GEC -> 32.0
            else -> 10.0
        }

        // ── KM TÜKETİMİ (NRC 2001 Tablo 1-2) ──
        val dmKg: Double = when (p.category) {
            AnimalCategory.SUT_INEGI_ERKEN,
            AnimalCategory.SUT_INEGI_ORTA,
            AnimalCategory.SUT_INEGI_GEC,
            AnimalCategory.MANDA_SUT -> {
                // 4% FCM = 0.4 * süt(kg) + 15 * süt_yağı(kg)
                val fcm = milk * (0.4 + 0.15 * fat)
                val base = (0.372 * fcm + 0.0968 * ca.pow(0.75))
                val adj = 1 - exp(-0.192 * (effectiveWeek + 3.67))
                (base * adj).coerceIn(10.0, 30.0)
            }
            AnimalCategory.KURU_INEK_UZAK -> (ca * 0.018).coerceIn(8.0, 14.0)
            AnimalCategory.KURU_INEK_YAKIN -> (ca * 0.012).coerceIn(7.0, 12.0)
            AnimalCategory.DUVE_0_6        -> (ca * 0.030).coerceIn(1.5, 4.0)
            AnimalCategory.DUVE_6_12       -> (ca * 0.026).coerceIn(3.0, 6.0)
            AnimalCategory.DUVE_12_24      -> (ca * 0.022).coerceIn(5.0, 10.0)
            AnimalCategory.BUZAGI          -> (ca * 0.028).coerceIn(0.5, 3.0)
            AnimalCategory.DANA            -> (ca * 0.025).coerceIn(2.0, 5.0)
            else -> (ca * 0.020).coerceIn(8.0, 25.0)
        }

        // ── NEL İHTİYACI (NRC 2001) ──
        val nelMaintMcal = 0.080 * ca.pow(0.75)          // bakım NEL MJ/gün
        val nelMilkMcal = milk * (0.0929 * fat + 0.0547 * pro + 0.0395)  // süt NEL
        val nelPregMcal = if (pregMonth >= 5) {          // gebelik NEL
            (0.00318 * pregMonth - 0.0352).coerceAtLeast(0.0)
        } else 0.0
        val nelGrowthMcal = if (lact == 1) 0.8 * ca * 0.0001 else 0.0
        val nelTotalMcal = nelMaintMcal + nelMilkMcal + nelPregMcal + nelGrowthMcal

        // Enerji esitlikleri Mcal tabanli, cikis MJ/gun olacak sekilde donusturulur.
        val meTotalMcal = nelTotalMcal / 0.62  // NEL/ME verimi ortalama 0.62

        // ── HAM PROTEİN (NRC 2001 MP Sistemi) ──
        val mpMaint = 3.8 * ca.pow(0.75)              // g/gün bakım MP
        val mpMilk  = milk * 1000 * pro / 100 * 0.67  // g/gün süt MP
        val mpPreg  = if (pregMonth >= 5) 4.5 * pregMonth else 0.0
        val mpGrowth = if (lact == 1) 0.15 * ca else 0.0
        val mpTotal = mpMaint + mpMilk + mpPreg + mpGrowth

        // MP(g) -> CP(g) cevirimi: CP = MP / MP_verimi
        val cpTotal = mpTotal / 0.67
        val rdp = cpTotal * 0.62
        val rup = cpTotal * 0.38

        // ── NDF MİN ──
        val ndfPct = when (p.category) {
            AnimalCategory.SUT_INEGI_ERKEN -> 28.0
            AnimalCategory.SUT_INEGI_ORTA,
            AnimalCategory.MANDA_SUT       -> 30.0
            AnimalCategory.SUT_INEGI_GEC   -> 32.0
            AnimalCategory.KURU_INEK_UZAK,
            AnimalCategory.KURU_INEK_YAKIN  -> 33.0
            AnimalCategory.BESI_BITIRME     -> 15.0
            else -> 25.0
        }

        // ── MİNERALLER (NRC 2001) ──
        val caG = when {
            milk > 0 -> 7.9 + milk * 1.22 + (if (pregMonth >= 7) pregMonth * 1.5 else 0.0)
            else -> ca * 0.016
        }
        val pG = when {
            milk > 0 -> 4.5 + milk * 0.90
            else -> ca * 0.012
        }
        val mgG = ca * 0.045 + milk * 0.15
        val naG = ca * 0.023 + milk * 0.63
        val kG  = ca * 0.20 + milk * 1.50

        // ── AMİNO ASİTLER (NRC 2001 Lizin:Met oranı 3:1) ──
        val lysG = mpTotal * 0.073
        val metG = lysG / 3.0
        val (thrG, trpG, sG) = estimateAaAndSulfurFromCp(cpTotal)

        // ── NEM / NEG (Besi için) ──
        val gcaa = p.targetDailyGain
        val nemMj: Double
        val negMj: Double
        if (p.category in listOf(
                AnimalCategory.BESI_BASLANGIC,
                AnimalCategory.BESI_BUYUTME,
                AnimalCategory.BESI_BITIRME,
                AnimalCategory.MANDA_BESI
            )) {
            nemMj = 0.077 * ca.pow(0.75)
            negMj = if (gcaa > 0) (0.0557 * ca.pow(0.75) * (gcaa / 1000.0).pow(1.097)) else 0.0
        } else {
            nemMj = 0.0
            negMj = 0.0
        }

        return NrcResult(
            dmKg = round2(dmKg),
            meMj = round2(mcalToMj(meTotalMcal)),
            nelMj = round2(mcalToMj(nelTotalMcal)),
            nemMj = 0.0,
            negMj = 0.0,
            cpG = round1(cpTotal),
            rdpG = round1(rdp),
            rupG = round1(rup),
            ndfPct = ndfPct,
            caG = round1(caG),
            pG = round1(pG),
            mgG = round1(mgG),
            naG = round1(naG),
            kG = round1(kG),
            lysG = round1(lysG),
            metG = round1(metG),
            thrG = round1(thrG),
            trpG = round1(trpG),
            sG = round1(sG),
            notes = "NRC 2001 Dairy Cattle (ME: MJ/gun)"
        )
    }

    // NRC/NASEM 2016 — BESI SIGIRI
    private fun calcSigirBesi2016(p: AnimalProfile): NrcResult {
        val bw = p.bodyWeight.coerceAtLeast(80.0)
        val adgKg = (p.targetDailyGain.coerceAtLeast(0.0) / 1000.0)

        val dmKg = when (p.category) {
            AnimalCategory.BESI_BASLANGIC -> (bw * 0.024 + adgKg * 0.8).coerceIn(4.0, 12.0)
            AnimalCategory.BESI_BUYUTME,
            AnimalCategory.MANDA_BESI -> (bw * 0.022 + adgKg * 0.7).coerceIn(6.0, 14.0)
            AnimalCategory.BESI_BITIRME -> (bw * 0.019 + adgKg * 0.5).coerceIn(7.0, 16.0)
            else -> (bw * 0.021).coerceIn(5.0, 15.0)
        }

        val nemMcal = 0.077 * bw.pow(0.75)
        val negMcal = if (adgKg > 0) 0.0557 * bw.pow(0.75) * adgKg.pow(1.097) else 0.0

        // ME yaklasik cevirim: NEm ~ 0.64, NEg ~ 0.45 ME verimi
        val meMcal = (nemMcal / 0.64) + (negMcal / 0.45)
        val nelMcal = 0.0

        val cpG = when (p.category) {
            AnimalCategory.BESI_BASLANGIC -> bw * 2.9 + adgKg * 1000.0 * 0.40
            AnimalCategory.BESI_BUYUTME,
            AnimalCategory.MANDA_BESI -> bw * 2.6 + adgKg * 1000.0 * 0.34
            AnimalCategory.BESI_BITIRME -> bw * 2.3 + adgKg * 1000.0 * 0.30
            else -> bw * 2.5 + adgKg * 1000.0 * 0.33
        }

        val rdp = cpG * 0.58
        val rup = cpG * 0.42

        val ndfPct = when (p.category) {
            AnimalCategory.BESI_BASLANGIC -> 25.0
            AnimalCategory.BESI_BUYUTME,
            AnimalCategory.MANDA_BESI -> 22.0
            AnimalCategory.BESI_BITIRME -> 18.0
            else -> 22.0
        }

        // Mineral gereksinimleri DM uzerinden yaklasiklanir
        val caG = dmKg * 1000.0 * 0.006
        val pG = dmKg * 1000.0 * 0.0035
        val mgG = dmKg * 1000.0 * 0.0018
        val naG = dmKg * 1000.0 * 0.0012
        val kG = dmKg * 1000.0 * 0.0065

        val (thrG, trpG, sG) = estimateAaAndSulfurFromCp(cpG)

        return NrcResult(
            dmKg = round2(dmKg),
            meMj = round2(meMcal * 4.184),
            nelMj = round2(nelMcal * 4.184),
            nemMj = round2(nemMcal * 4.184),
            negMj = round2(negMcal * 4.184),
            cpG = round1(cpG),
            rdpG = round1(rdp),
            rupG = round1(rup),
            ndfPct = ndfPct,
            caG = round1(caG),
            pG = round1(pG),
            mgG = round1(mgG),
            naG = round1(naG),
            kG = round1(kG),
            lysG = round1(cpG * 0.060),
            metG = round1(cpG * 0.020),
            thrG = round1(thrG),
            trpG = round1(trpG),
            sG = round1(sG),
            notes = "NRC/NASEM 2016 Beef Cattle (ME: MJ/gun)"
        )
    }

    // ══════════════════════════════════════════════════════════
    // NRC 2007 — KOYUN
    // ══════════════════════════════════════════════════════════
    private fun calcKoyun(p: AnimalProfile): NrcResult {
        val ca = p.bodyWeight
        val milk = p.milkYield
        val pregMonth = p.pregnancyMonth

        val dmKg = when (p.category) {
            AnimalCategory.KOYUN_SUT      -> (ca * 0.040 + milk * 0.30).coerceIn(1.2, 3.5)
            AnimalCategory.KOYUN_EMZIREN  -> (ca * 0.044).coerceIn(1.5, 3.8)
            AnimalCategory.KOYUN_GEBE     -> (ca * 0.025 + if (pregMonth >= 4) 0.3 else 0.0).coerceIn(0.8, 2.5)
            AnimalCategory.KOYUN_BESI     -> (ca * 0.032).coerceIn(0.8, 2.0)
            AnimalCategory.KUZU_BESI,
            AnimalCategory.KUZU_BESI_BASLANGIC,
            AnimalCategory.KUZU_BESI_BITIS -> (ca * 0.045).coerceIn(0.3, 1.5)
            else -> ca * 0.030
        }

        val meMj = when (p.category) {
            AnimalCategory.KOYUN_SUT      -> 0.418 * ca.pow(0.75) + milk * 4.60
            AnimalCategory.KOYUN_EMZIREN  -> 0.418 * ca.pow(0.75) + milk * 5.00
            AnimalCategory.KOYUN_GEBE     -> 0.418 * ca.pow(0.75) * (1 + if (pregMonth >= 4) 0.25 else 0.0)
            AnimalCategory.KOYUN_BESI     -> 0.418 * ca.pow(0.75) + p.targetDailyGain / 1000.0 * 25.0
            AnimalCategory.KUZU_BESI,
            AnimalCategory.KUZU_BESI_BASLANGIC,
            AnimalCategory.KUZU_BESI_BITIS -> 0.418 * ca.pow(0.75) + p.targetDailyGain / 1000.0 * 22.0
            else -> 0.418 * ca.pow(0.75)
        }

        val cpG = when (p.category) {
            AnimalCategory.KOYUN_SUT      -> ca * 3.5 + milk * 50
            AnimalCategory.KOYUN_EMZIREN  -> ca * 4.0 + milk * 55
            AnimalCategory.KOYUN_GEBE     -> ca * 2.8 + if (pregMonth >= 4) ca * 0.8 else 0.0
            AnimalCategory.KOYUN_BESI     -> ca * 3.0 + p.targetDailyGain * 0.18
            AnimalCategory.KUZU_BESI,
            AnimalCategory.KUZU_BESI_BASLANGIC,
            AnimalCategory.KUZU_BESI_BITIS -> ca * 5.0 + p.targetDailyGain * 0.20
            else -> ca * 3.0
        }

        val caG = when (p.category) {
            AnimalCategory.KOYUN_SUT, AnimalCategory.KOYUN_EMZIREN -> ca * 0.12 + milk * 1.25
            AnimalCategory.KOYUN_GEBE -> ca * 0.10 + (if (pregMonth >= 4) 2.0 else 0.0)
            else -> ca * 0.08
        }
        val pG  = caG * 0.65
        val mgG = ca * 0.018
        val naG = ca * 0.015
        val kG  = ca * 0.18

        val (thrG, trpG, sG) = estimateAaAndSulfurFromCp(cpG)

        return NrcResult(
            dmKg = round2(dmKg), meMj = round2(meMj),
            nelMj = round2(meMj * 0.58), nemMj = 0.0, negMj = 0.0,
            cpG = round1(cpG), rdpG = round1(cpG * 0.65), rupG = round1(cpG * 0.35),
            ndfPct = 30.0,
            caG = round1(caG), pG = round1(pG), mgG = round1(mgG),
            naG = round1(naG), kG = round1(kG),
            lysG = round1(cpG * 0.062), metG = round1(cpG * 0.021),
            thrG = round1(thrG), trpG = round1(trpG), sG = round1(sG),
            notes = "NRC 2007 Small Ruminants - Koyun (ME: MJ/gun)"
        )
    }

    // ══════════════════════════════════════════════════════════
    // NRC 2007 — KEÇİ
    // ══════════════════════════════════════════════════════════
    private fun calcKeci(p: AnimalProfile): NrcResult {
        val ca = p.bodyWeight
        val milk = p.milkYield

        val dmKg = when (p.category) {
            AnimalCategory.KECI_SUT    -> (0.036 * ca + 0.25 * milk).coerceIn(1.0, 3.5)
            AnimalCategory.KECI_BESI   -> (ca * 0.030).coerceIn(0.8, 2.0)
            AnimalCategory.KECI_ANKARA -> (ca * 0.032).coerceIn(1.0, 2.5)
            AnimalCategory.OGLAK_BESI  -> (ca * 0.045).coerceIn(0.3, 1.2)
            else -> ca * 0.030
        }

        val meMj = when (p.category) {
            AnimalCategory.KECI_SUT    -> 0.38 * ca.pow(0.75) + milk * 4.80
            AnimalCategory.KECI_BESI   -> 0.38 * ca.pow(0.75) + p.targetDailyGain / 1000.0 * 24.0
            AnimalCategory.KECI_ANKARA -> 0.38 * ca.pow(0.75) + 0.5  // tiftik artı enerji
            AnimalCategory.OGLAK_BESI  -> 0.38 * ca.pow(0.75) + p.targetDailyGain / 1000.0 * 20.0
            else -> 0.38 * ca.pow(0.75)
        }

        val cpG = when (p.category) {
            AnimalCategory.KECI_SUT    -> ca * 3.2 + milk * 52
            AnimalCategory.KECI_BESI   -> ca * 2.8 + p.targetDailyGain * 0.17
            AnimalCategory.KECI_ANKARA -> ca * 3.5
            AnimalCategory.OGLAK_BESI  -> ca * 4.5 + p.targetDailyGain * 0.20
            else -> ca * 3.0
        }

        val caG = if (milk > 0) ca * 0.10 + milk * 1.20 else ca * 0.08
        val pG  = caG * 0.62
        val mgG = ca * 0.016
        val naG = ca * 0.014
        val kG  = ca * 0.18

        val (thrG, trpG, sG) = estimateAaAndSulfurFromCp(cpG)

        return NrcResult(
            dmKg = round2(dmKg), meMj = round2(meMj),
            nelMj = round2(meMj * 0.58), nemMj = 0.0, negMj = 0.0,
            cpG = round1(cpG), rdpG = round1(cpG * 0.62), rupG = round1(cpG * 0.38),
            ndfPct = 28.0,
            caG = round1(caG), pG = round1(pG), mgG = round1(mgG),
            naG = round1(naG), kG = round1(kG),
            lysG = round1(cpG * 0.060), metG = round1(cpG * 0.020),
            thrG = round1(thrG), trpG = round1(trpG), sG = round1(sG),
            notes = "NRC 2007 Small Ruminants - Keci (ME: MJ/gun)"
        )
    }

    // ══════════════════════════════════════════════════════════
    // NRC 1994 — KANATLI
    // ══════════════════════════════════════════════════════════
    private fun calcKanatli(p: AnimalProfile): NrcResult {
        val bwKg = p.bodyWeight.coerceAtLeast(0.0)
        val gcaa = p.targetDailyGain.coerceAtLeast(0.0)

        data class PoultryReq(
            val meDensityMcalKg: Double,
            val cpPct: Double,
            val lysPct: Double,
            val metPct: Double,
            val thrPct: Double,
            val trpPct: Double,
            val caPct: Double,
            val pPct: Double,
            val naPct: Double,
            val kPct: Double,
            val mgPct: Double,
            val sPct: Double,
            val maintCoef: Double,
            val gainCoef: Double
        )

        val req = when (p.category) {
            AnimalCategory.BROILER_BASLANGIC -> PoultryReq(
                meDensityMcalKg = 3.05, cpPct = 22.0, lysPct = 1.20, metPct = 0.50,
                thrPct = 0.80, trpPct = 0.23, caPct = 0.95, pPct = 0.48,
                naPct = 0.18, kPct = 0.90, mgPct = 0.06, sPct = 0.20,
                maintCoef = 0.22, gainCoef = 0.0020
            )
            AnimalCategory.BROILER_BUYUTME -> PoultryReq(
                meDensityMcalKg = 3.10, cpPct = 20.0, lysPct = 1.05, metPct = 0.44,
                thrPct = 0.74, trpPct = 0.20, caPct = 0.90, pPct = 0.45,
                naPct = 0.17, kPct = 0.85, mgPct = 0.06, sPct = 0.19,
                maintCoef = 0.21, gainCoef = 0.0019
            )
            AnimalCategory.BROILER_BITIRME -> PoultryReq(
                meDensityMcalKg = 3.15, cpPct = 18.0, lysPct = 0.92, metPct = 0.39,
                thrPct = 0.68, trpPct = 0.18, caPct = 0.85, pPct = 0.42,
                naPct = 0.17, kPct = 0.80, mgPct = 0.06, sPct = 0.18,
                maintCoef = 0.20, gainCoef = 0.0018
            )
            AnimalCategory.YUMURTACI_PILIC -> PoultryReq(
                meDensityMcalKg = 2.95, cpPct = 18.0, lysPct = 0.95, metPct = 0.40,
                thrPct = 0.68, trpPct = 0.19, caPct = 1.00, pPct = 0.50,
                naPct = 0.17, kPct = 0.80, mgPct = 0.06, sPct = 0.18,
                maintCoef = 0.18, gainCoef = 0.0010
            )
            AnimalCategory.YUMURTACI_YUM -> PoultryReq(
                meDensityMcalKg = 2.75, cpPct = 16.5, lysPct = 0.82, metPct = 0.36,
                thrPct = 0.62, trpPct = 0.17, caPct = 3.80, pPct = 0.45,
                naPct = 0.17, kPct = 0.75, mgPct = 0.06, sPct = 0.17,
                maintCoef = 0.18, gainCoef = 0.0013
            )
            AnimalCategory.YUMURTACI_YASLI -> PoultryReq(
                meDensityMcalKg = 2.70, cpPct = 15.5, lysPct = 0.75, metPct = 0.33,
                thrPct = 0.58, trpPct = 0.16, caPct = 4.00, pPct = 0.42,
                naPct = 0.17, kPct = 0.70, mgPct = 0.06, sPct = 0.16,
                maintCoef = 0.18, gainCoef = 0.0008
            )
            AnimalCategory.HINDI_BUYUTME,
            AnimalCategory.HINDI_BESI -> PoultryReq(
                meDensityMcalKg = 3.00, cpPct = 21.0, lysPct = 1.12, metPct = 0.46,
                thrPct = 0.76, trpPct = 0.21, caPct = 1.00, pPct = 0.50,
                naPct = 0.18, kPct = 0.90, mgPct = 0.06, sPct = 0.20,
                maintCoef = 0.20, gainCoef = 0.0019
            )
            AnimalCategory.BILDIRCIN_YUM -> PoultryReq(
                meDensityMcalKg = 2.80, cpPct = 19.0, lysPct = 1.05, metPct = 0.44,
                thrPct = 0.70, trpPct = 0.19, caPct = 3.20, pPct = 0.42,
                naPct = 0.17, kPct = 0.75, mgPct = 0.06, sPct = 0.18,
                maintCoef = 0.18, gainCoef = 0.0010
            )
            AnimalCategory.BILDIRCIN_BESI,
            AnimalCategory.ORDEK_BESI,
            AnimalCategory.KAZ_BESI -> PoultryReq(
                meDensityMcalKg = 2.95, cpPct = 19.0, lysPct = 1.00, metPct = 0.41,
                thrPct = 0.72, trpPct = 0.19, caPct = 0.95, pPct = 0.45,
                naPct = 0.18, kPct = 0.85, mgPct = 0.06, sPct = 0.19,
                maintCoef = 0.19, gainCoef = 0.0017
            )
            else -> PoultryReq(
                meDensityMcalKg = 2.95, cpPct = 19.0, lysPct = 1.00, metPct = 0.41,
                thrPct = 0.70, trpPct = 0.19, caPct = 0.95, pPct = 0.45,
                naPct = 0.18, kPct = 0.85, mgPct = 0.06, sPct = 0.18,
                maintCoef = 0.19, gainCoef = 0.0015
            )
        }

        val meMcal = req.maintCoef * bwKg.pow(0.75) + req.gainCoef * gcaa
        val meKcal = meMcal * 1000.0
        val meMj = meMcal * 4.184

        val dmKg = (meMcal / req.meDensityMcalKg).coerceAtLeast(0.0)
        val dmG = dmKg * 1000.0

        val cpG = dmG * (req.cpPct / 100.0)
        val lysG = dmG * (req.lysPct / 100.0)
        val metG = dmG * (req.metPct / 100.0)
        val thrG = dmG * (req.thrPct / 100.0)
        val trpG = dmG * (req.trpPct / 100.0)

        val caG = dmG * (req.caPct / 100.0)
        val pG = dmG * (req.pPct / 100.0)
        val naG = dmG * (req.naPct / 100.0)
        val kG = dmG * (req.kPct / 100.0)
        val mgG = dmG * (req.mgPct / 100.0)
        val sG = dmG * (req.sPct / 100.0)

        // Poultry rations still use degradable/undegradable protein split for balancing reports.
        val rdpG = cpG * 0.60
        val rupG = cpG * 0.40

        val warnings = validatePoultryRanges(
            category = p.category,
            dmKg = dmKg,
            meKcal = meKcal,
            cpG = cpG,
            lysG = lysG,
            metG = metG,
            caG = caG,
            pG = pG,
            bwKg = bwKg
        )
        val noteSuffix = if (warnings.isEmpty()) {
            ""
        } else {
            " | Uyari: " + warnings.joinToString("; ")
        }

        return NrcResult(
            dmKg = round3(dmKg),
            meMj = round1(meKcal), // Poultry ME is intentionally reported as kcal/day.
            nelMj = round2(meMj * 0.72),
            nemMj = round2(meMj * 0.55),
            negMj = round2((meMj * 0.17).coerceAtLeast(0.0)),
            cpG = round1(cpG),
            rdpG = round1(rdpG),
            rupG = round1(rupG),
            ndfPct = 5.0,
            caG = round2(caG),
            pG = round2(pG),
            mgG = round2(mgG),
            naG = round2(naG),
            kG = round2(kG),
            lysG = round2(lysG),
            metG = round2(metG),
            thrG = round2(thrG),
            trpG = round2(trpG),
            sG = round2(sG),
            notes = "NRC 1994 Poultry - aminoasit ve mineraller tamamlandi (ME: kcal/gun)$noteSuffix"
        )
    }

    private data class PoultryValidationProfile(
        val meDensityKcalKg: Range,
        val cpPct: Range,
        val lysPct: Range,
        val metPct: Range,
        val caPct: Range,
        val pPct: Range,
        val dmPerBw: Range
    )

    private data class Range(val min: Double, val max: Double)

    private fun validatePoultryRanges(
        category: String,
        dmKg: Double,
        meKcal: Double,
        cpG: Double,
        lysG: Double,
        metG: Double,
        caG: Double,
        pG: Double,
        bwKg: Double
    ): List<String> {
        if (dmKg <= 0.0 || bwKg <= 0.0) return listOf("DM veya canli agirlik gecersiz")

        val profile = when (category) {
            AnimalCategory.BROILER_BASLANGIC -> PoultryValidationProfile(
                meDensityKcalKg = Range(2950.0, 3150.0),
                cpPct = Range(21.0, 23.0),
                lysPct = Range(1.12, 1.28),
                metPct = Range(0.46, 0.56),
                caPct = Range(0.90, 1.05),
                pPct = Range(0.44, 0.54),
                dmPerBw = Range(0.04, 0.18)
            )
            AnimalCategory.BROILER_BUYUTME -> PoultryValidationProfile(
                meDensityKcalKg = Range(3000.0, 3200.0),
                cpPct = Range(19.0, 21.0),
                lysPct = Range(0.98, 1.12),
                metPct = Range(0.40, 0.49),
                caPct = Range(0.85, 0.98),
                pPct = Range(0.41, 0.50),
                dmPerBw = Range(0.03, 0.14)
            )
            AnimalCategory.BROILER_BITIRME -> PoultryValidationProfile(
                meDensityKcalKg = Range(3050.0, 3250.0),
                cpPct = Range(17.0, 19.0),
                lysPct = Range(0.86, 0.98),
                metPct = Range(0.35, 0.43),
                caPct = Range(0.80, 0.92),
                pPct = Range(0.38, 0.46),
                dmPerBw = Range(0.02, 0.10)
            )
            AnimalCategory.YUMURTACI_PILIC -> PoultryValidationProfile(
                meDensityKcalKg = Range(2850.0, 3050.0),
                cpPct = Range(17.0, 19.0),
                lysPct = Range(0.88, 1.02),
                metPct = Range(0.36, 0.44),
                caPct = Range(0.95, 1.08),
                pPct = Range(0.45, 0.55),
                dmPerBw = Range(0.03, 0.13)
            )
            AnimalCategory.YUMURTACI_YUM,
            AnimalCategory.YUMURTACI_YASLI,
            AnimalCategory.BILDIRCIN_YUM -> PoultryValidationProfile(
                meDensityKcalKg = Range(2650.0, 2850.0),
                cpPct = Range(15.0, 17.0),
                lysPct = Range(0.74, 0.88),
                metPct = Range(0.30, 0.40),
                caPct = Range(3.4, 4.2),
                pPct = Range(0.38, 0.48),
                dmPerBw = Range(0.02, 0.11)
            )
            else -> PoultryValidationProfile(
                meDensityKcalKg = Range(2850.0, 3150.0),
                cpPct = Range(18.0, 21.5),
                lysPct = Range(0.90, 1.15),
                metPct = Range(0.36, 0.50),
                caPct = Range(0.85, 1.10),
                pPct = Range(0.40, 0.55),
                dmPerBw = Range(0.02, 0.16)
            )
        }

        val dmG = dmKg * 1000.0
        val meDensity = meKcal / dmKg
        val cpPctCalc = cpG / dmG * 100.0
        val lysPctCalc = lysG / dmG * 100.0
        val metPctCalc = metG / dmG * 100.0
        val caPctCalc = caG / dmG * 100.0
        val pPctCalc = pG / dmG * 100.0
        val dmPerBwCalc = dmKg / bwKg

        val checks: List<Pair<String, Pair<Double, Range>>> = listOf(
            "ME_yogunluk" to (meDensity to profile.meDensityKcalKg),
            "CP%" to (cpPctCalc to profile.cpPct),
            "Lys%" to (lysPctCalc to profile.lysPct),
            "Met%" to (metPctCalc to profile.metPct),
            "Ca%" to (caPctCalc to profile.caPct),
            "P%" to (pPctCalc to profile.pPct),
            "DM/CA" to (dmPerBwCalc to profile.dmPerBw)
        )

        return checks.mapNotNull { (label, pair) ->
            val value: Double = pair.first
            val range: Range = pair.second
            if (value < range.min || value > range.max) {
                "$label disi (${round3(value)})"
            } else {
                null
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // NRC 2007 — AT
    // ══════════════════════════════════════════════════════════
    // private fun calcAt(p: AnimalProfile): NrcResult {
    //     val ca = p.bodyWeight

    //     // At esitlikleri Mcal tabanli ele alinir.
    //     val (meMcal, cpG) = when (p.category) {
    //         // AnimalCategory.AT_HAFIF -> Pair(0.0333 * ca.pow(0.75), ca * 1.26)
    //         // AnimalCategory.AT_ORTA,
    //         // AnimalCategory.ESEK_KATIR_DINLENME -> Pair(0.0500 * ca.pow(0.75), ca * 1.44)
    //         // AnimalCategory.AT_AGIR,
    //         // AnimalCategory.ESEK_KATIR_CALISMA -> Pair(0.0671 * ca.pow(0.75), ca * 1.62)
    //         else -> Pair(0.0333 * ca.pow(0.75), ca * 1.26)
    //     }

    //     val meMj = mcalToMj(meMcal)
    //     val dmKg = meMj / 9.0
    //     val caG = ca * 0.040
    //     val pG  = ca * 0.028
    //     val mgG = ca * 0.018
    //     val naG = ca * 0.020
    //     val kG  = ca * 0.055

    //     val (thrG, trpG, sG) = estimateAaAndSulfurFromCp(cpG)

    //     return NrcResult(
    //         dmKg = round2(dmKg), meMj = round2(meMj),
    //         nelMj = round2(meMj * 0.60), nemMj = 0.0, negMj = 0.0,
    //         cpG = round1(cpG), rdpG = 0.0, rupG = 0.0,
    //         ndfPct = 35.0,
    //         caG = round1(caG), pG = round1(pG), mgG = round1(mgG),
    //         naG = round1(naG), kG = round1(kG),
    //         lysG = round1(cpG * 0.042), metG = round1(cpG * 0.016),
    //         thrG = round1(thrG), trpG = round1(trpG), sG = round1(sG),
    //         notes = "NRC 2007 Horses (ME: MJ/gun)"
    //     )
    // }

    // private fun calcSuUrunleri(p: AnimalProfile): NrcResult {
    //     val bwKg = p.bodyWeight.coerceAtLeast(0.0)
    //     val temp = p.waterTemp
    //     val gcaa = p.targetDailyGain.coerceAtLeast(0.0)

    //     val tempFactor = 1.0 + (temp - 15.0) * 0.02
    //     val meDensityMcalKg: Double
    //     val cpPct: Double

    //     val meMcal = when (p.category) {
    //         // AnimalCategory.ALABALIK_YAVRU -> {
    //         // AnimalCategory.ALABALIK_BUYUTME,
    //         // AnimalCategory.ALABALIK_PAZAR -> {
    //         // AnimalCategory.LEVREK_YAVRU,
    //         // AnimalCategory.LEVREK_BUYUTME,
    //         // AnimalCategory.CIPURA_YAVRU,
    //         // AnimalCategory.CIPURA_BUYUTME -> {
    //         // AnimalCategory.SAZAN_BUYUTME -> {
    //             meDensityMcalKg = 2.95
    //             cpPct = 35.0
    //             (0.08 * bwKg.pow(0.80) + 0.0011 * gcaa) * tempFactor
    //         }
    //         else -> {
    //             meDensityMcalKg = 3.10
    //             cpPct = 40.0
    //             (0.09 * bwKg.pow(0.80) + 0.0012 * gcaa) * tempFactor
    //         }
    //     }

    //     val dmKg = (meMcal / meDensityMcalKg).coerceAtLeast(0.0)
    //     val cpG = dmKg * 1000.0 * (cpPct / 100.0)
    //     val caG = dmKg * 1000.0 * 0.015
    //     val pG = dmKg * 1000.0 * 0.012

    //     val (thrG, trpG, sG) = estimateAaAndSulfurFromCp(cpG)

    //     return NrcResult(
    //         dmKg = round3(dmKg), meMj = round2(meMcal * 4.184),
    //         nelMj = 0.0, nemMj = 0.0, negMj = 0.0,
    //         cpG = round1(cpG), rdpG = 0.0, rupG = 0.0,
    //         ndfPct = 0.0,
    //         caG = round2(caG), pG = round2(pG), mgG = round2(dmKg * 0.50),
    //         naG = round2(dmKg * 0.30), kG = round2(dmKg * 0.80),
    //         lysG = round2(cpG * 0.060), metG = round2(cpG * 0.025),
    //         thrG = round2(thrG), trpG = round2(trpG), sG = round2(sG),
    //         notes = "NRC 2011 Fish - enerji ME: MJ/gun, su ${temp}C"
    //     )
    // }

    // ══════════════════════════════════════════════════════════
    // TAVŞAN
    // ══════════════════════════════════════════════════════════
    private fun calcTavsan1997(p: AnimalProfile): NrcResult {
        val ca = p.bodyWeight * 1000
        val meMcal = ca * 0.00042 + (if (p.milkYield > 0) p.milkYield * 3.5 else 0.0)
        val cpG  = ca * 0.0022 + (if (p.milkYield > 0) p.milkYield * 45 else 0.0)
        val dmKg = meMcal / 2.45
        val (thrG, trpG, sG) = estimateAaAndSulfurFromCp(cpG)
        return NrcResult(
            dmKg = round2(dmKg), meMj = round2(meMcal * 4.184),
            nelMj = 0.0, nemMj = 0.0, negMj = 0.0,
            cpG = round1(cpG), rdpG = 0.0, rupG = 0.0,
            ndfPct = 12.0,
            caG = round2(dmKg * 1000 * 0.008), pG = round2(dmKg * 1000 * 0.005),
            mgG = round2(dmKg * 0.30), naG = round2(dmKg * 0.22), kG = round2(dmKg * 0.60),
            lysG = round2(cpG * 0.058), metG = round2(cpG * 0.022),
            thrG = round2(thrG), trpG = round2(trpG), sG = round2(sG),
            notes = "NRC 1997 Other Species (Rabbit) - enerji ME: MJ/gun"
        )
    }

    private fun estimateAaAndSulfurFromCp(cpG: Double): Triple<Double, Double, Double> {
        // CP tabanli yaklasik oranlar: Thr %3.8, Trp %1.1, Sulfur %0.9
        return Triple(cpG * 0.038, cpG * 0.011, cpG * 0.009)
    }

    private fun round1(v: Double) = (v * 10).roundToInt() / 10.0
    private fun round2(v: Double) = (v * 100).roundToInt() / 100.0
    private fun round3(v: Double) = (v * 1000).roundToInt() / 1000.0
    private fun Double.roundToInt() = kotlin.math.round(this).toInt()

    private fun mcalToMj(value: Double): Double = value * 4.184
}