package com.nutripulse.app.ui.ration

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nutripulse.app.data.model.FeedCategories
import com.nutripulse.app.databinding.ItemRationFeedBinding

class SelectedFeedAdapter(
    private val feeds: MutableList<SelectedFeedInput>,
    private val onRemove: (SelectedFeedInput) -> Unit,
    private val onAmountChanged: (SelectedFeedInput) -> Unit
) : RecyclerView.Adapter<SelectedFeedAdapter.VH>() {

    inner class VH(val b: ItemRationFeedBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemRationFeedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = feeds[position]
        val feed = item.feed
        val b = holder.b

        b.tvRationFeedName.text = feed.name
        b.tvRationFeedAmount.text = if (feed.pricePerKg > 0) "₺${feed.pricePerKg}/kg" else "Fiyat girilmedi"
        b.tvRationFeedAmountDm.text = "KM:${feed.dm}%  NEL:${feed.nel}  HP:${feed.cp}%"

        val dmKg = item.amountKg * (feed.dm / 100.0)
        b.tvRationFeedDmPct.text = if (item.amountKg > 0) "Giris: ${fmt(item.amountKg)} kg (KM ${fmt(dmKg)} kg)" else "Miktar girin"

        b.etManualAmountKg.setText(if (item.amountKg > 0) fmt(item.amountKg) else "")
        b.etManualAmountKg.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) return@setOnFocusChangeListener
            val value = b.etManualAmountKg.text?.toString()?.toDoubleOrNull() ?: 0.0
            if (kotlin.math.abs(item.amountKg - value) > 0.0001) {
                item.amountKg = value
                onAmountChanged(item)
            }
            val dmNow = item.amountKg * (feed.dm / 100.0)
            b.tvRationFeedDmPct.text = if (item.amountKg > 0) "Giris: ${fmt(item.amountKg)} kg (KM ${fmt(dmNow)} kg)" else "Miktar girin"
        }

        b.tvRationFeedCost.text = "✕ Cikar"
        b.tvRationFeedCost.setTextColor(Color.parseColor("#FF7A00"))
        b.tvRationFeedCost.setOnClickListener { onRemove(item) }
        b.tvRationFeedShadow.visibility = android.view.View.GONE
        try {
            b.viewColor.setBackgroundColor(Color.parseColor(FeedCategories.color(feed.category)))
        } catch (_: Exception) {}
    }

    override fun getItemCount() = feeds.size

    private fun fmt(v: Double): String =
        if (v >= 10) String.format("%.1f", v) else String.format("%.2f", v)
}