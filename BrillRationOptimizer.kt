package com.nutripulse.app.data.solver.impl

import com.nutripulse.app.data.model.AnimalProfile
import com.nutripulse.app.data.model.AnimalSpecies
import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories
import com.nutripulse.app.data.model.RationItem
import com.nutripulse.app.data.solver.HiGhsFallbackOptimizer
import kotlin.math.max
import kotlin.random.Random

/**
 * Sığır/Manda için Brill LP (least-cost) çözümü üretir ve RationResult döndürür.
 *
 * DÜZELTİLEN HATALAR:
 * 1. FABRIKA modunda roughageMinPct artık 0.0 olarak ayarlanıyor (TMR kaba yem kısıtı sızıntısı giderildi)
 * 2. FABRIKA modunda categoryMinConstraints'ten kaba yem kısıtları çıkarıldı
 * 3. FABRIKA modunda ConstraintEngine bounds'tan roughageMinPct ve peNdfMin sıfırlandı
 */
object BrillRationOptimizer {

    fun optimize(inp: RationOptimizer.OptimizationInput): RationOptimizer.RationResult {
        val animal = RationOptimizer.resolveAnimalRequirements(inp.animal)

        // ── FABRIKA MODU kontrolü (FIX #1) ──────────────────────────────────────
        val isFactoryMode = inp.rationMode == "FABRIKA"

        // Yem listesini karıştır
        val validFeeds = inp.feeds.filter { it.dm in 0.1..100.0 }.shuffled(Random(System.currentTimeMillis()))

        if (validFeeds.size < 2) return error("En az 2 gecerli yem secin")

        if (animal.species != AnimalSpecies.SIGIR && animal.species != AnimalSpecies.MANDA) {
            return error("Brill LP su an sadece Sigir/Manda icin aktif.")
        }

        val standards = RationStandards.constraintsFor(animal.species, inp.balanceMode)

        // Hedefler (NRC)
        val dmT = inp.dmTargetOverrideKg.takeIf { it > 0.1 } ?: max(1.0, animal.reqDmKg)
        val nelT = if (animal.reqNelMj > 0.1) animal.reqNelMj else dmT * 6.2
        val meT  = if (animal.reqMeMj  > 0.1) animal.reqMeMj  else nelT / 0.62
        val cpT  = if (animal.reqCpG   > 0.1) animal.reqCpG   else dmT * 160.0
        val caT  = if (animal.reqCaG   > 0.1) animal.reqCaG   else dmT * 4.5
        val pT   = if (animal.reqPG    > 0.1) animal.reqPG    else dmT * 2.8
        val mgT  = if (animal.reqMgG   > 0.1) animal.reqMgG   else dmT * 1.8
        val naT  = if (animal.reqNaG   > 0.1) animal.reqNaG   else 0.0
        val kT   = if (animal.reqKG    > 0.1) animal.reqKG    else 0.0
        val lysT = if (animal.reqLysG  > 0.1) animal.reqLysG  else 0.0
        val metT = if (animal.reqMetG  > 0.1) animal.reqMetG  else 0.0
        val rdpT = if (animal.reqRdpG  > 0.1) animal.reqRdpG  else 0.0
        val rupT = if (animal.reqRupG  > 0.1) animal.reqRupG  else 0.0

        // ── Brill hard constraint'ler ─────────────────────────────────────────────
        val ndfMin        = inp.customNdfMin.takeIf        { it > 0 } ?: animal.reqNdfPct.takeIf { it > 0 } ?: standards.ndfMinPct
        val ndfMax        = inp.customNdfMax.takeIf        { it > 0 } ?: standards.ndfMaxPct
        val fatMaxPct     = inp.customFatMaxPct.takeIf    { it > 0 } ?: standards.fatMaxPct
        val starchMaxPct  = inp.customStarchMaxPct.takeIf { it > 0 } ?: standards.starchMaxPct
        val nfcMaxPct     = inp.customNfcMaxPct.takeIf   { it > 0 } ?: standards.nfcMaxPct
        val sugarMax      = inp.customSugarMaxPct.takeIf  { it > 0 } ?: standards.sugarMaxPct
        val cpMaxPct      = inp.customCpMaxPct.takeIf    { it > 0 } ?: standards.cpMaxPct
        val dcadMin       = inp.customDcadMin.takeIf     { it > 0 } ?: standards.dcadProxyMin
        val peNdfMin      = inp.customPeNdfMinPct.takeIf { it > 0 } ?: standards.peNdfProxyMinPct

        // FABRIKA modunda kaba yem minimum ─────────────────────────────────────────
        val roughageMinPct = if (isFactoryMode) {
            0.0  // Esnek - kısıtlama yok
        } else {
            inp.customRoughageMinPct.takeIf { it > 0 } ?: standards.roughageMinPct
        }

        val maxFeedSharePct = inp.maxFeedSharePct.takeIf { it > 0 } ?: 30.0

        // FABRIKA modunda kategori kısıtlamaları gevşet ───────────────────
        val categoryMinConstraints = buildList {
            if (!isFactoryMode) {
                add(BrillLpModel.CategoryMinConstraint(FeedCategories.ROUGHAGE_WET, 0.3))
                add(BrillLpModel.CategoryMinConstraint(FeedCategories.ROUGHAGE_DRY, 0.3))
            }
            add(BrillLpModel.CategoryMinConstraint(FeedCategories.GRAIN,   0.3))
            add(BrillLpModel.CategoryMinConstraint(FeedCategories.PROTEIN, 0.2))
            add(BrillLpModel.CategoryMinConstraint(FeedCategories.MINERAL, 0.02))
        }

        data class SolveAttempt(
            val tolPct: Double,
            val dmTolPct: Double,
            val includeNelUpperBand: Boolean,
            val includeCpUpperBand: Boolean
        )

        val solveAttempts = listOf(
            SolveAttempt(tolPct = 1.5, dmTolPct = 1.5, includeNelUpperBand = true,  includeCpUpperBand = true),
            SolveAttempt(tolPct = 2.0, dmTolPct = 2.0, includeNelUpperBand = true,  includeCpUpperBand = true),
            SolveAttempt(tolPct = 3.0, dmTolPct = 3.0, includeNelUpperBand = true,  includeCpUpperBand = true),
            SolveAttempt(tolPct = 5.0, dmTolPct = 5.0, includeNelUpperBand = true,  includeCpUpperBand = true)
        )

        val attemptNotes  = mutableListOf<String>()
        var solvedAttempt = -1
        var lp: BrillLpOptimizer.SolveResult? = null

        for ((idx, attempt) in solveAttempts.withIndex()) {
            val candidate = BrillLpOptimizer.solveCattleBuffalo(
                animal                  = animal,
                feeds                   = validFeeds,
                dmTargetKg              = dmT,
                dmTolPct                = attempt.dmTolPct,
                nutrientTolPct          = attempt.tolPct,
                ndfMinPct               = ndfMin,
                ndfMaxPct               = ndfMax,
                roughageMinPct          = roughageMinPct,
                cpTargetG               = cpT,
                nelTargetMj             = nelT,
                caTargetG               = caT,
                pTargetG                = pT,
                rdpTargetG              = rdpT,
                rupTargetG              = rupT,
                lysTargetG              = lysT,
                metTargetG              = metT,
                fatMaxPct               = fatMaxPct,
                starchMaxPct            = starchMaxPct,
                nfcMaxPct               = nfcMaxPct,
                maxFeedSharePct         = maxFeedSharePct,
                includeNelUpperBand     = attempt.includeNelUpperBand,
                includeCpUpperBand      = attempt.includeCpUpperBand,
                categoryMinConstraints  = categoryMinConstraints
            )

            val nelBandText = if (attempt.includeNelUpperBand) "NEL_ust:Açık" else "NEL_ust:Kapalı"
            val cpBandText  = if (attempt.includeCpUpperBand)  "CP_ust:Açık"  else "CP_ust:Kapalı"
            attemptNotes.add("Deneme${idx + 1}: nutrient_tol=%${r1(attempt.tolPct)}, dm_tol=%${r1(attempt.dmTolPct)}, $nelBandText, $cpBandText -> durum=${candidate.status}")

            lp = candidate
            if (candidate.status == BrillLpOptimizer.SolveResult.Status.OPTIMAL) {
                solvedAttempt = idx + 1
                break
            }
        }

        val solvedLp = lp ?: return error("Brill LP calistirilamadi")

        // ── LP başarısız olursa otomatik fallback ─────────────────────────────────
        if (solvedLp.status != BrillLpOptimizer.SolveResult.Status.OPTIMAL) {
            val note      = attemptNotes.joinToString(" | ")
            val diagnosis = diagnoseInfeasibility(
                animal, validFeeds, dmT, nelT, cpT, caT, pT,
                ndfMin, ndfMax, roughageMinPct
            )

            // Otomatik fallback dene
            val fallback = HiGhsFallbackOptimizer.tryOptimize(inp)
            if (fallback.succeeded && fallback.rationResult != null) {
                return fallback.rationResult.copy(
                    status   = "OPTIMAL_FALLBACK",
                    warnings = fallback.rationResult.warnings + listOf(
                        "Brill LP cozum bulunamadi: ${solvedLp.message}",
                        "Otomatik gevsetme ile cozum uretildi: ${fallback.stepLabel}"
                    ),
                    message  = "HiGHS Fallback (${fallback.stepLabel}) | ${diagnosis}"
                )
            }

            return error("Brill LP cozum bulunamadi: ${solvedLp.message}. $note\n\n$diagnosis")
        }

        val x      = solvedLp.xKgDm
        val actDm  = x.sum().coerceAtLeast(1e-6)

        if (actDm <= 0.05) return error("Secili yemlerle rasyon olusturulamadi")

        fun dmCost(f: Feed): Double          = BrillUnits.dmCostTlPerKgDm(f)
        fun nfcPct(f: Feed): Double          = (100.0 - (f.cp + f.ndf + f.fat + f.ash)).coerceIn(0.0, 100.0)
        fun dcadProxy(f: Feed): Double       = f.na * 434.98 + f.k * 255.74 - f.s * 624.15
        fun peNdfProxy(f: Feed): Double {
            val e = when (f.category) {
                FeedCategories.ROUGHAGE_DRY                        -> 1.00
                FeedCategories.ROUGHAGE_WET                        -> 0.85
                FeedCategories.BYPRODUCT                           -> 0.45
                FeedCategories.GRAIN, FeedCategories.PROTEIN       -> 0.25
                else                                               -> 0.20
            }
            return f.ndf * e
        }

        val practicalInclusionDmKg = 0.05
        val items: List<RationItem> = validFeeds.indices.mapNotNull { i ->
            val f   = validFeeds[i]
            val thr = when (f.category) {
                FeedCategories.MINERAL,
                FeedCategories.PREMIKS,
                FeedCategories.VITAMIN,
                FeedCategories.ADDITIVE -> 0.01
                FeedCategories.FAT      -> 0.02
                else                    -> practicalInclusionDmKg
            }
            if (x[i] < thr) return@mapNotNull null
            val fresh = x[i] / (f.dm / 100.0).coerceAtLeast(1e-6)
            RationItem(
                feedId       = f.id,
                feedName     = f.name,
                feedCategory = f.category,
                amountKg     = r3(fresh),
                amountDmKg   = r3(x[i]),
                dmPct        = r2(x[i] / actDm * 100.0),
                costPerDay   = r2(x[i] * dmCost(f)),
                pricePerKg   = f.pricePerKg
            )
        }

        val maxPracticalFeeds = 20
        var feedCountWarning  = ""
        val finalItems = if (items.size > maxPracticalFeeds) {
            val removedCount = items.size - maxPracticalFeeds
            feedCountWarning = "⚠ $removedCount yem çıkarıldı (pratik limit: $maxPracticalFeeds)"
            items.sortedByDescending { it.amountDmKg }.take(maxPracticalFeeds)
        } else {
            items
        }

        val removedItems = if (items.size > maxPracticalFeeds) {
            items.sortedByDescending { it.amountDmKg }.drop(maxPracticalFeeds)
        } else {
            emptyList()
        }

        // Nutrient totals üzerinden hesapla
        val finalFeedIds = finalItems.map { it.feedId }.toSet()
        val finalFeeds   = validFeeds.filter { it.id in finalFeedIds }
        val finalX       = finalFeeds.map { f -> x[validFeeds.indexOf(f)] }.toDoubleArray()
        val actDmFinal   = finalX.sum().coerceAtLeast(1e-6)

        fun nutr(get: (Feed) -> Double): Double  = finalFeeds.indices.sumOf { i -> finalX[i] * get(finalFeeds[i]) }
        fun nutrG(get: (Feed) -> Double): Double = nutr(get) * 10.0

        val actNel    = nutr  { it.nel }
        val actMe     = nutr  { if (it.me > 0) it.me else (it.nel / 0.62) }
        val actCp     = nutrG { it.cp }
        val actRdp    = nutrG { it.rdp }
        val actRup    = nutrG { it.rup }
        val actNdf    = nutr  { it.ndf }    / actDmFinal
        val actAdf    = nutr  { it.adf }    / actDmFinal
        val actCa     = nutrG { it.ca }
        val actP      = nutrG { it.p }
        val actMg     = nutrG { it.mg }
        val actNa     = nutrG { it.na }
        val actK      = nutrG { it.k }
        val actLys    = nutrG { it.lys }
        val actMet    = nutrG { it.met }
        val actStarch = nutr  { it.starch } / actDmFinal
        val actSugar  = nutr  { it.sugar }  / actDmFinal
        val actFat    = nutr  { it.fat }    / actDmFinal
        val actNfc    = nutr  { nfcPct(it) }   / actDmFinal
        val actDcad   = nutr  { dcadProxy(it) } / actDmFinal
        val actPeNdf  = nutr  { peNdfProxy(it) }/ actDmFinal

        val totalCost = finalFeeds.indices.sumOf { i -> finalX[i] * dmCost(finalFeeds[i]) }

        val nutrient = RationOptimizer.NutrientActual(
            dmKg      = r2(actDmFinal), dmTarget  = r2(dmT),
            nelMj     = r2(actNel),     nelTarget  = r2(nelT),
            meMj      = r2(actMe),      meTarget   = r2(meT),
            cpG       = r1(actCp),      cpTarget   = r1(cpT),
            rdpG      = r1(actRdp),     rdpTarget  = r1(rdpT),
            rupG      = r1(actRup),     rupTarget  = r1(rupT),
            ndfPct    = r2(actNdf),     ndfMin     = ndfMin,
            adfPct    = r2(actAdf),
            caG       = r1(actCa),      caTarget   = r1(caT),
            pG        = r1(actP),       pTarget    = r1(pT),
            mgG       = r1(actMg),      mgTarget   = r1(mgT),
            naG       = r1(actNa),      naTarget   = r1(naT),
            kG        = r1(actK),       kTarget    = r1(kT),
            lysG      = r1(actLys),     lysTarget  = r1(lysT),
            metG      = r1(actMet),     metTarget  = r1(metT),
            totalCost = r2(totalCost),
            caPRatio  = if (actP > 0) r2(actCa / actP) else 0.0
        )

        val covList = listOf(
            if (dmT   > 0) actDmFinal / dmT   else 1.0,
            if (nelT  > 0) actNel     / nelT  else 1.0,
            if (meT   > 0) actMe      / meT   else 1.0,
            if (cpT   > 0) actCp      / cpT   else 1.0,
            if (ndfMin > 0 && ndfMax > 0) {
                when {
                    actNdf < ndfMin -> actNdf / ndfMin
                    actNdf > ndfMax -> ndfMax / actNdf
                    else            -> 1.0
                }
            } else if (ndfMin > 0) actNdf / ndfMin else 1.0,
            if (caT   > 0) actCa  / caT  else 1.0,
            if (pT    > 0) actP   / pT   else 1.0,
            if (mgT   > 0) actMg  / mgT  else 1.0
        ).filter { it.isFinite() && it > 0 }

        val limRatio = covList.minOrNull() ?: 1.0

        val warnings = mutableListOf<String>()
        val modeLabel = if (isFactoryMode) "Fabrika" else "TMR"
        warnings.add("ℹ HiGHS LP aktif ($modeLabel modu, heuristik kapali)")
        warnings.add("ℹ ${solvedLp.message}")
        warnings.add("ℹ Yem sayısı: ${finalItems.size} (toplam ${validFeeds.size} adaydan)")

        if (feedCountWarning.isNotEmpty()) warnings.add(feedCountWarning)

        if (removedItems.isNotEmpty()) {
            val removedDm   = removedItems.sumOf { it.amountDmKg }
            val removedCost = removedItems.sumOf { it.costPerDay }
            warnings.add("⚠ Silinen yemler: ${r2(removedDm)} kg KM, ₺${r2(removedCost)} maliyet kaybı")
            warnings.add(" → Besin kaybı olabilir, yem havuzunu çeşitlendirin")
        }

        if (solvedAttempt > 0) warnings.add("ℹ Retry sonucu: ${attemptNotes[solvedAttempt - 1]}")
        if (solvedAttempt == 3) warnings.add("⚠ 3. denemede NEL ust bandi devre disi birakildi")

        if (cpMaxPct   > 0 && (actCp    / (actDmFinal * 10.0)) > cpMaxPct   * 1.02) warnings.add("⚠ Ham protein fazla")
        if (starchMaxPct > 0 && actStarch > starchMaxPct * 1.02) warnings.add("⚠ Nisasta yuksek")
        if (sugarMax     > 0 && actSugar  > sugarMax     * 1.02) warnings.add("⚠ Seker yuksek")
        if (fatMaxPct    > 0 && actFat    > fatMaxPct    * 1.02) warnings.add("⚠ Yag yuksek")
        if (nfcMaxPct    > 0 && actNfc    > nfcMaxPct    * 1.02) warnings.add("⚠ NFC yuksek")
        if (dcadMin      > 0 && actDcad   < dcadMin      * 0.98) warnings.add("⚠ DCAD proxy dusuk")
        if (peNdfMin     > 0 && actPeNdf  < peNdfMin     * 0.98) warnings.add("⚠ peNDF proxy dusuk")

        // ── FIX #3: FABRIKA modunda ConstraintEngine sınırlarından kaba yem kısıtı çıkarıldı ──
        val bounds = if (isFactoryMode) {
            ConstraintEngine.generateBoundsForAnimal(animal, inp.balanceMode)
                .copy(roughageMinPct = 0.0, peNdfMin = 0.0)
        } else {
            ConstraintEngine.generateBoundsForAnimal(animal, inp.balanceMode)
        }

        val constraintViolations = ConstraintEngine.checkViolations(finalItems, animal, bounds, validFeeds)

        val resultStatus = when {
            constraintViolations.isNotEmpty()     -> "CONSTRAINT_FAILED"
            warnings.none { it.startsWith("⚠") } -> "OPTIMAL"
            else                                  -> "APPROXIMATE"
        }

        val allWarnings = mutableListOf<String>()
        if (constraintViolations.isNotEmpty()) {
            allWarnings.add("❌ NRC Hard Constraint İhlalleri:")
            allWarnings.addAll(constraintViolations.take(5))
            if (constraintViolations.size > 5) allWarnings.add(" +${constraintViolations.size - 5} daha")
        }
        allWarnings.addAll(warnings)

        val estMilk  = animal.milkYield       * limRatio
        val estGain  = animal.targetDailyGain * limRatio
        val revenue  = estMilk * inp.milkPrice
        val profit   = revenue - totalCost
        val count    = max(1, animal.animalCount)
        val coverage = r2((limRatio * 100.0).coerceIn(0.0, 120.0))

        val message = when (resultStatus) {
            "CONSTRAINT_FAILED" -> "NRC hard kısıtlamaları ihlal edildi. Yem havuzunu genişletin."
            "OPTIMAL"           -> "Rasyon olusturuldu (HiGHS LP – $modeLabel)"
            else                -> "Rasyon kisit uyarilariyla olusturuldu (HiGHS LP – $modeLabel)"
        }

        val analysisRows = listOf(
            RationOptimizer.NutrientAnalysisRow("KM",  r2(dmT),  r2(actDmFinal), r2(actDmFinal - dmT), 0.0, "kg", rowStatus(actDmFinal, dmT)),
            RationOptimizer.NutrientAnalysisRow("ME",  r2(meT),  r2(actMe),  r2(actMe  - meT),  0.0, "MJ", rowStatus(actMe,  meT)),
            RationOptimizer.NutrientAnalysisRow("NEL", r2(nelT), r2(actNel), r2(actNel - nelT), 0.0, "MJ", rowStatus(actNel, nelT)),
            RationOptimizer.NutrientAnalysisRow("HP",  r1(cpT),  r1(actCp),  r1(actCp  - cpT),  0.0, "g",  rowStatus(actCp,  cpT)),
            RationOptimizer.NutrientAnalysisRow("RDP", r1(rdpT), r1(actRdp), r1(actRdp - rdpT), 0.0, "g",  rowStatus(actRdp, rdpT)),
            RationOptimizer.NutrientAnalysisRow("RUP", r1(rupT), r1(actRup), r1(actRup - rupT), 0.0, "g",  rowStatus(actRup, rupT)),
            RationOptimizer.NutrientAnalysisRow("Ca",  r1(caT),  r1(actCa),  r1(actCa  - caT),  0.0, "g",  rowStatus(actCa,  caT)),
            RationOptimizer.NutrientAnalysisRow("P",   r1(pT),   r1(actP),   r1(actP   - pT),   0.0, "g",  rowStatus(actP,   pT)),
            RationOptimizer.NutrientAnalysisRow("Mg",  r1(mgT),  r1(actMg),  r1(actMg  - mgT),  0.0, "g",  rowStatus(actMg,  mgT)),
            RationOptimizer.NutrientAnalysisRow("Lys", r1(lysT), r1(actLys), r1(actLys - lysT), 0.0, "g",  rowStatus(actLys, lysT)),
            RationOptimizer.NutrientAnalysisRow("Met", r1(metT), r1(actMet), r1(actMet - metT), 0.0, "g",  rowStatus(actMet, metT)),
            RationOptimizer.NutrientAnalysisRow("NDF", r2(ndfMin), r2(actNdf), r2(actNdf - ndfMin), 0.0, "%", rowStatus(actNdf, ndfMin))
        )

        val shadowRows = solvedLp.shadowPrices.entries.map { (name, price) ->
            RationOptimizer.ShadowRow(name = name, price = price, unit = "TL/birim", note = "HiGHS dual")
        }

        return RationOptimizer.RationResult(
            status               = resultStatus,
            rationItems          = finalItems,
            nutrient             = nutrient,
            shadow               = if (shadowRows.isNotEmpty()) shadowRows else solvedLp.nutrientNames.mapIndexed { i, name ->
                RationOptimizer.ShadowRow(name = name, price = solvedLp.duals.getOrElse(i) { 0.0 }, unit = "TL/birim", note = "HiGHS dual")
            },
            yield                = RationOptimizer.YieldEstimate(
                coveragePct      = coverage,
                limitingNutrient = limitingNutrient(nutrient),
                estMilkL         = r2(estMilk),
                estGainG         = r2(estGain),
                milkRevenue      = r2(revenue),
                rationCost       = r2(totalCost),
                netProfit        = r2(profit),
                herdCost         = r2(totalCost * count),
                herdMonthNet     = r2(profit * count * 30.0)
            ),
            warnings             = allWarnings,
            message              = message,
            analysisRows         = analysisRows,
            smartRecommendations = listOf("HiGHS LP: Yem secimi solver tarafindan yapildi."),
            iterationCount       = solvedLp.iterations
        )
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Yardımcı fonksiyonlar
    // ────────────────────────────────────────────────────────────────────────────

    private fun limitingNutrient(n: RationOptimizer.NutrientActual): String = when {
        n.meTarget  > 0 && n.meMj  < n.meTarget  * 0.97 -> "ME"
        n.meTarget  > 0 && n.meMj  > n.meTarget  * 1.05 -> "ME (fazla)"
        n.nelTarget > 0 && n.nelMj < n.nelTarget * 0.97 -> "NEL"
        n.nelTarget > 0 && n.nelMj > n.nelTarget * 1.05 -> "NEL (fazla)"
        n.cpTarget  > 0 && n.cpG   < n.cpTarget  * 0.97 -> "Ham Protein"
        n.cpTarget  > 0 && n.cpG   > n.cpTarget  * 1.05 -> "Ham Protein (fazla)"
        n.caTarget  > 0 && n.caG   < n.caTarget  * 0.95 -> "Kalsiyum"
        n.caTarget  > 0 && n.caG   > n.caTarget  * 1.10 -> "Kalsiyum (fazla)"
        n.pTarget   > 0 && n.pG    < n.pTarget   * 0.95 -> "Fosfor"
        n.pTarget   > 0 && n.pG    > n.pTarget   * 1.10 -> "Fosfor (fazla)"
        n.mgTarget  > 0 && n.mgG   < n.mgTarget  * 0.90 -> "Magnezyum"
        n.mgTarget  > 0 && n.mgG   > n.mgTarget  * 1.15 -> "Magnezyum (fazla)"
        n.rdpTarget > 0 && n.rdpG  < n.rdpTarget * 0.92 -> "RDP (rumen protein düşük)"
        n.rupTarget > 0 && n.rupG  < n.rupTarget * 0.88 -> "RUP (by-pass protein düşük)"
        n.lysTarget > 0 && n.lysG  < n.lysTarget * 0.90 -> "Lizin (düşük)"
        n.metTarget > 0 && n.metG  < n.metTarget * 0.90 -> "Metiyonin (düşük)"
        n.ndfMin    > 0 && n.ndfPct < n.ndfMin   * 0.95 -> "NDF (düşük)"
        n.ndfMin    > 0 && n.ndfPct > n.ndfMin   * 1.20 -> "NDF (yüksek)"
        else -> "Kisitlar"
    }

    private fun diagnoseInfeasibility(
        animal: AnimalProfile,
        feeds: List<Feed>,
        dmT: Double,
        nelT: Double,
        cpT: Double,
        caT: Double,
        pT: Double,
        ndfMin: Double,
        ndfMax: Double,
        roughageMinPct: Double
    ): String {
        val sb = StringBuilder()
        sb.appendLine("=== İnfeasibility Teşhis ===")

        val nelRange         = feeds.map { it.nel }
        val nelAvg           = nelRange.average()
        val nelFeedMax       = nelRange.maxOrNull() ?: 0.0
        val nelFeedMin       = nelRange.minOrNull() ?: 0.0
        val requiredNelPerKg = if (dmT > 0) nelT / dmT else 0.0

        sb.appendLine()
        sb.appendLine("1. NEL Analizi:")
        sb.appendLine("   Hedef: $nelT MJ (${"%.1f".format(requiredNelPerKg)} MJ/kg)")
        sb.appendLine("   Yem NEL aralığı: ${"%.1f".format(nelFeedMin)} - ${"%.1f".format(nelFeedMax)} MJ/kg")
        sb.appendLine("   Ortalama NEL: ${"%.1f".format(nelAvg)} MJ/kg")
        if (nelFeedMax < requiredNelPerKg) {
            sb.appendLine("   ❌ SORUN: NEL hedefi yemlerle karşılanamaz!")
            sb.appendLine("   → Çözüm: Daha yüksek enerjili yem ekleyin (mısır, arpa, yağ)")
        }

        val cpRange      = feeds.map { it.cp }
        val cpFeedMax    = cpRange.maxOrNull() ?: 0.0
        val cpAvg        = cpRange.average()
        val requiredCpPct = if (dmT > 0) cpT / (dmT * 10) else 0.0

        sb.appendLine()
        sb.appendLine("2. Ham Protein Analizi:")
        sb.appendLine("   Hedef: $cpT g (${"%.1f".format(requiredCpPct)}%)")
        sb.appendLine("   Yem HP aralığı: ${"%.1f".format(cpRange.minOrNull() ?: 0.0)} - $cpFeedMax%")
        sb.appendLine("   Ortalama HP: ${"%.1f".format(cpAvg)}%")
        if (cpFeedMax < requiredCpPct) {
            sb.appendLine("   ❌ SORUN: HP hedefi yemlerle karşılanamaz!")
            sb.appendLine("   → Çözüm: Protein kaynağı ekleyin (soya, ayçiçeği)")
        }

        val ndfRange  = feeds.map { it.ndf }
        val ndfFeedMax = ndfRange.maxOrNull() ?: 0.0
        val ndfAvg    = ndfRange.average()

        sb.appendLine()
        sb.appendLine("3. NDF Analizi:")
        sb.appendLine("   NDF hedef araligi: $ndfMin% - ${if (ndfMax > 0) ndfMax.toInt() else "yok"}%")
        sb.appendLine("   Yem NDF araligi: ${"%.1f".format(ndfRange.minOrNull() ?: 0.0)} - $ndfFeedMax%")
        sb.appendLine("   Ortalama NDF: ${"%.1f".format(ndfAvg)}%")

        val roughageFeeds = feeds.filter {
            it.category == FeedCategories.ROUGHAGE_WET || it.category == FeedCategories.ROUGHAGE_DRY
        }

        sb.appendLine()
        sb.appendLine("4. Kaba Yem Analizi:")
        sb.appendLine("   Gerekli min: $roughageMinPct%")
        sb.appendLine("   Kaba yem sayısı: ${roughageFeeds.size}")
        if (roughageMinPct > 0 && roughageFeeds.isEmpty()) {
            sb.appendLine("   ❌ SORUN: Kaba yem yok!")
            sb.appendLine("   → Çözüm: Silaj, saman veya ot ekleyin")
        } else if (roughageMinPct == 0.0) {
            sb.appendLine("   ✅ Kaba yem kısıtlaması devre dışı (Fabrika modu)")
        }

        val caRange       = feeds.map { it.ca }
        val pRange        = feeds.map { it.p }
        val caFeedMax     = caRange.maxOrNull() ?: 0.0
        val pFeedMax      = pRange.maxOrNull()  ?: 0.0
        val requiredCaPct = if (dmT > 0) caT / (dmT * 10) else 0.0
        val requiredPPct  = if (dmT > 0) pT  / (dmT * 10) else 0.0

        sb.appendLine()
        sb.appendLine("5. Mineral Analizi:")
        sb.appendLine("   Ca hedef: $caT g (${"%.2f".format(requiredCaPct)}%)")
        sb.appendLine("   P hedef: $pT g (${"%.2f".format(requiredPPct)}%)")
        sb.appendLine("   Max Ca: $caFeedMax%, Max P: $pFeedMax%")
        if (caFeedMax < requiredCaPct * 0.8) {
            sb.appendLine("   ❌ SORUN: Ca hedefi karşılanamaz!")
            sb.appendLine("   → Çözüm: Kireçtaşı veya mineral ekleyin")
        }
        if (pFeedMax < requiredPPct * 0.8) {
            sb.appendLine("   ❌ SORUN: P hedefi karşılanamaz!")
            sb.appendLine("   → Çözüm: Dikalsiyum fosfat veya premiks ekleyin")
        }

        sb.appendLine()
        sb.appendLine("=== Genel Öneriler ===")
        sb.appendLine("• Yem çeşitliliğini artırın (en az 8-10 yem)")
        sb.appendLine("• Farklı besin profillerine sahip yemler ekleyin")
        sb.appendLine("• Kısıtlamaları gevşetmeyi deneyin (NDF, roughage min)")
        sb.appendLine("• Yemlerin besin değerlerini kontrol edin")

        return sb.toString()
    }

    private fun rowStatus(actual: Double, target: Double): String {
        if (target <= 1e-6) return "OK"
        val ratio = actual / target
        return when {
            ratio in 0.97..1.03 -> "OK"
            ratio in 0.92..1.08 -> "NEAR"
            else                -> "ALERT"
        }
    }

    private fun r1(v: Double) = kotlin.math.round(v * 10.0)   / 10.0
    private fun r2(v: Double) = kotlin.math.round(v * 100.0)  / 100.0
    private fun r3(v: Double) = kotlin.math.round(v * 1000.0) / 1000.0

    private fun error(msg: String) = RationOptimizer.RationResult(
        status               = "ERROR",
        rationItems          = emptyList(),
        nutrient             = emptyNutrient(),
        shadow               = emptyList(),
        yield                = emptyYield(),
        warnings             = listOf(msg),
        message              = msg
    )

    private fun emptyNutrient() = RationOptimizer.NutrientActual(
        dmKg  = 0.0, dmTarget  = 18.0,
        nelMj = 0.0, nelTarget = 80.0,
        meMj  = 0.0, meTarget  = 130.0,
        cpG   = 0.0, cpTarget  = 1500.0,
        rdpG  = 0.0, rdpTarget = 0.0,
        rupG  = 0.0, rupTarget = 0.0,
        ndfPct = 0.0, ndfMin   = 28.0,
        adfPct = 0.0,
        caG   = 0.0, caTarget  = 80.0,
        pG    = 0.0, pTarget   = 50.0,
        mgG   = 0.0, mgTarget  = 25.0,
        naG   = 0.0, naTarget  = 0.0,
        kG    = 0.0, kTarget   = 0.0,
        lysG  = 0.0, lysTarget = 0.0,
        metG  = 0.0, metTarget = 0.0,
        totalCost = 0.0,
        caPRatio  = 0.0
    )

    private fun emptyYield() = RationOptimizer.YieldEstimate(0.0, "-", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
}