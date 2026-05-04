package com.nutripulse.app.ui.report

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nutripulse.app.data.AppDatabase
import com.nutripulse.app.data.report.ReportEngine
import com.nutripulse.app.databinding.FragmentReportBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!

    private var activeTab = 0

    // Adapterler
    private lateinit var rationAdapter: RationReportAdapter
    private lateinit var stockAdapter: StockReportAdapter
    private lateinit var costAdapter: CostReportAdapter
    private lateinit var profitAdapter: ProfitReportAdapter

    private val tabs = listOf(
        "🏠 Özet" to 0,
        "🐄 Rasyon" to 1,
        "📦 Stok" to 2,
        "💰 Maliyet" to 3,
        "📈 Karlılık" to 4
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale("tr")).format(Date())
        binding.tvReportDate.text = dateStr

        setupTabs()
        setupRecyclers()
        loadAllData()

        binding.btnExportPdf.setOnClickListener {
            Toast.makeText(requireContext(),
                "PDF dışa aktarma yakında eklenecek!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTabs() {
        val tabViews = mutableListOf<TextView>()
        tabs.forEachIndexed { idx, (label, _) ->
            val tv = TextView(requireContext()).apply {
                text = label; textSize = 10f
                val lp = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                lp.marginEnd = 6; layoutParams = lp; setPadding(20, 12, 20, 12)
                if (idx == 0) { setBackgroundResource(com.nutripulse.app.R.drawable.bg_filter_active); setTextColor(Color.parseColor("#05080A")) }
                else { setBackgroundResource(com.nutripulse.app.R.drawable.bg_filter_inactive); setTextColor(Color.parseColor("#00D4FF")) }

                setOnClickListener {
                    activeTab = idx
                    tabViews.forEach { v ->
                        v.setBackgroundResource(com.nutripulse.app.R.drawable.bg_filter_inactive)
                        v.setTextColor(Color.parseColor("#00D4FF"))
                    }
                    setBackgroundResource(com.nutripulse.app.R.drawable.bg_filter_active)
                    setTextColor(Color.parseColor("#05080A"))
                    showSection(idx)
                }
            }
            tabViews.add(tv)
            binding.reportTabBar.addView(tv)
        }
    }

    private fun showSection(tab: Int) {
        binding.sectionDashboard.visibility = if (tab == 0) View.VISIBLE else View.GONE
        binding.sectionRation.visibility    = if (tab == 1) View.VISIBLE else View.GONE
        binding.sectionStock.visibility     = if (tab == 2) View.VISIBLE else View.GONE
        binding.sectionCost.visibility      = if (tab == 3) View.VISIBLE else View.GONE
    }

    private fun setupRecyclers() {
        rationAdapter = RationReportAdapter(emptyList())
        binding.recyclerRationReport.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRationReport.adapter = rationAdapter

        stockAdapter = StockReportAdapter(emptyList())
        binding.recyclerStockReport.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerStockReport.adapter = stockAdapter

        costAdapter = CostReportAdapter(emptyList())
        profitAdapter = ProfitReportAdapter(emptyList())
    }

    private fun loadAllData() {
        val db = AppDatabase.getDatabase(requireContext())

        lifecycleScope.launch {
            // Dashboard
            val dashboard = withContext(Dispatchers.IO) {
                ReportEngine.getDashboard(
                    db.feedDao(), db.rationDao(), db.stockDao(), db.animalProfileDao()
                )
            }
            bindDashboard(dashboard)

            // Rasyon raporu
            val rationRows = withContext(Dispatchers.IO) {
                ReportEngine.getRationReport(db.rationDao())
            }
            rationAdapter.update(rationRows)

            // Stok raporu
            val stockRows = withContext(Dispatchers.IO) {
                ReportEngine.getStockReport(db.stockDao())
            }
            stockAdapter.update(stockRows)
            bindStockSummary(stockRows)

            // Maliyet raporu
            val costRows = withContext(Dispatchers.IO) {
                ReportEngine.getFeedCostReport(db.feedDao(), db.stockDao())
            }
            costAdapter.update(costRows)
            buildCostBarChart(costRows.take(6))

            // Karlılık raporu
            val profitRows = withContext(Dispatchers.IO) {
                ReportEngine.getProfitabilityReport(db.rationDao())
            }
            profitAdapter.update(profitRows)
        }
    }

    private fun bindDashboard(d: ReportEngine.DashboardData) {
        binding.kpiRations.text    = d.totalRations.toString()
        binding.kpiAnimals.text    = d.totalAnimals.toString()
        binding.kpiStockValue.text = "₺${fmtK(d.totalStockValue)}"
        binding.kpiCritical.text   = d.criticalStockCount.toString()
        binding.kpiCritical.setTextColor(
            if (d.criticalStockCount > 0) Color.parseColor("#FF7A00")
            else Color.parseColor("#00FF88")
        )
        binding.tvBestRation.text  = d.bestRationName
        binding.tvBestProfit.text  = "₺${fmt2(d.bestRationProfit)}"
        binding.tvAvgCost.text     = "₺${fmt2(d.avgRationCost)}/inek/gün"
        binding.tvAvgCoverage.text = "%${fmt1(d.avgCoverage)}"
        binding.tvMonthlyBuy.text  = "₺${fmtK(d.monthlyFeedCost)}"
    }

    private fun bindStockSummary(rows: List<ReportEngine.StockReportRow>) {
        binding.tvStockNormal.text   = rows.count { it.status == "NORMAL"   }.toString()
        binding.tvStockLow.text      = rows.count { it.status == "LOW"      }.toString()
        binding.tvStockCritical.text = rows.count { it.status == "CRITICAL" }.toString()
        binding.tvStockEmpty.text    = rows.count { it.status == "EMPTY"    }.toString()
    }

    private fun buildCostBarChart(rows: List<ReportEngine.FeedCostRow>) {
        binding.costBarChart.removeAllViews()
        val maxVal = rows.maxOfOrNull { it.pctOfTotal } ?: 1.0

        rows.forEach { r ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 4, 0, 4)
            }

            // Yem adı
            val tvName = TextView(requireContext()).apply {
                text = r.feedName.take(14)
                textSize = 9f
                setTextColor(Color.parseColor("#C0E8D0"))
                width = 240
            }
            row.addView(tvName)

            // Bar
            val bar = View(requireContext()).apply {
                val pct = (r.pctOfTotal / maxVal * 200).toInt()
                layoutParams = LinearLayout.LayoutParams(pct.coerceAtLeast(8), 18)
                val color = when {
                    r.pctOfTotal >= 30 -> "#FF7A00"
                    r.pctOfTotal >= 15 -> "#FFC400"
                    else -> "#00FF88"
                }
                setBackgroundColor(Color.parseColor(color))
            }
            row.addView(bar)

            // Değer
            val tvVal = TextView(requireContext()).apply {
                text = " ₺${fmtK(r.totalValue)} (%${fmt1(r.pctOfTotal)})"
                textSize = 8f
                setTextColor(Color.parseColor("#7ABFA0"))
            }
            row.addView(tvVal)

            binding.costBarChart.addView(row)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun fmtK(v: Double) =
        if (v >= 1000) "${String.format("%.1f", v/1000)}K"
        else String.format("%.0f", v)

    private fun fmt1(v: Double) = String.format("%.1f", v)
    private fun fmt2(v: Double) = String.format("%.2f", v)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
