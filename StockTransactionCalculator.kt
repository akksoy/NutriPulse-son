package com.nutripulse.app.ui.stock

import com.nutripulse.app.data.model.StockItem
import com.nutripulse.app.data.model.StockTransaction
import com.nutripulse.app.data.model.TransactionType

data class StockTransactionPreview(
    val stockAfter: Double,
    val signedAmountKg: Double,
    val totalCost: Double,
    val updatedAvgCostPerKg: Double,
    val updatedDaysRemaining: Int
)

data class StockTransactionDraft(
    val transaction: StockTransaction,
    val preview: StockTransactionPreview
)

object StockTransactionCalculator {

    fun validate(item: StockItem, type: String, amountKg: Double): String? = when {
        amountKg <= 0.0 -> "Miktar girin!"
        (type == TransactionType.USAGE || type == TransactionType.WASTE) && amountKg > item.currentStockKg ->
            "Çıkış miktarı mevcut stoktan fazla olamaz!"
        else -> null
    }

    fun preview(item: StockItem, type: String, amountKg: Double?, pricePerKg: Double = item.lastPurchasePrice): StockTransactionPreview {
        if (amountKg == null || amountKg <= 0.0) {
            return StockTransactionPreview(
                stockAfter = item.currentStockKg,
                signedAmountKg = 0.0,
                totalCost = 0.0,
                updatedAvgCostPerKg = item.avgCostPerKg,
                updatedDaysRemaining = computeDaysRemaining(item.currentStockKg, item.dailyUsageKg)
            )
        }

        val newStock = when (type) {
            TransactionType.PURCHASE -> item.currentStockKg + amountKg
            TransactionType.USAGE, TransactionType.WASTE -> item.currentStockKg - amountKg
            TransactionType.ADJUSTMENT -> amountKg
            else -> item.currentStockKg
        }

        val signedAmount = when (type) {
            TransactionType.PURCHASE -> amountKg
            TransactionType.USAGE, TransactionType.WASTE -> -amountKg
            TransactionType.ADJUSTMENT -> amountKg - item.currentStockKg
            else -> 0.0
        }

        val avgCost = when (type) {
            TransactionType.PURCHASE -> {
                val oldValue = item.currentStockKg * item.avgCostPerKg
                val newValue = amountKg * pricePerKg
                (oldValue + newValue) / (newStock.coerceAtLeast(1.0))
            }
            else -> item.avgCostPerKg
        }

        return StockTransactionPreview(
            stockAfter = newStock.coerceAtLeast(0.0),
            signedAmountKg = signedAmount,
            totalCost = if (type == TransactionType.PURCHASE) amountKg * pricePerKg else 0.0,
            updatedAvgCostPerKg = avgCost,
            updatedDaysRemaining = computeDaysRemaining(newStock.coerceAtLeast(0.0), item.dailyUsageKg)
        )
    }

    fun buildDraft(
        item: StockItem,
        type: String,
        amountKg: Double,
        pricePerKg: Double,
        referenceNo: String,
        supplierName: String,
        notes: String
    ): StockTransactionDraft {
        val preview = preview(item, type, amountKg, pricePerKg)

        val transaction = StockTransaction(
            stockItemId = item.id,
            feedName = item.feedName,
            feedCategory = item.feedCategory,
            type = type,
            amountKg = preview.signedAmountKg,
            pricePerKg = if (type == TransactionType.PURCHASE) pricePerKg else 0.0,
            totalCost = preview.totalCost,
            stockAfter = preview.stockAfter,
            referenceNo = referenceNo,
            supplierName = supplierName,
            notes = notes
        )

        return StockTransactionDraft(transaction = transaction, preview = preview)
    }

    private fun computeDaysRemaining(stockKg: Double, dailyUsageKg: Double): Int {
        return if (dailyUsageKg > 0.0) (stockKg / dailyUsageKg).toInt() else 0
    }
}
