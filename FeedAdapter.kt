package com.nutripulse.app.ui.feed

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nutripulse.app.R
import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories
import com.nutripulse.app.utils.PriceFormatter

class FeedAdapter(
    private var feeds: List<Feed>,
    private val onFeedClick: (Feed) -> Unit,
    private val isInFinal: (Feed) -> Boolean = { false }
) : RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    inner class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvFeedName)
        val tvCategory: TextView = itemView.findViewById(R.id.tvFeedCategory)
        val tvDm: TextView = itemView.findViewById(R.id.tvFeedDm)
        val tvMe: TextView = itemView.findViewById(R.id.tvFeedMe)
        val tvCp: TextView = itemView.findViewById(R.id.tvFeedCp)
        val tvPrice: TextView = itemView.findViewById(R.id.tvFeedPrice)
        val viewColor: View = itemView.findViewById(R.id.viewCategoryColor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_feed, parent, false)
        return FeedViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val feed = feeds[position]
        val inFinal = isInFinal(feed)

        holder.tvName.text = feed.name
        holder.tvCategory.text = if (inFinal) "${FeedCategories.displayName(feed.category)} • Final" else FeedCategories.displayName(feed.category)
        holder.tvDm.text = "KM:${feed.dm.toInt()}%"
        holder.tvMe.text = if (feed.me > 0) "ME:${feed.me} MJ" else "ME:—"
        holder.tvCp.text = if (feed.cp > 0) "HP:${feed.cp}%" else "HP:—"
        holder.itemView.alpha = if (inFinal) 0.92f else 1f

        // Fiyat göster
        if (feed.pricePerKg > 0) {
            holder.tvPrice.text = PriceFormatter.formatPriceWithCurrency(feed.pricePerKg)
            holder.tvPrice.visibility = View.VISIBLE
        } else {
            holder.tvPrice.visibility = View.GONE
        }

        // Kategori rengi
        val colorHex = FeedCategories.color(feed.category)
        try {
            val c = Color.parseColor(colorHex)
            holder.viewColor.setBackgroundColor(c)
            holder.tvMe.setTextColor(c)
        } catch (e: Exception) {
            holder.viewColor.setBackgroundColor(Color.parseColor("#00FF88"))
        }

        holder.itemView.setOnClickListener { onFeedClick(feed) }
    }

    override fun getItemCount() = feeds.size

    fun updateFeeds(newFeeds: List<Feed>) {
        feeds = newFeeds
        notifyDataSetChanged()
    }
}