package com.nutripulse.app.ui.report

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nutripulse.app.data.report.ReportEngine
import com.nutripulse.app.databinding.ItemReportRationRowBinding
import com.nutripulse.app.databinding.ItemReportStockRowBinding
import com.nutripulse.app.databinding.ItemReportCostRowBinding
import com.nutripulse.app.databinding.ItemReportProfitRowBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Rasyon raporu adapter ─────────────────────────────────────
class RationReportAdapter(private var rows: List<ReportEngine.RationReportRow>) :
    RecyclerView.Adapter<RationReportAdapter.VH>() {

    inner class VH(val b: ItemReportRationRowBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, vt: Int): VH {
        val b = ItemReportRationRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val r = rows[pos]
        h.b.tvRowRationName.text = r.rationName
        h.b.tvRowAnimalCat.text = "${r.animalCount} baş  •  ${r.animalCategory}"
        h.b.tvRowCost.text = "₺${fmt2(r.costPerDay)}"
        h.b.tvRowProfit.text = "₺${fmt2(r.netProfit)}"
        h.b.tvRowProfit.setTextColor(Color.parseColor(if (r.netProfit >= 0) "#00FF88" else "#FF5A5A"))
        h.b.tvRowCoverage.text = "%${fmt1(r.coverage)}"
        h.b.tvRowCoverage.setTextColor(Color.parseColor(
            when { r.coverage >= 98 -> "#00FF88"; r.coverage >= 85 -> "#FFC400"; else -> "#FF7A00" }
        ))
        h.b.tvRowYield.text = if (r.estimatedYield > 0) fmt1(r.estimatedYield) else "—"
        h.b.tvRowDate.text = SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(Date(r.createdAt))
    }

    override fun getItemCount() = rows.size
    fun update(list: List<ReportEngine.RationReportRow>) { rows = list; notifyDataSetChanged() }
    private fun fmt1(v: Double) = String.format("%.1f", v)
    private fun fmt2(v: Double) = String.format("%.2f", v)
}

// ── Stok raporu adapter ───────────────────────────────────────
class StockReportAdapter(private var rows: List<ReportEngine.StockReportRow>) :
    RecyclerView.Adapter<StockReportAdapter.VH>() {

    inner class VH(val b: ItemReportStockRowBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, vt: Int): VH {
        val b = ItemReportStockRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val r = rows[pos]
        val (color, badge) = when (r.status) {
            "EMPTY"    -> "#FF5A5A" to "❌ BİTTİ"
            "CRITICAL" -> "#FF7A00" to "⚠ KRİTİK"
            "LOW"      -> "#FFC400" to "↓ DÜŞÜK"
            else       -> "#00FF88" to "✅ NORMAL"
        }
        h.b.viewStockStatus.setBackgroundColor(Color.parseColor(color))
        h.b.tvReportFeedName.text = r.feedName
        h.b.tvReportStatusBadge.text = badge
        h.b.tvReportStatusBadge.setTextColor(Color.parseColor(color))
        h.b.tvReportStock.text = "${fmt1(r.currentKg)} kg"
        h.b.tvReportStock.setTextColor(Color.parseColor(color))
        h.b.tvReportMinStock.text = if (r.minKg > 0) "min: ${fmt1(r.minKg)} kg" else ""
        h.b.tvReportDays.text = if (r.daysLeft > 0) "${r.daysLeft} gün" else "—"
        h.b.tvReportDays.setTextColor(Color.parseColor(
            when { r.daysLeft in 1..7 -> "#FF7A00"; r.daysLeft in 8..14 -> "#FFC400"; else -> "#00D4FF" }
        ))
        h.b.tvReportValue.text = "₺${fmt0(r.stockValue)}"
    }

    override fun getItemCount() = rows.size
    fun update(list: List<ReportEngine.StockReportRow>) { rows = list; notifyDataSetChanged() }
    private fun fmt0(v: Double) = String.format("%.0f", v)
    private fun fmt1(v: Double) = String.format("%.1f", v)
}

// ── Maliyet adapter ───────────────────────────────────────────
class CostReportAdapter(private var rows: List<ReportEngine.FeedCostRow>) :
    RecyclerView.Adapter<CostReportAdapter.VH>() {

    inner class VH(val b: ItemReportCostRowBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, vt: Int): VH {
        val b = ItemReportCostRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val r = rows[pos]
        h.b.tvCostFeedName.text = r.feedName
        h.b.tvCostPriceKg.text = "₺${String.format("%.2f",r.pricePerKg)}/kg  •  ${String.format("%.0f",r.stockKg)} kg stok"
        h.b.tvCostTotal.text = "₺${fmt0(r.totalValue)}"
        h.b.tvCostPct.text = "%${String.format("%.1f",r.pctOfTotal)}"
        h.b.progressCost.progress = r.pctOfTotal.toInt().coerceIn(0,100)

        // Renk: büyük harcamalar kırmızımsı
        val color = when {
            r.pctOfTotal >= 30 -> "#FF7A00"
            r.pctOfTotal >= 15 -> "#FFC400"
            else -> "#00FF88"
        }
        h.b.viewCostColor.setBackgroundColor(Color.parseColor(color))
        h.b.progressCost.progressTintList =
            ColorStateList.valueOf(Color.parseColor(color))
    }

    override fun getItemCount() = rows.size
    fun update(list: List<ReportEngine.FeedCostRow>) { rows = list; notifyDataSetChanged() }
    private fun fmt0(v: Double) = String.format("%.0f", v)
}

// ── Karlılık adapter ──────────────────────────────────────────
class ProfitReportAdapter(private var rows: List<ReportEngine.ProfitabilityRow>) :
    RecyclerView.Adapter<ProfitReportAdapter.VH>() {

    inner class VH(val b: ItemReportProfitRowBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, vt: Int): VH {
        val b = ItemReportProfitRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val r = rows[pos]
        h.b.tvProfRationName.text = r.rationName
        h.b.tvProfAnimalCount.text = "${r.animalCount} baş"
        h.b.tvProfROI.text = "ROI %${String.format("%.0f",r.roi)}"
        h.b.tvProfROI.setTextColor(Color.parseColor(if (r.roi >= 0) "#00FF88" else "#FF5A5A"))
        h.b.tvProfCost.text = "₺${fmt2(r.rationCost)}"
        h.b.tvProfRevenue.text = "₺${fmt2(r.estimatedRevenue)}"
        h.b.tvProfNet.text = "₺${fmt2(r.netProfit)}"
        h.b.tvProfNet.setTextColor(Color.parseColor(if (r.netProfit >= 0) "#00FF88" else "#FF5A5A"))
        h.b.tvProfMonthly.text = "₺${String.format("%.0f", r.monthlyNet)}"
    }

    override fun getItemCount() = rows.size
    fun update(list: List<ReportEngine.ProfitabilityRow>) { rows = list; notifyDataSetChanged() }
    private fun fmt2(v: Double) = String.format("%.2f", v)
}
