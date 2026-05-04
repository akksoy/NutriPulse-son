package com.nutripulse.app.ui.ration

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nutripulse.app.data.model.FeedCategories
import com.nutripulse.app.data.model.RationItem
import com.nutripulse.app.databinding.ItemRationFeedBinding

class RationFeedAdapter(
    private var items: List<RationItem>,
    private val feedShadowMap: Map<String, Double> = emptyMap()
) : RecyclerView.Adapter<RationFeedAdapter.VH>() {

    inner class VH(val b: ItemRationFeedBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemRationFeedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val b = holder.b
        val shadowValue = feedShadowMap[item.feedName] ?: 0.0
        b.tvRationFeedName.text = item.feedName
        b.tvRationFeedAmount.text = "${formatQty(item.amountKg)} kg"
        b.tvRationFeedAmountDm.text = "KM: ${formatQty(item.amountDmKg)} kg"
        b.tvRationFeedDmPct.text = "Rasyona katki: %${formatPct(item.dmPct)}"
        b.tvRationFeedCost.text = "Maliyet: ₺${String.format("%.2f", item.costPerDay)}"
        b.tvRationFeedShadow.text = "Shadow price: ${formatShadow(shadowValue)}"
        b.tvRationFeedPrice.text = "Fiyat: ₺${String.format("%.2f", item.pricePerKg)}/kg"
        b.tvRationFeedRisk.text = "Risk: ${riskLabel(item.dmPct, shadowValue)}"
        b.etManualAmountKg.visibility = View.GONE
        try {
            b.viewColor.setBackgroundColor(Color.parseColor(FeedCategories.color(item.feedCategory)))
        } catch (_: Exception) {
        }
    }

    override fun getItemCount() = items.size
    fun update(list: List<RationItem>) {
        items = list
        notifyDataSetChanged()
    }

    private fun formatShadow(value: Double): String = if (kotlin.math.abs(value) >= 100) {
        String.format("₺%.1f", value)
    } else {
        String.format("₺%.2f", value)
    }

    private fun formatQty(value: Double): String =
        if (value >= 10.0) String.format("%.1f", value) else String.format("%.2f", value)

    private fun formatPct(value: Double): String =
        if (kotlin.math.abs(value) >= 10.0) String.format("%.1f", value) else String.format("%.2f", value)

    private fun riskLabel(dmPct: Double, shadow: Double): String {
        val score = dmPct * 0.45 + kotlin.math.abs(shadow) * 2.2
        return when {
            score >= 16.0 -> "YUKSEK"
            score >= 8.0 -> "ORTA"
            else -> "DUSUK"
        }
    }
}