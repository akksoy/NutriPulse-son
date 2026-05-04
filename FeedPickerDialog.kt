package com.nutripulse.app.ui.ration

import android.app.Dialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.nutripulse.app.data.AppDatabase
import com.nutripulse.app.data.model.AnimalProfile
import com.nutripulse.app.data.model.AnimalSpecies
import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Tum yemleri grup grup gosterir; hayvan varsa onu sadece onceliklendirme icin kullanir.
 */
class FeedPickerDialog : DialogFragment() {

    private var animalProfile: AnimalProfile? = null
    private var selectedFeeds = mutableListOf<Feed>()
    private var onConfirm: (List<Feed>) -> Unit = {}

    private val feedCheckMap = mutableMapOf<Int, Boolean>()

    companion object {
        fun newInstance(
            animal: AnimalProfile? = null,
            currentFeeds: List<Feed>,
            onConfirm: (List<Feed>) -> Unit
        ): FeedPickerDialog {
            return FeedPickerDialog().apply {
                animalProfile = animal
                selectedFeeds = currentFeeds.toMutableList()
                this.onConfirm = onConfirm
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val animal = animalProfile

        val feedList = mutableListOf<Feed>()
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 8)
        }
        val checkBoxes = mutableListOf<CheckBox>()

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(if (animal == null) "Fabrika Stok Listesi" else "${animal.profileName} icin Fabrika Stogundan Yem Sec")
            .setNegativeButton("İptal") { _, _ -> dismiss() }
            .setPositiveButton("Seç") { _, _ ->
                val checked = feedList.filter { feedCheckMap[it.id] == true }
                if (checked.isEmpty()) {
                    Toast.makeText(context, "En az 1 yem seçin!", Toast.LENGTH_SHORT).show()
                } else {
                    selectedFeeds = checked.toMutableList()
                    onConfirm(selectedFeeds)
                    dismiss()
                }
            }

        val scroll = ScrollView(requireContext())
        scroll.addView(content)

        content.addView(TextView(requireContext()).apply {
            text = "Yemler fabrika stok listesinden, kategori bazinda gosterilir; secilenler aday havuza eklenir."
            textSize = 11f
        })

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val allFeeds = withContext(Dispatchers.IO) { db.feedDao().getAllFeedsSnapshot() }
            val visibleFeeds = rankFeedsForSelection(allFeeds, animal)

            withContext(Dispatchers.Main) {
                content.removeAllViews()
                checkBoxes.clear()

                if (visibleFeeds.isEmpty()) {
                    content.addView(TextView(requireContext()).apply { text = "Bu tur icin yem bulunamadi" })
                } else {
                    feedList.clear()
                    feedList.addAll(visibleFeeds)

                    selectedFeeds.forEach { selected -> feedCheckMap[selected.id] = true }

                    val selectAll = CheckBox(requireContext()).apply {
                        text = "Hepsini Sec"
                        isChecked = visibleFeeds.isNotEmpty() && visibleFeeds.all { feedCheckMap[it.id] == true }
                        setOnCheckedChangeListener { _, checked ->
                            visibleFeeds.forEach { feed -> feedCheckMap[feed.id] = checked }
                            checkBoxes.forEach { cb -> cb.isChecked = checked }
                        }
                    }
                    content.addView(selectAll)

                    val grouped = visibleFeeds.groupBy { it.category }
                    val groupedOrder = if (animal == null) FeedCategories.ALL else rankCategoriesForSpecies(animal.species)
                    groupedOrder.forEach { categoryCode ->
                        val feedsInCategory = grouped[categoryCode].orEmpty()
                        if (feedsInCategory.isEmpty()) return@forEach

                        content.addView(TextView(requireContext()).apply {
                            text = FeedCategories.displayName(categoryCode)
                            setTypeface(typeface, Typeface.BOLD)
                            textSize = 13f
                            setPadding(0, 16, 0, 8)
                        })

                        feedsInCategory.forEach { feed ->
                            val cb = CheckBox(requireContext()).apply {
                                val price = if (feed.pricePerKg > 0) "  💰 ₺${String.format("%.2f", feed.pricePerKg)}/kg" else "  ❓ Fiyat yok"
                                val nelText = if (feed.nel > 0) "NEL:${fmt(feed.nel)}" else ""
                                val hpText = if (feed.cp > 0) "HP:${fmt(feed.cp)}%" else ""
                                val caText = if (feed.ca > 0) "Ca:${fmt(feed.ca)}%" else ""
                                text = "${feed.name}\n    ${nelText} ${hpText} ${caText}${price}"
                                textSize = 12f
                                isChecked = feedCheckMap[feed.id] ?: false
                                setOnCheckedChangeListener { _, checked ->
                                    feedCheckMap[feed.id] = checked
                                    selectAll.isChecked = visibleFeeds.all { feedCheckMap[it.id] == true }
                                }
                            }
                            checkBoxes.add(cb)
                            content.addView(cb)
                        }
                    }
                }
            }
        }

        dialog.setView(scroll)
        return dialog.create()
    }

    // Bu dosya artık kullanılmıyor. Yem seçimi motoru FeedSelectionEngine ile yapılacak. Koddan kaldırıldı.
    private fun rankFeedsForSelection(allFeeds: List<Feed>, animal: AnimalProfile?): List<Feed> {
        val allowedForSpecies = when (animal?.species) {
            AnimalSpecies.SIGIR, AnimalSpecies.MANDA -> setOf(
                FeedCategories.ROUGHAGE_WET,
                FeedCategories.ROUGHAGE_DRY,
                FeedCategories.GRAIN,
                FeedCategories.PROTEIN,
                FeedCategories.BYPRODUCT,
                FeedCategories.FAT,
                FeedCategories.MINERAL,
                FeedCategories.PREMIKS,
                FeedCategories.VITAMIN,
                FeedCategories.ADDITIVE
            )
            AnimalSpecies.KOYUN, AnimalSpecies.KECI -> setOf(
                FeedCategories.ROUGHAGE_WET,
                FeedCategories.ROUGHAGE_DRY,
                FeedCategories.GRAIN,
                FeedCategories.PROTEIN,
                FeedCategories.BYPRODUCT,
                FeedCategories.FAT,
                FeedCategories.MINERAL,
                FeedCategories.PREMIKS,
                FeedCategories.VITAMIN,
                FeedCategories.ADDITIVE
            )
            AnimalSpecies.KANATLI -> setOf(
                FeedCategories.GRAIN,
                FeedCategories.PROTEIN,
                FeedCategories.BYPRODUCT,
                FeedCategories.FAT,
                FeedCategories.MINERAL,
                FeedCategories.PREMIKS,
                FeedCategories.VITAMIN,
                FeedCategories.ADDITIVE
            )
            else -> FeedCategories.ALL.toSet()
        }

        val excludedFeedNames = when (animal?.species) {
            AnimalSpecies.KANATLI -> setOf("Broiler", "Yemlik", "Alabalik", "Levrek", "Cipura", "Bezelye Dane", "Sazan", "Yusuf")
            AnimalSpecies.KOYUN -> setOf("Broiler", "Yemlik", "Alabalik", "Levrek", "Cipura", "Bezelye Dane", "Muz Kuru", "Sazan")
            AnimalSpecies.KECI -> setOf("Broiler", "Yemlik", "Alabalik", "Levrek", "Cipura", "Sazan", "Yusuf")
            AnimalSpecies.AT -> setOf("Broiler", "Yemlik", "Alabalik", "Levrek", "Cipura", "Bezelye Dane", "Sazan")
            else -> emptySet()
        }

        fun categoryPriority(category: String): Int = when (animal?.species) {
            AnimalSpecies.SIGIR, AnimalSpecies.MANDA -> when (category) {
                FeedCategories.ROUGHAGE_WET -> 0
                FeedCategories.ROUGHAGE_DRY -> 1
                FeedCategories.GRAIN -> 2
                FeedCategories.PROTEIN -> 3
                FeedCategories.BYPRODUCT -> 4
                FeedCategories.FAT -> 5
                FeedCategories.MINERAL -> 6
                FeedCategories.PREMIKS -> 7
                FeedCategories.VITAMIN -> 8
                FeedCategories.ADDITIVE -> 9
                FeedCategories.AQUA -> 10
                else -> 11
            }
            AnimalSpecies.KOYUN, AnimalSpecies.KECI -> when (category) {
                FeedCategories.ROUGHAGE_DRY -> 0
                FeedCategories.ROUGHAGE_WET -> 1
                FeedCategories.GRAIN -> 2
                FeedCategories.PROTEIN -> 3
                FeedCategories.BYPRODUCT -> 4
                FeedCategories.MINERAL -> 5
                FeedCategories.PREMIKS -> 6
                FeedCategories.VITAMIN -> 7
                FeedCategories.ADDITIVE -> 8
                FeedCategories.FAT -> 9
                FeedCategories.AQUA -> 10
                else -> 11
            }
            else -> FeedCategories.ALL.indexOf(category).takeIf { it >= 0 } ?: 99
        }

        fun feedScore(feed: Feed): Double {
            val energy = maxOf(feed.nel, feed.me * 0.55, 0.0)
            val protein = feed.cp * 0.12 + feed.lys * 0.05 + feed.met * 0.05
            val fiber = feed.ndf * 0.04 + feed.adf * 0.02
            val minerals = (feed.ca + feed.p + feed.mg + feed.na + feed.k + feed.s) * 0.05
            val fat = feed.fat * 0.05
            val pricePenalty = if (feed.pricePerKg > 0) feed.pricePerKg * 0.12 else 0.0
            val speciesBias = when (animal?.species) {
                AnimalSpecies.SIGIR, AnimalSpecies.MANDA -> if (feed.category == FeedCategories.ROUGHAGE_WET) 1.15 else 1.0
                AnimalSpecies.KOYUN, AnimalSpecies.KECI -> if (feed.category == FeedCategories.ROUGHAGE_DRY) 1.15 else 1.0
                else -> 1.0
            }
            return ((energy + protein + fiber + minerals + fat) * speciesBias) - pricePenalty
        }

        return allFeeds
            .asSequence()
            .filter { it.category in allowedForSpecies }
            .filter { feed -> 
                excludedFeedNames.none { excluded -> feed.name.contains(excluded, ignoreCase = true) }
            }
            .sortedWith(
                compareBy<Feed> { categoryPriority(it.category) }
                    .thenByDescending { feedScore(it) }
                    .thenBy { if (it.pricePerKg > 0) it.pricePerKg else Double.MAX_VALUE }
                    .thenBy { it.name }
            )
            .toList()
    }

    private fun rankCategoriesForSpecies(species: String): List<String> = when (species) {
        AnimalSpecies.SIGIR, AnimalSpecies.MANDA -> listOf(
            FeedCategories.ROUGHAGE_WET,
            FeedCategories.ROUGHAGE_DRY,
            FeedCategories.GRAIN,
            FeedCategories.PROTEIN,
            FeedCategories.BYPRODUCT,
            FeedCategories.FAT,
            FeedCategories.MINERAL,
            FeedCategories.PREMIKS,
            FeedCategories.VITAMIN,
            FeedCategories.ADDITIVE,
            FeedCategories.AQUA
        )
        AnimalSpecies.KOYUN, AnimalSpecies.KECI -> listOf(
            FeedCategories.ROUGHAGE_DRY,
            FeedCategories.ROUGHAGE_WET,
            FeedCategories.GRAIN,
            FeedCategories.PROTEIN,
            FeedCategories.BYPRODUCT,
            FeedCategories.MINERAL,
            FeedCategories.PREMIKS,
            FeedCategories.VITAMIN,
            FeedCategories.ADDITIVE,
            FeedCategories.FAT,
            FeedCategories.AQUA
        )
        else -> FeedCategories.ALL
    }

    private fun fmt(v: Double): String {
        return if (kotlin.math.abs(v - v.toInt()) < 0.01) v.toInt().toString() else String.format("%.1f", v)
    }
}