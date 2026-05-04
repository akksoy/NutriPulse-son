package com.nutripulse.app.ui.ration

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.nutripulse.app.R
import com.nutripulse.app.data.AppDatabase
import com.nutripulse.app.data.model.AnimalSpecies
import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories
import com.nutripulse.app.data.solver.SmartFeedSelector
import com.nutripulse.app.databinding.FragmentFeedPoolBinding
import com.nutripulse.app.databinding.ItemFeedPoolCardBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.nutripulse.app.data.model.AnimalProfile

/**
 * Brill tarzı yem havuzu seçici.
 * - Kategori tabları
 * - Her yemde checkbox + min/max girişi
 * - Akıllı otomatik seçim
 * - FragmentResult ile RationFragment'a bildirir
 * - Sadece seçili hayvan türüne uygun yemleri gösterir
 */
class FeedPoolFragment : Fragment() {

    private var _binding: FragmentFeedPoolBinding? = null
    private val binding get() = _binding!!

    data class PoolEntry(
        val feed: Feed,
        var selected: Boolean = false,
        var minKg: Double = 0.0,
        var maxKg: Double = -1.0      // -1 = varsayılan limit
    )

    private var allFeeds: List<Feed> = emptyList()
    private var poolEntries: MutableList<PoolEntry> = mutableListOf()
    private var filteredEntries: List<PoolEntry> = emptyList()
    private var activeCategory = "ALL"
    private var animalSpecies: String? = null
    private var rationMode: String = "TMR" // Varsayılan
    private lateinit var poolAdapter: PoolAdapter
    private val filterViews = mutableListOf<TextView>()

    companion object {
        const val RESULT_KEY = "feed_pool_result"
        fun newInstance(animalSpecies: String? = null, selectedFeedIds: IntArray? = null, rationMode: String = "TMR") = FeedPoolFragment().apply {
            arguments = Bundle().apply {
                animalSpecies?.let { putString("animal_species", it) }
                selectedFeedIds?.let { putIntArray("selected_ids", it) }
                putString("ration_mode", rationMode)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedPoolBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get animal species from arguments
        animalSpecies = arguments?.getString("animal_species")
        rationMode = arguments?.getString("ration_mode") ?: "TMR"

        poolAdapter = PoolAdapter(mutableListOf()) { entry ->
            entry.selected = !entry.selected
            updateCount()
            poolAdapter.notifyDataSetChanged()
        }
        binding.recyclerFeedPool.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFeedPool.adapter = poolAdapter

        setupSearch()
        setupQuickButtons()
        loadFeeds()

        // Wire switches and progress bar behavior
        binding.switchAutoRun.setOnCheckedChangeListener { _, checked ->
            // If turned on and we already have feeds, trigger smart selection
            if (checked) {
                if (poolEntries.isNotEmpty()) {
                    smartSelect()
                } else {
                    android.widget.Toast.makeText(requireContext(), "Yem listesi yüklenmeden otomatik seçim yapılamaz!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.switchAutoApply.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                val hasSelection = poolEntries.any { it.selected }
                if (hasSelection) {
                    applySelection()
                } else {
                    android.widget.Toast.makeText(requireContext(), "Otomatik uygulama için seçili yem yok!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnApply.setOnClickListener { applySelection() }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { filterFeeds(s?.toString() ?: "") }
        })
    }

    private fun setupQuickButtons() {
        binding.btnSelectAll.setOnClickListener {
            filteredEntries.forEach { it.selected = true }
            updateCount(); poolAdapter.notifyDataSetChanged()
        }
        binding.btnSelectNone.setOnClickListener {
            poolEntries.forEach { it.selected = false }
            updateCount(); poolAdapter.notifyDataSetChanged()
        }
        binding.btnSelectSmart.setOnClickListener {
            smartSelect()
        }
    }

    private fun loadFeeds() {
        lifecycleScope.launch {
            var feeds = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).feedDao().getAllFeedsSnapshot()
            }
            // Türkiye'de kullanılmayan yemleri filtrele
            feeds = feeds.filter { SmartFeedSelector.isTurkeyFeed(it) }
            
            // Filter feeds based on animal species and ration mode
            val filteredFeeds = if (animalSpecies != null) {
                filterFeedsBySpeciesAndMode(feeds, animalSpecies!!, rationMode)
            } else {
                feeds
            }
            allFeeds = filteredFeeds
            poolEntries = filteredFeeds.map { f ->
                PoolEntry(
                    feed = f,
                    selected = false,
                    maxKg = if (f.maxDailyKg > 0) f.maxDailyKg
                            else if (f.maxDmPct > 0) -1.0   // hesaplanacak
                            else -1.0
                )
            }.toMutableList()

            // Önceden seçilmişler
            val prevIds = arguments?.getIntArray("selected_ids")
            if (prevIds != null) {
                poolEntries.forEach { e -> e.selected = e.feed.id in prevIds }
            }

            buildCategoryTabs()
            filterFeeds("")
            updateCount()
            // If auto-run switch is enabled, trigger smart selection
            if (binding.switchAutoRun.isChecked) {
                smartSelect()
            }
        }
    }

    /**
     * Filter feeds based on animal species
     */
    private fun filterFeedsBySpeciesAndMode(allFeeds: List<Feed>, species: String, rationMode: String): List<Feed> {
        val allowedCategories = when {
            rationMode == "FABRIKA" -> setOf(
                FeedCategories.GRAIN,
                FeedCategories.PROTEIN,
                FeedCategories.BYPRODUCT,
                FeedCategories.FAT,
                FeedCategories.MINERAL,
                FeedCategories.VITAMIN,
                FeedCategories.PREMIKS,
                FeedCategories.ADDITIVE
            )
            species == AnimalSpecies.SIGIR || species == AnimalSpecies.MANDA -> setOf(
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
            species == AnimalSpecies.KOYUN || species == AnimalSpecies.KECI -> setOf(
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
            species == AnimalSpecies.KANATLI -> setOf(
                FeedCategories.GRAIN,
                FeedCategories.PROTEIN,
                FeedCategories.BYPRODUCT,
                FeedCategories.FAT,
                FeedCategories.MINERAL,
                FeedCategories.VITAMIN,
                FeedCategories.PREMIKS,
                FeedCategories.ADDITIVE
            )
            species == AnimalSpecies.AT -> setOf(
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
            else -> setOf(
                FeedCategories.GRAIN,
                FeedCategories.PROTEIN,
                FeedCategories.BYPRODUCT,
                FeedCategories.MINERAL,
                FeedCategories.VITAMIN
            )
        }

        return allFeeds.filter {
            it.category in allowedCategories && SmartFeedSelector.isSpeciesCompatible(it, species)
        }
    }

    private fun buildCategoryTabs() {
        val cats = mutableListOf("TÜMÜ" to "ALL")
        FeedCategories.ALL.forEach { code ->
            val count = poolEntries.count { it.feed.category == code }
            if (count > 0) cats.add("${FeedCategories.displayName(code).take(6)}($count)" to code)
        }
        cats.forEachIndexed { idx, (label, code) ->
            val tv = TextView(requireContext()).apply {
                text = label; textSize = 9f
                val lp = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                lp.marginEnd = 5; layoutParams = lp; setPadding(18, 10, 18, 10)
                if (idx == 0) {
                    setBackgroundResource(R.drawable.bg_filter_active)
                    setTextColor(Color.parseColor("#05080A"))
                } else {
                    setBackgroundResource(R.drawable.bg_filter_inactive)
                    setTextColor(Color.parseColor("#00FF88"))
                }
                setOnClickListener {
                    activeCategory = code
                    filterViews.forEach { v ->
                        v.setBackgroundResource(R.drawable.bg_filter_inactive)
                        v.setTextColor(Color.parseColor("#00FF88"))
                    }
                    setBackgroundResource(R.drawable.bg_filter_active)
                    setTextColor(Color.parseColor("#05080A"))
                    filterFeeds(binding.etSearch.text?.toString() ?: "")
                }
            }
            filterViews.add(tv)
            binding.categoryTabBar.addView(tv)
        }
    }

    private fun filterFeeds(query: String) {
        var filtered = poolEntries.toList()
        if (activeCategory != "ALL") filtered = filtered.filter { it.feed.category == activeCategory }
        if (query.isNotEmpty()) filtered = filtered.filter {
            it.feed.name.contains(query, ignoreCase = true)
        }
        // Seçililer önce
        filteredEntries = filtered.sortedWith(
            compareByDescending<PoolEntry> { it.selected }.thenBy { it.feed.name }
        )
        poolAdapter.update(filteredEntries.toMutableList())
    }

    /**
     * Akıllı seçim: SmartFeedSelector algoritmasını kullanarak en uygun yemleri seç
     */
    private fun smartSelect() {
        // Clear previous selections
        poolEntries.forEach { it.selected = false }

        // Build a minimal AnimalProfile from species
        val animalProfile = animalSpecies?.let { AnimalProfile(species = it) } ?: return

        // Disable smart button and show progress while running
        binding.btnSelectSmart.isEnabled = false
        binding.btnSelectSmart.alpha = 0.5f
        binding.progressSmart.visibility = View.VISIBLE
 
        val categoryCaps = linkedMapOf(
            FeedCategories.ROUGHAGE_WET to 2,
            FeedCategories.ROUGHAGE_DRY to 2,
            FeedCategories.GRAIN to 3,
            FeedCategories.PROTEIN to 2,
            FeedCategories.BYPRODUCT to 2,
            FeedCategories.FAT to 1,
            FeedCategories.MINERAL to 1,
            FeedCategories.PREMIKS to 1,
            FeedCategories.VITAMIN to 1,
            FeedCategories.ADDITIVE to 0
        )
        val allowedCategories = poolEntries.map { it.feed.category }.toSet()

        lifecycleScope.launch {
            try {
                val feedsToConsider = poolEntries.map { it.feed }
                val validCount = feedsToConsider.count { it.dm > 0.0 }
                if (validCount < 2) {
                    Snackbar.make(binding.root, "Akıllı seçim için en az 2 geçerli yem olmalı (dm bilgisi gereklidir).", Snackbar.LENGTH_LONG).show()
                    return@launch
                }

                val chosen = withContext(Dispatchers.Default) {
                    SmartFeedSelector.buildCandidatePool(
                        animal = animalProfile,
                        allFeeds = feedsToConsider,
                        config = SmartFeedSelector.SelectionConfig(
                            limit = 15,
                            attempt = 0,
                            allowedCategories = allowedCategories,
                            requiredCategories = emptySet(),
                            categoryCaps = categoryCaps
                        )
                    )
                }

                poolEntries.forEach { entry ->
                    entry.selected = chosen.any { it.id == entry.feed.id }
                }
                filterFeeds(binding.etSearch.text?.toString() ?: "")
                updateCount()
                android.widget.Toast.makeText(requireContext(), "${chosen.size} yem otomatik seçildi", android.widget.Toast.LENGTH_SHORT).show()
                // Eğer fallback kullanıldıysa kullanıcıya bilgi ver
                if (chosen.isNotEmpty() && chosen.size < poolEntries.size) {
                    Snackbar.make(binding.root, "HiGHS ile seçim yapıldı.", Snackbar.LENGTH_LONG).show()
                }
                // If auto-apply is enabled, apply selection automatically
                if (binding.switchAutoApply.isChecked) {
                    if (chosen.isNotEmpty()) {
                        applySelection()
                    } else {
                        android.widget.Toast.makeText(requireContext(), "Otomatik uygulama için seçili yem yok!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                // Log the exception for debugging and show a readable message to the user
                android.util.Log.e("FeedPoolFragment", "Smart selection failed", e)
                val msg = e.message ?: e.toString()
                Snackbar.make(binding.root, "Akıllı seçim sırasında hata: $msg", Snackbar.LENGTH_LONG).show()
            } finally {
                // Re-enable smart button and hide progress
                binding.btnSelectSmart.isEnabled = true
                binding.btnSelectSmart.alpha = 1f
                binding.progressSmart.visibility = View.GONE
            }
        }
     }

    private fun updateCount() {
        val count = poolEntries.count { it.selected }
        binding.tvPoolCount.text = "$count yem seçildi"
        binding.tvPoolCount.setTextColor(
            if (count >= 5) Color.parseColor("#00FF88") else Color.parseColor("#FFC400")
        )
    }

    private fun applySelection() {
        val selected = poolEntries.filter { it.selected }
        if (selected.isEmpty()) {
            android.widget.Toast.makeText(requireContext(),
                "En az 1 yem seçin!", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        // FragmentResult ile RationFragment'a gönder
        val feedIds   = selected.map { it.feed.id }.toIntArray()
        val minValues = selected.map { it.minKg }.toDoubleArray()
        val maxValues = selected.map { it.maxKg }.toDoubleArray()

        parentFragmentManager.setFragmentResult(
            RESULT_KEY,
            bundleOf(
                "feed_ids"   to feedIds,
                "feed_mins"  to minValues,
                "feed_maxes" to maxValues
            )
        )
        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ── Pool Adapter ─────────────────────────────────────────────
class PoolAdapter(
    private var entries: MutableList<FeedPoolFragment.PoolEntry>,
    private val onToggle: (FeedPoolFragment.PoolEntry) -> Unit
) : RecyclerView.Adapter<PoolAdapter.PVH>() {

    inner class PVH(val b: ItemFeedPoolCardBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, vt: Int): PVH {
        val b = ItemFeedPoolCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return PVH(b)
    }

    override fun onBindViewHolder(h: PVH, pos: Int) {
        val entry = entries[pos]
        val feed  = entry.feed
        val b = h.b

        // Checkbox rengi ve ikonu
        b.tvCheckbox.text = if (entry.selected) "✅" else "☐"
        b.tvCheckbox.setTextColor(
            if (entry.selected) Color.parseColor("#00FF88")
            else Color.parseColor("#3A5A48"))

        b.tvPoolFeedName.text = feed.name
        b.tvPoolCategory.text = FeedCategories.displayName(feed.category)
        b.tvPoolKm.text       = "KM:${feed.dm.toInt()}%"

        b.tvPoolMe.text  = if (feed.me  > 0) "ME:${feed.me}" else ""
        b.tvPoolNel.text = if (feed.nel > 0) "NEL:${feed.nel}" else ""
        b.tvPoolCp.text  = if (feed.cp  > 0) "HP:${feed.cp}%" else ""
        b.tvPoolPrice.text = if (feed.pricePerKg > 0) "₺${feed.pricePerKg}/kg" else ""

        // Min/max alan görünürlüğü
        b.layoutMinMax.visibility = if (entry.selected) View.VISIBLE else View.GONE

        if (entry.selected) {
            if (entry.minKg > 0) b.etFeedMin.setText(entry.minKg.toString())
            if (entry.maxKg > 0) b.etFeedMax.setText(entry.maxKg.toString())

            b.etFeedMin.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    entry.minKg = b.etFeedMin.text.toString().toDoubleOrNull() ?: 0.0
                }
            }
            b.etFeedMax.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    entry.maxKg = b.etFeedMax.text.toString().toDoubleOrNull() ?: -1.0
                }
            }
        }

        // Kategori rengi
        try {
            val color = FeedCategories.color(feed.category)
            b.tvPoolFeedName.setTextColor(
                if (entry.selected) Color.parseColor(color) else Color.parseColor("#ffffffdd"))
        } catch (_: Exception) {}

        b.root.setOnClickListener { onToggle(entry) }
        b.tvCheckbox.setOnClickListener { onToggle(entry) }
    }

    override fun getItemCount() = entries.size

    fun update(list: MutableList<FeedPoolFragment.PoolEntry>) {
        entries = list; notifyDataSetChanged()
    }
}