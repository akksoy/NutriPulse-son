package com.nutripulse.app.ui.feed

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nutripulse.app.R
import com.nutripulse.app.data.AppDatabase
import com.nutripulse.app.data.TmoFetcher
import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories
import com.nutripulse.app.databinding.FragmentPriceUpdateBinding
import com.nutripulse.app.databinding.ItemPriceRowBinding
import com.nutripulse.app.utils.PriceFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PriceUpdateFragment : Fragment() {

    private var _binding: FragmentPriceUpdateBinding? = null
    private val binding get() = _binding!!

    private var allFeeds: List<Feed> = emptyList()
    private var activeCategory = "ALL"
    private var isAutoMode = false
    private lateinit var priceAdapter: PriceAdapter

    // Baslangicta Mart 2026 guncel fiyatlari kullan
    private var currentRefPrices = TmoFetcher.getCurrent2026Prices()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPriceUpdateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val today = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        binding.tvLastUpdate.text = "Son guncelleme: $today"

        setupRecycler()
        setupModeButtons()
        setupCategoryTabs()
        loadFeeds()

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnSaveAll.setOnClickListener { saveAllPrices() }
    }

    private fun setupModeButtons() {
        setManualModeUI()

        binding.btnManualMode.setOnClickListener {
            isAutoMode = false
            setManualModeUI()
            binding.tvAutoStatus.visibility = View.GONE
            applyPriceSuggestions(showToast = true)
        }

        binding.btnAutoMode.setOnClickListener {
            isAutoMode = true
            setAutoModeUI()
            binding.tvAutoStatus.visibility = View.VISIBLE
            binding.tvAutoStatus.text = "7 Ticaret Borsasindan fiyatlar cekiliyor...\nIzmir • Ankara • Adana • Konya • TMO • KOBIK • Tarim Bakanligi"
            binding.tvAutoStatus.setTextColor(Color.parseColor("#FFC400"))

            lifecycleScope.launch {
                val result = TmoFetcher.fetchPrices()

                withContext(Dispatchers.Main) {
                    val merged = currentRefPrices.toMutableMap()
                    merged.putAll(result.prices)
                    currentRefPrices = merged

                    applyPriceSuggestions(showToast = false)

                    val suggestedForAllFeeds = buildSuggestions(allFeeds, currentRefPrices)
                        .values
                        .count { it > 0.0 }

                    if (result.success) {
                        binding.tvAutoStatus.text =
                            "${result.source}\n${result.message}\n" +
                            "Referans fiyat: ${currentRefPrices.size} kayit\n" +
                            "Oneri olusan yem: $suggestedForAllFeeds/${allFeeds.size}"
                        binding.tvAutoStatus.setTextColor(Color.parseColor("#00FF88"))
                    } else {
                        binding.tvAutoStatus.text =
                            "Mart 2026 referans fiyatlari yuklendi (${currentRefPrices.size} kayit)\n" +
                            "Oneri olusan yem: $suggestedForAllFeeds/${allFeeds.size}\n" +
                            "Internet geldiginde fiyatlar tekrar otomatik cekilir."
                        binding.tvAutoStatus.setTextColor(Color.parseColor("#FFC400"))
                    }
                }
            }
        }
    }

    private fun setManualModeUI() {
        binding.btnManualMode.setBackgroundResource(R.drawable.bg_filter_active)
        (binding.btnManualMode.getChildAt(0) as TextView).setTextColor(Color.parseColor("#05080A"))
        binding.btnAutoMode.setBackgroundResource(R.drawable.bg_filter_inactive)
        (binding.btnAutoMode.getChildAt(0) as TextView).setTextColor(Color.parseColor("#00FF88"))
    }

    private fun setAutoModeUI() {
        binding.btnAutoMode.setBackgroundResource(R.drawable.bg_filter_active)
        (binding.btnAutoMode.getChildAt(0) as TextView).setTextColor(Color.parseColor("#05080A"))
        binding.btnManualMode.setBackgroundResource(R.drawable.bg_filter_inactive)
        (binding.btnManualMode.getChildAt(0) as TextView).setTextColor(Color.parseColor("#00FF88"))
    }

    private fun setupCategoryTabs() {
        val cats = mutableListOf("TÜMÜ" to "ALL")
        FeedCategories.ALL.forEach { code ->
            cats.add(FeedCategories.displayName(code).take(8) to code)
        }
        val tabViews = mutableListOf<TextView>()
        cats.forEachIndexed { idx, (label, code) ->
            val tv = TextView(requireContext()).apply {
                text = label; textSize = 9f
                val lp = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                lp.marginEnd = 6; layoutParams = lp; setPadding(20, 10, 20, 10)
                if (idx == 0) { setBackgroundResource(R.drawable.bg_filter_active); setTextColor(Color.parseColor("#05080A")) }
                else { setBackgroundResource(R.drawable.bg_filter_inactive); setTextColor(Color.parseColor("#00FF88")) }
                setOnClickListener {
                    activeCategory = code
                    tabViews.forEach { v -> v.setBackgroundResource(R.drawable.bg_filter_inactive); v.setTextColor(Color.parseColor("#00FF88")) }
                    setBackgroundResource(R.drawable.bg_filter_active); setTextColor(Color.parseColor("#05080A"))
                    filterAndShow()
                }
            }
            tabViews.add(tv)
            binding.categoryTabs.addView(tv)
        }
    }

    private fun setupRecycler() {
        priceAdapter = PriceAdapter(emptyList(), currentRefPrices)
        binding.recyclerPrices.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPrices.adapter = priceAdapter
    }

    private fun loadFeeds() {
        AppDatabase.getDatabase(requireContext()).feedDao().getAllFeeds()
            .observe(viewLifecycleOwner) { feeds ->
                allFeeds = feeds
                filterAndShow()
                // Liste her yenilendiginde referanslari tekrar uygula ki eksikler kapanabilsin.
                applyPriceSuggestions(showToast = false)
            }
    }

    private fun filterAndShow() {
        val filtered = if (activeCategory == "ALL") allFeeds
                       else allFeeds.filter { it.category == activeCategory }
        priceAdapter.updateFeeds(filtered)
    }

    private fun applyPriceSuggestions(showToast: Boolean) {
        if (!::priceAdapter.isInitialized || allFeeds.isEmpty()) return

        val suggestions = buildSuggestions(allFeeds, currentRefPrices)
        priceAdapter.setReferencePrices(currentRefPrices)
        priceAdapter.setEditedPrices(suggestions)

        if (showToast) {
            val missingAfter = allFeeds.count { feed ->
                val current = suggestions[feed.id] ?: feed.pricePerKg
                current <= 0.0
            }
            val filled = allFeeds.size - missingAfter
            Toast.makeText(
                requireContext(),
                "Referans fiyatlar uygulandi: $filled/${allFeeds.size}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun buildSuggestions(feeds: List<Feed>, refs: Map<String, Double>): Map<Int, Double> {
        val normalizedRefs = refs.entries.associate { normalizeName(it.key) to it.value }
        val byCategory = mutableMapOf<String, MutableList<Double>>()

        fun findRef(name: String): Double? {
            return refs[name] ?: normalizedRefs[normalizeName(name)]
        }

        feeds.forEach { feed ->
            val p = findRef(feed.name)
            if (p != null && p > 0.0) {
                byCategory.getOrPut(feed.category) { mutableListOf() }.add(p)
            }
        }

        val categoryAvg = byCategory.mapValues { (_, prices) -> prices.average() }
        val globalAvg = refs.values.filter { it > 0.0 }.average().takeIf { !it.isNaN() }

        return feeds.associate { feed ->
            val existing = feed.pricePerKg
            val suggested = when {
                existing > 0.0 -> existing
                else -> findRef(feed.name)
                    ?: categoryAvg[feed.category]
                    ?: globalAvg
                    ?: 0.0
            }
            feed.id to suggested
        }
    }

    private fun normalizeName(raw: String): String {
        return raw.lowercase(Locale.ROOT)
            .replace("ı", "i")
            .replace("ğ", "g")
            .replace("ş", "s")
            .replace("ç", "c")
            .replace("ö", "o")
            .replace("ü", "u")
            .replace("â", "a")
            .replace("î", "i")
            .replace("û", "u")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
    }

    private fun saveAllPrices() {
        val prices = priceAdapter.getEditedPrices()
        if (prices.isEmpty()) {
            Toast.makeText(requireContext(), "Degisiklik yok", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(requireContext())
                prices.forEach { (id, price) -> 
                    if (price > 0.0 && PriceFormatter.isValidPrice(price)) {
                        // Fiyatı 1 ondalık basamağa yuvarla
                        val rounded = kotlin.math.round(price * 10) / 10
                        db.feedDao().updatePrice(id, rounded)
                    }
                }
            }
            Toast.makeText(requireContext(), "✅ ${prices.size} yem fiyatı kaydedildi!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ──── PriceAdapter ────────────────────────────────────────────
class PriceAdapter(
    private var feeds: List<Feed>,
    private var refPrices: Map<String, Double>
) : RecyclerView.Adapter<PriceAdapter.PriceVH>() {

    private val edited = mutableMapOf<Int, Double>()

    inner class PriceVH(val b: ItemPriceRowBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PriceVH {
        val b = ItemPriceRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PriceVH(b)
    }

    override fun onBindViewHolder(holder: PriceVH, position: Int) {
        val feed = feeds[position]
        holder.b.tvPriceFeedName.text = feed.name
        try { holder.b.viewColor.setBackgroundColor(Color.parseColor(FeedCategories.color(feed.category))) } catch (_: Exception) {}

        val ref = findRef(feed.name)
        holder.b.tvRefPrice.text = if (ref != null) "ref: ₺${PriceFormatter.formatPrice(ref)}" else "ref: —"

        val current = edited[feed.id] ?: if (feed.pricePerKg > 0) feed.pricePerKg else ref
        if (current != null) {
            // Formatlı değeri göster ama input'a normal sayıyı koy
            holder.b.etPrice.setText(PriceFormatter.formatPrice(current))
        }
        else holder.b.etPrice.text = null

        holder.b.etPrice.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val inputPrice = PriceFormatter.parsePrice(holder.b.etPrice.text.toString())
                if (inputPrice > 0 && PriceFormatter.isValidPrice(inputPrice)) {
                    edited[feed.id] = inputPrice
                    // Düzeltilmiş değeri göster
                    holder.b.etPrice.setText(PriceFormatter.formatPrice(inputPrice))
                }
            }
        }
    }

    override fun getItemCount() = feeds.size

    fun updateFeeds(f: List<Feed>) { feeds = f; notifyDataSetChanged() }

    fun setReferencePrices(p: Map<String, Double>) {
        refPrices = p
        notifyDataSetChanged()
    }

    fun setEditedPrices(prices: Map<Int, Double>) {
        edited.clear()
        edited.putAll(prices.filterValues { it > 0.0 })
        notifyDataSetChanged()
    }

    fun getEditedPrices(): Map<Int, Double> = edited.toMap()

    private fun findRef(name: String): Double? {
        refPrices[name]?.let { return it }
        val normalizedTarget = normalizeName(name)
        return refPrices.entries.firstOrNull { normalizeName(it.key) == normalizedTarget }?.value
    }

    private fun normalizeName(raw: String): String {
        return raw.lowercase(Locale.ROOT)
            .replace("ı", "i")
            .replace("ğ", "g")
            .replace("ş", "s")
            .replace("ç", "c")
            .replace("ö", "o")
            .replace("ü", "u")
            .replace("â", "a")
            .replace("î", "i")
            .replace("û", "u")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
    }
}