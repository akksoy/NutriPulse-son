package com.nutripulse.app.ui.feed

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.nutripulse.app.R
import com.nutripulse.app.data.AppDatabase
import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories
import com.nutripulse.app.databinding.FragmentFeedBinding

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: FeedAdapter
    private var allFeeds: List<Feed> = emptyList()
    private var activeCategory: String = "ALL"

    // Tüm filtre TextView'leri
    private val filterViews = mutableListOf<TextView>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        setupSearch()
        buildCategoryFilters()
        observeFeeds()

        // + YEM EKLE
        binding.btnAddFeed.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddEditFeedFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        // FİYAT YÖNETİMİ
        binding.btnPriceManage.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PriceUpdateFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupRecycler() {
        adapter = FeedAdapter(
            feeds = emptyList(),
            onFeedClick = { feed ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, FeedDetailFragment.newInstance(feed.id))
                    .addToBackStack(null)
                    .commit()
            }
        )
        binding.recyclerFeeds.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFeeds.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { filterFeeds(s?.toString() ?: "") }
        })
    }

    // Kategori filtrelerini dinamik oluştur — VITAMIN, PREMIKS dahil
    private fun buildCategoryFilters() {
        val categories = mutableListOf<Pair<String, String>>()
        categories.add("TÜMÜ" to "ALL")
        categories.add("SULU KABA" to FeedCategories.ROUGHAGE_WET)
        categories.add("KURU KABA" to FeedCategories.ROUGHAGE_DRY)
        categories.add("TAHIL" to FeedCategories.GRAIN)
        categories.add("PROTEİN" to FeedCategories.PROTEIN)
        categories.add("YAN ÜRÜN" to FeedCategories.BYPRODUCT)
        categories.add("YAĞ" to FeedCategories.FAT)
        categories.add("MİNERAL" to FeedCategories.MINERAL)
        categories.add("VİTAMİN" to FeedCategories.VITAMIN)
        categories.add("PREMİKS" to FeedCategories.PREMIKS)
        categories.add("KATKI" to FeedCategories.ADDITIVE)
        categories.add("SU ÜRN." to FeedCategories.AQUA)

        categories.forEachIndexed { index, (label, code) ->
            val tv = TextView(requireContext()).apply {
                text = label
                textSize = 10f
                val lp = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.marginEnd = 6
                layoutParams = lp
                setPadding(24, 14, 24, 14)

                if (index == 0) {
                    setBackgroundResource(R.drawable.bg_filter_active)
                    setTextColor(Color.parseColor("#05080A"))
                } else {
                    setBackgroundResource(R.drawable.bg_filter_inactive)
                    setTextColor(Color.parseColor("#00FF88"))
                }

                setOnClickListener {
                    activeCategory = code
                    // Tüm filtreleri pasif yap
                    filterViews.forEach { v ->
                        v.setBackgroundResource(R.drawable.bg_filter_inactive)
                        v.setTextColor(Color.parseColor("#00FF88"))
                    }
                    // Bu filtreyi aktif yap
                    setBackgroundResource(R.drawable.bg_filter_active)
                    setTextColor(Color.parseColor("#05080A"))
                    filterFeeds(binding.etSearch.text?.toString() ?: "")
                }
            }
            filterViews.add(tv)
            binding.categoryFilterBar.addView(tv)
        }
    }

    private fun observeFeeds() {
        AppDatabase.getDatabase(requireContext()).feedDao().getAllFeeds()
            .observe(viewLifecycleOwner) { feeds ->
                allFeeds = feeds
                binding.tvFeedCount.text = "${feeds.size} yem kayıtlı"
                filterFeeds(binding.etSearch.text?.toString() ?: "")
            }
    }

    private fun filterFeeds(query: String) {
        var filtered = allFeeds
        if (activeCategory != "ALL") {
            filtered = filtered.filter { it.category == activeCategory }
        }
        if (query.isNotEmpty()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
        }
        adapter.updateFeeds(filtered)
        binding.recyclerFeeds.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        binding.layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}