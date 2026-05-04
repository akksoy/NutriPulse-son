package com.nutripulse.app.data.solver.impl

import com.nutripulse.app.data.model.AnimalSpecies
import com.nutripulse.app.data.model.FeedCategories
import com.nutripulse.app.data.solver.impl.SpeciesOptimizationProfile.BalanceMode

data class FeedUsageBound(val minPctDm: Double, val maxPctDm: Double)

data class RationConstraintProfile(
    val ndfMinPct: Double,
    val ndfMaxPct: Double,
    val roughageMinPct: Double,
    val cpMaxPct: Double,
    val starchMaxPct: Double,
    val sugarMaxPct: Double,
    val fatMaxPct: Double,
    val nfcMaxPct: Double,
    val dcadProxyMin: Double,
    val peNdfProxyMinPct: Double
)

object RationStandards {

    // Final calibration package freeze metadata.
    const val CALIBRATION_PACKAGE_VERSION = "v1.0"
    const val CALIBRATION_PACKAGE_DATE = "2026-04-04"

    fun constraintsForSpecies(species: String): RationConstraintProfile = when (species) {
        AnimalSpecies.SIGIR, AnimalSpecies.MANDA -> RationConstraintProfile(
            ndfMinPct = 28.0,
            ndfMaxPct = 45.0,
            roughageMinPct = 40.0,
            cpMaxPct = 20.0,
            starchMaxPct = 28.0,
            sugarMaxPct = 8.0,
            fatMaxPct = 6.5,
            nfcMaxPct = 45.0,
            dcadProxyMin = 100.0,
            peNdfProxyMinPct = 19.0
        )

        AnimalSpecies.KOYUN, AnimalSpecies.KECI -> RationConstraintProfile(
            ndfMinPct = 30.0,
            ndfMaxPct = 48.0,
            roughageMinPct = 45.0,
            cpMaxPct = 21.0,
            starchMaxPct = 32.0,
            sugarMaxPct = 10.0,
            fatMaxPct = 7.0,
            nfcMaxPct = 45.0,
            dcadProxyMin = 80.0,
            peNdfProxyMinPct = 21.0
        )

        else -> RationConstraintProfile(
            ndfMinPct = 0.0,
            ndfMaxPct = 0.0,
            roughageMinPct = 0.0,
            cpMaxPct = 0.0,
            starchMaxPct = 0.0,
            sugarMaxPct = 0.0,
            fatMaxPct = 0.0,
            nfcMaxPct = 0.0,
            dcadProxyMin = 0.0,
            peNdfProxyMinPct = 0.0
        )
    }

    fun usageBoundFor(species: String, category: String): FeedUsageBound {
        val b = when (category) {
            FeedCategories.ROUGHAGE_WET -> FeedUsageBound(2.0, if (species == AnimalSpecies.SIGIR || species == AnimalSpecies.MANDA) 45.0 else 50.0)
            FeedCategories.ROUGHAGE_DRY -> FeedUsageBound(2.0, if (species == AnimalSpecies.AT) 75.0 else 65.0)
            FeedCategories.GRAIN -> FeedUsageBound(0.0, 40.0)
            FeedCategories.PROTEIN -> FeedUsageBound(0.0, 30.0)
            FeedCategories.BYPRODUCT -> FeedUsageBound(0.0, 20.0)
            FeedCategories.FAT -> FeedUsageBound(0.0, 6.0)
            FeedCategories.MINERAL -> FeedUsageBound(0.0, 5.0)
            FeedCategories.PREMIKS, FeedCategories.VITAMIN, FeedCategories.ADDITIVE -> FeedUsageBound(0.0, 3.0)
            FeedCategories.AQUA -> FeedUsageBound(0.0, 80.0)
            else -> FeedUsageBound(0.0, 25.0)
        }
        return if (species == AnimalSpecies.KANATLI || species == AnimalSpecies.SU) {
            when (category) {
                FeedCategories.ROUGHAGE_WET, FeedCategories.ROUGHAGE_DRY -> FeedUsageBound(0.0, 2.0)
                else -> b
            }
        } else b
    }

    fun constraintsFor(species: String, mode: BalanceMode): RationConstraintProfile {
        val base = constraintsForSpecies(species)
        if (base.ndfMinPct <= 0.0) return base

        return when (mode) {
            BalanceMode.LOW_COST -> base.copy(
                ndfMinPct = (base.ndfMinPct - 1.0).coerceAtLeast(0.0),
                roughageMinPct = (base.roughageMinPct - 2.0).coerceAtLeast(0.0),
                cpMaxPct = base.cpMaxPct + 1.0,
                starchMaxPct = base.starchMaxPct + 2.0,
                sugarMaxPct = base.sugarMaxPct + 1.0,
                fatMaxPct = base.fatMaxPct + 0.5,
                nfcMaxPct = base.nfcMaxPct + 2.0,
                dcadProxyMin = (base.dcadProxyMin - 10.0).coerceAtLeast(0.0),
                peNdfProxyMinPct = (base.peNdfProxyMinPct - 1.0).coerceAtLeast(0.0)
            )
            BalanceMode.BALANCED -> base
            BalanceMode.STRICT -> base.copy(
                ndfMinPct = base.ndfMinPct + 1.0,
                roughageMinPct = base.roughageMinPct + 2.0,
                cpMaxPct = (base.cpMaxPct - 1.0).coerceAtLeast(0.0),
                starchMaxPct = (base.starchMaxPct - 2.0).coerceAtLeast(0.0),
                sugarMaxPct = (base.sugarMaxPct - 1.0).coerceAtLeast(0.0),
                fatMaxPct = (base.fatMaxPct - 0.5).coerceAtLeast(0.0),
                nfcMaxPct = (base.nfcMaxPct - 2.0).coerceAtLeast(0.0),
                dcadProxyMin = base.dcadProxyMin + 10.0,
                peNdfProxyMinPct = base.peNdfProxyMinPct + 1.0
            )
        }
    }

    fun presetSummaryFor(species: String, mode: BalanceMode): String {
        val c = constraintsFor(species, mode)
        if (c.ndfMinPct <= 0.0) return "Preset yok"

        val parts = mutableListOf<String>()
        parts.add("NDF>=${fmt(c.ndfMinPct)}")
        if (c.roughageMinPct > 0) parts.add("Kaba>=${fmt(c.roughageMinPct)}")
        if (c.nfcMaxPct > 0) parts.add("NFC<=${fmt(c.nfcMaxPct)}")
        if (c.starchMaxPct > 0) parts.add("Nisasta<=${fmt(c.starchMaxPct)}")
        if (c.sugarMaxPct > 0) parts.add("Seker<=${fmt(c.sugarMaxPct)}")
        if (c.fatMaxPct > 0) parts.add("Yag<=${fmt(c.fatMaxPct)}")
        if (c.peNdfProxyMinPct > 0) parts.add("peNDF>=${fmt(c.peNdfProxyMinPct)}")
        if (c.dcadProxyMin > 0) parts.add("DCAD>=${fmt(c.dcadProxyMin)}")
        return parts.joinToString(" | ")
    }

    private fun fmt(v: Double): String {
        val asInt = v.toInt().toDouble()
        return if (kotlin.math.abs(v - asInt) < 0.001) asInt.toInt().toString() else String.format("%.1f", v)
    }
}