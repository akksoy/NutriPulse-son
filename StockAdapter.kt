package com.nutripulse.app.ui.stock

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nutripulse.app.data.model.FeedCategories
import com.nutripulse.app.data.model.StockItem
import com.nutripulse.app.databinding.ItemStockCardBinding
import java.text.NumberFormat
import java.util.Locale

class StockAdapter(
    private var items: List<StockItem>,
    private val onDetail: (StockItem) -> Unit,
    private val onQuickIn: (StockItem) -> Unit,
    private val onQuickOut: (StockItem) -> Unit
) : RecyclerView.Adapter<StockAdapter.StockVH>() {

    inner class StockVH(val b: ItemStockCardBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, vt: Int): StockVH {
        val b = ItemStockCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StockVH(b)
    }

    override fun onBindViewHolder(holder: StockVH, pos: Int) {
        val item = items[pos]
        val b = holder.b

        // İsim & kategori
        b.tvStockFeedName.text = item.feedName
        b.tvStockCategory.text = FeedCategories.displayName(item.feedCategory)
        b.tvLocation.text = item.location.ifEmpty { "" }

        // Stok miktarı
        b.tvCurrentStock.text = "${fmt(item.currentStockKg)} kg"
        b.tvStockValue.text = "₺${fmtPrice(item.totalStockValue)}"

        // Stok durumu rengi
        val (statusColor, pct) = stockStatus(item)
        b.viewStatusColor.setBackgroundColor(Color.parseColor(statusColor))
        b.tvCurrentStock.setTextColor(Color.parseColor(statusColor))

        // Progress bar
        b.progressStock.progress = pct.coerceIn(0, 100)
        b.progressStock.progressTintList =
            ColorStateList.valueOf(Color.parseColor(statusColor))

        val pctStr = if (item.maxStockKg > 0) "%$pct" else "—"
        b.tvStockPct.text = pctStr
        b.tvStockPct.setTextColor(Color.parseColor(statusColor))

        // Kalan gün
        if (item.daysRemaining > 0) {
            b.tvDaysLeft.text = "${item.daysRemaining} gün"
            b.tvDaysLeft.setTextColor(
                when {
                    item.daysRemaining <= 7  -> Color.parseColor("#FF7A00")
                    item.daysRemaining <= 14 -> Color.parseColor("#FFC400")
                    else                     -> Color.parseColor("#00D4FF")
                }
            )
        } else {
            b.tvDaysLeft.text = "—"
            b.tvDaysLeft.setTextColor(Color.parseColor("#3A5A48"))
        }

        // Fiyat
        b.tvPriceInfo.text = if (item.avgCostPerKg > 0)
            "₺${fmtPrice(item.avgCostPerKg)}/kg" else "Fiyat girilmedi"

        // Uyarı bandı
        val warn = warningText(item)
        if (warn != null) {
            b.layoutWarning.visibility = android.view.View.VISIBLE
            b.tvWarningText.text = warn
        } else {
            b.layoutWarning.visibility = android.view.View.GONE
        }

        // Tıklamalar
        b.root.setOnClickListener { onDetail(item) }
        b.btnQuickIn.setOnClickListener { onQuickIn(item) }
        b.btnQuickOut.setOnClickListener { onQuickOut(item) }
        b.btnDetail.setOnClickListener { onDetail(item) }
    }

    override fun getItemCount() = items.size

    fun update(list: List<StockItem>) { items = list; notifyDataSetChanged() }

    private fun stockStatus(item: StockItem): Pair<String, Int> {
        val pct = if (item.maxStockKg > 0)
            (item.currentStockKg / item.maxStockKg * 100).toInt()
        else 50

        val color = when {
            item.currentStockKg <= 0               -> "#FF5A5A"   // bitti
            item.minStockKg > 0 &&
            item.currentStockKg < item.minStockKg  -> "#FF7A00"   // kritik
            item.minStockKg > 0 &&
            item.currentStockKg < item.minStockKg*1.5 -> "#FFC400" // düşük
            else                                    -> "#00FF88"   // normal
        }
        return Pair(color, pct)
    }

    private fun warningText(item: StockItem): String? = when {
        item.currentStockKg <= 0 -> "❌ STOK BİTTİ — Acil sipariş gerekli!"
        item.minStockKg > 0 && item.currentStockKg < item.minStockKg ->
            "⚠ KRİTİK: Stok alt sınırın altında (min ${fmt(item.minStockKg)} kg)"
        item.daysRemaining in 1..7 -> "⏰ ${item.daysRemaining} gün sonra bitecek — Sipariş verin"
        else -> null
    }

    private fun fmt(v: Double) =
        if (v >= 100) v.toInt().toString()
        else String.format("%.1f", v)

    private fun fmtPrice(v: Double) =
        NumberFormat.getNumberInstance(Locale("tr","TR")).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }.format(v)
}
