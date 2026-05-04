package com.nutripulse.app.utils

import java.text.DecimalFormat
import kotlin.math.round

object PriceFormatter {
    
    /**
     * Format price to 1 decimal place maximum
     * Example: 14.141176 -> "14,1"
     * Example: 14.0 -> "14"
     */
    fun formatPrice(price: Double): String {
        if (price <= 0) return "—"
        
        // Round to 1 decimal place
        val rounded = round(price * 10) / 10
        
        // Use Turkish locale for comma separator
        val df = DecimalFormat("0.#")
        val formatted = df.format(rounded)
        
        // Replace dot with comma for Turkish format
        return formatted.replace(".", ",")
    }
    
    /**
     * Format price with currency symbol
     * Example: 14.141176 -> "₺14,1/kg"
     */
    fun formatPriceWithCurrency(price: Double): String {
        if (price <= 0) return "₺—/kg"
        return "₺${formatPrice(price)}/kg"
    }
    
    /**
     * Clean and parse price input
     * Accepts both comma and dot as decimal separator
     */
    fun parsePrice(input: String?): Double {
        if (input.isNullOrBlank()) return 0.0
        
        return try {
            val cleaned = input.trim()
                .replace("₺", "")
                .replace("/kg", "")
                .replace("TL", "")
                .replace(",", ".")
                .trim()
            
            cleaned.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }
    
    /**
     * Validate price is reasonable (between 0 and 10000 TL/kg)
     */
    fun isValidPrice(price: Double): Boolean {
        return price > 0 && price < 10000.0
    }
}
