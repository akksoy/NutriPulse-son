package com.nutripulse.app.ui.stock

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nutripulse.app.data.model.StockTransaction
import com.nutripulse.app.data.model.TransactionType
import com.nutripulse.app.databinding.ItemStockTransactionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StockTransactionAdapter(
    private var items: List<StockTransaction>
) : RecyclerView.Adapter<StockTransactionAdapter.TxVH>() {

    inner class TxVH(val b: ItemStockTransactionBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, vt: Int): TxVH {
        val b = ItemStockTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TxVH(b)
    }

    override fun onBindViewHolder(holder: TxVH, pos: Int) {
        val tx = items[pos]
        val b = holder.b
        val color = TransactionType.color(tx.type)

        b.tvTxEmoji.text = TransactionType.emoji(tx.type)
        b.tvTxType.text = TransactionType.displayName(tx.type)
        b.tvTxType.setTextColor(Color.parseColor(color))

        b.tvTxRefNo.text = tx.referenceNo.ifEmpty { "" }
        b.tvTxDate.text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            .format(Date(tx.date))
        b.tvTxSupplier.text = tx.supplierName.ifEmpty { "" }

        // Miktar: giriş + yeşil, çıkış - sarı
        val sign = if (tx.type == TransactionType.PURCHASE ||
                       (tx.type == TransactionType.ADJUSTMENT && tx.amountKg > 0)) "+" else "-"
        b.tvTxAmount.text = "$sign${String.format("%.1f", kotlin.math.abs(tx.amountKg))} kg"
        b.tvTxAmount.setTextColor(Color.parseColor(color))

        b.tvTxCost.text = if (tx.totalCost > 0) "₺${String.format("%.2f", tx.totalCost)}" else ""
        b.tvTxAfterStock.text = "Sonra: ${String.format("%.1f", tx.stockAfter)} kg"
    }

    override fun getItemCount() = items.size
    fun update(list: List<StockTransaction>) { items = list; notifyDataSetChanged() }
}