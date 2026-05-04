package com.nutripulse.app.ui.ration.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nutripulse.app.data.model.Ration

class RationCompareAdapter(
    private var list: List<Ration>,
    private val onSelect: (Ration) -> Unit
) : RecyclerView.Adapter<RationCompareAdapter.VH>() {

    private var selectedIds = mutableSetOf<Int>()

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView    = v.findViewById(android.R.id.text1)
        val tvDetail: TextView  = v.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        v.setPadding(32, 18, 32, 18)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = list[position]
        val isSelected = r.id in selectedIds

        holder.tvName.text = if (isSelected) "✅ ${r.name}" else r.name
        holder.tvName.setTextColor(
            if (isSelected) Color.parseColor("#00FF88") else Color.parseColor("#C0E8D0")
        )
        holder.tvName.textSize = 13f

        holder.tvDetail.text =
            "Maliyet: ₺${String.format("%.2f", r.totalCostPerDay)}/gün  " +
            "Kar: ₺${String.format("%.2f", r.netProfitPerDay)}  " +
            "Karşılama: %${r.coveragePct}  " +
            "Verim: ${r.estimatedYield} L"
        holder.tvDetail.setTextColor(Color.parseColor("#7ABFA0"))
        holder.tvDetail.textSize = 9.5f

        holder.itemView.setBackgroundColor(
            if (isSelected) Color.parseColor("#0D2E1A") else Color.TRANSPARENT
        )

        holder.itemView.setOnClickListener {
            if (r.id in selectedIds) selectedIds.remove(r.id)
            else if (selectedIds.size < 2) selectedIds.add(r.id)
            else {
                // 3. seçimde ilkini çıkar
                selectedIds.remove(selectedIds.first())
                selectedIds.add(r.id)
            }
            notifyDataSetChanged()
            onSelect(r)
        }
    }

    override fun getItemCount() = list.size

    fun update(newList: List<Ration>) {
        list = newList
        notifyDataSetChanged()
    }

    fun getSelectedIds(): Set<Int> = selectedIds.toSet()

    fun getSelected(): List<Ration> = list.filter { it.id in selectedIds }
}