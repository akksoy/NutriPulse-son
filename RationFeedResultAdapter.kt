package com.nutripulse.app.ui.ration

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nutripulse.app.R
import com.nutripulse.app.data.model.FeedCategories
import com.nutripulse.app.data.model.RationItem

class RationFeedResultAdapter(
    private var items: List<RationItem>,
    private val feedShadowMap: Map<String, Double> = emptyMap()
) : RecyclerView.Adapter<RationFeedResultAdapter.VH>() {

    inner class VH(val root: android.view.View) : RecyclerView.ViewHolder(root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ration_feed_result, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        
        holder.root.findViewById<TextView>(R.id.tvRationFeedName).text = item.feedName
        holder.root.findViewById<TextView>(R.id.tvRationFeedAmount).text = "${fmt(item.amountKg)} kg"
        holder.root.findViewById<TextView>(R.id.tvRationFeedCost).text = "₺${fmt(item.costPerDay)}"
        
        holder.root.findViewById<TextView>(R.id.tvRationFeedCategory).text = FeedCategories.shortName(item.feedCategory)
        
        holder.root.findViewById<TextView>(R.id.tvRationFeedNdf).text = "DM:${fmt(item.dmPct)}%"
    }

    override fun getItemCount() = items.size

    fun update(list: List<RationItem>) {
        items = list
        notifyDataSetChanged()
    }

    private fun fmt(value: Double): String =
        if (kotlin.math.abs(value) >= 10.0) String.format("%.1f", value) else String.format("%.2f", value)
}
