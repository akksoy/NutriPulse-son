package com.nutripulse.app.ui.stock

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
import com.nutripulse.app.data.model.FeedCategories
import com.nutripulse.app.data.model.StockItem
import com.nutripulse.app.data.model.TransactionType
import com.nutripulse.app.databinding.FragmentStockBinding
import java.text.NumberFormat
import java.util.Locale

class StockFragment : Fragment() {

    private var _binding: FragmentStockBinding? = null
    private val binding get() = _binding!!

    private lateinit var stockAdapter: StockAdapter
    private var allItems: List<StockItem> = emptyList()
    private var activeFilter = "ALL"
    private val filterViews = mutableListOf<TextView>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecycler()
        buildFilters()
        setupSearch()
        observeData()

        binding.btnAddStock.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, StockAddFragment.newInstance())
                .addToBackStack(null).commit()
        }
    }

    private fun setupRecycler() {
        stockAdapter = StockAdapter(
            emptyList(),
            onDetail = { item ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, StockAddFragment.newInstance(item.id))
                    .addToBackStack(null).commit()
            },
            onQuickIn = { item ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer,
                        StockTransactionFragment.newInstance(item.id, TransactionType.PURCHASE))
                    .addToBackStack(null).commit()
            },
            onQuickOut = { item ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer,
                        StockTransactionFragment.newInstance(item.id, TransactionType.USAGE))
                    .addToBackStack(null).commit()
            }
        )
        binding.recyclerStock.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerStock.adapter = stockAdapter
    }

    private fun buildFilters() {
        val filters = mutableListOf("TÜMÜ" to "ALL", "⚠ KRİTİK" to "CRITICAL")
        FeedCategories.ALL.forEach { code ->
            filters.add(FeedCategories.displayName(code).take(7) to code)
        }

        filters.forEachIndexed { idx, (label, code) ->
            val tv = TextView(requireContext()).apply {
                text = label; textSize = 9f
                val lp = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                lp.marginEnd = 6; layoutParams = lp; setPadding(20, 12, 20, 12)

                if (idx == 0) { setBackgroundResource(R.drawable.bg_filter_active); setTextColor(Color.parseColor("#05080A")) }
                else if (code == "CRITICAL") { setBackgroundResource(R.drawable.bg_stat_orange); setTextColor(Color.parseColor("#FF7A00")) }
                else { setBackgroundResource(R.drawable.bg_filter_inactive); setTextColor(Color.parseColor("#00FF88")) }

                setOnClickListener {
                    activeFilter = code
                    filterViews.forEach { v ->
                        v.setBackgroundResource(R.drawable.bg_filter_inactive)
                        v.setTextColor(Color.parseColor("#00FF88"))
                    }
                    setBackgroundResource(R.drawable.bg_filter_active)
                    setTextColor(Color.parseColor("#05080A"))
                    filterItems(binding.etSearch.text?.toString() ?: "")
                }
            }
            filterViews.add(tv)
            binding.filterBar.addView(tv)
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterItems(s?.toString() ?: "")
            }
        })
    }

    private fun observeData() {
        val db = AppDatabase.getDatabase(requireContext())

        db.stockDao().getAllStock().observe(viewLifecycleOwner) { items ->
            allItems = items
            filterItems(binding.etSearch.text?.toString() ?: "")
            updateSummary(items)
        }

        db.stockDao().getCriticalCount().observe(viewLifecycleOwner) { count ->
            binding.tvCriticalCount.text = "$count ürün"
            binding.tvCriticalCount.setTextColor(
                if (count > 0) Color.parseColor("#FF7A00") else Color.parseColor("#00FF88")
            )
        }

        db.stockDao().getCount().observe(viewLifecycleOwner) { count ->
            binding.tvStockCount.text = "$count yem"
        }

        db.stockDao().getTotalStockValue().observe(viewLifecycleOwner) { value ->
            val v = value ?: 0.0
            binding.tvTotalValue.text = "₺${fmtPrice(v)}"
            binding.tvStockSubtitle.text = "Toplam stok değeri ₺${fmtPrice(v)}"
        }
    }

    private fun filterItems(query: String) {
        var filtered = allItems
        when (activeFilter) {
            "CRITICAL" -> filtered = filtered.filter {
                it.minStockKg > 0 && it.currentStockKg < it.minStockKg
            }
            "ALL" -> {}
            else -> filtered = filtered.filter { it.feedCategory == activeFilter }
        }
        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.feedName.contains(query, true) || it.supplierName.contains(query, true)
            }
        }
        // Kritik olanlar başa
        filtered = filtered.sortedWith(
            compareBy<StockItem> { if (it.minStockKg > 0 && it.currentStockKg < it.minStockKg) 0 else 1 }
                .thenBy { it.daysRemaining }
        )
        stockAdapter.update(filtered)
        binding.recyclerStock.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        binding.layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateSummary(items: List<StockItem>) {
        val totalVal = items.sumOf { it.totalStockValue }
        binding.tvTotalValue.text = "₺${fmtPrice(totalVal)}"
    }

    private fun fmtPrice(v: Double) =
        NumberFormat.getNumberInstance(Locale("tr", "TR")).apply {
            maximumFractionDigits = 0
        }.format(v)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
