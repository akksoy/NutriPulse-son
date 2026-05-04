package com.nutripulse.app.data

import android.content.Context
import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.StockItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object PriceScenarioManager {

    data class PriceScenario(
        val id: String,
        val name: String,
        val description: String,
        val prices: Map<String, Double>,
        val createdAt: Long,
        val isActive: Boolean = false
    )

    data class PriceAlert(
        val feedName: String,
        val currentPrice: Double,
        val thresholdPrice: Double,
        val alertType: AlertType,
        val message: String
    )

    enum class AlertType {
        PRICE_ABOVE_THRESHOLD,
        PRICE_BELOW_THRESHOLD,
        PRICE_CHANGE_HIGH,
        STOCK_LOW
    }

    private const val SCENARIOS_FILE = "price_scenarios.json"
    private const val ALERTS_FILE = "price_alerts.json"

    // Excel/CSV格式: YemAdi,Fiyat(TL/kg)
    // Supported formats: .csv, .xlsx (basit), .txt
    suspend fun importPricesFromCSV(
        context: Context,
        filePath: String,
        delimiter: String = ","
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                return@withContext ImportResult(0, 0, "Dosya bulunamadı")
            }

            val prices = mutableMapOf<String, Double>()
            val errors = mutableListOf<String>()

            FileReader(file).use { reader ->
                val lines = reader.readLines()
                var imported = 0
                var skipped = 0

                for ((index, line) in lines.withIndex()) {
                    if (index == 0 && (line.contains("fiyat", ignoreCase = true) || line.contains("price", ignoreCase = true))) {
                        continue // Header satırını atla
                    }

                    val parts = line.split(delimiter)
                    if (parts.size >= 2) {
                        val name = parts[0].trim()
                        val priceStr = parts[1]
                            .replace(" TL", "")
                            .replace("₺", "")
                            .replace(",", ".")
                            .trim()

                        val price = priceStr.toDoubleOrNull()
                        if (price != null && price > 0 && name.isNotBlank()) {
                            prices[name] = price
                            imported++
                        } else {
                            skipped++
                        }
                    } else if (line.isNotBlank()) {
                        errors.add("Satır $index: Format hatası")
                    }
                }

                if (prices.isNotEmpty()) {
                    saveScenario(
                        context,
                        PriceScenario(
                            id = "import_${System.currentTimeMillis()}",
                            name = "İmport $(file.name)",
                            description = "${imported} yem fiyatı içe aktarıldı",
                            prices = prices,
                            createdAt = System.currentTimeMillis(),
                            isActive = true
                        )
                    )
                }

                ImportResult(imported, skipped, if (errors.isEmpty()) "Başarılı" else errors.joinToString("; "))
            }
        } catch (e: Exception) {
            android.util.Log.e("PriceScenarioManager", "Import prices failed", e)
            ImportResult(0, 0, "Hata: ${e.message ?: e.toString()}")
        }
    }

    data class ImportResult(val imported: Int, val skipped: Int, val message: String)

    fun saveScenario(context: Context, scenario: PriceScenario) {
        val scenarios = loadAllScenarios(context).toMutableList()
        scenarios.removeAll { it.id == scenario.id }
        scenarios.add(scenario)
        saveAllScenarios(context, scenarios)
    }

    fun loadAllScenarios(context: Context): List<PriceScenario> {
        return try {
            val file = File(context.filesDir, SCENARIOS_FILE)
            if (!file.exists()) return emptyList()

            val json = file.readText()
            val list = mutableListOf<PriceScenario>()
            val jsonArray = org.json.JSONArray(json)

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val prices = mutableMapOf<String, Double>()
                val pricesObj = obj.optJSONObject("prices") ?: return emptyList()
                pricesObj.keys().forEach { key ->
                    prices[key] = pricesObj.getDouble(key)
                }

                list.add(
                    PriceScenario(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        description = obj.getString("description"),
                        prices = prices,
                        createdAt = obj.getLong("createdAt"),
                        isActive = obj.optBoolean("isActive", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveAllScenarios(context: Context, scenarios: List<PriceScenario>) {
        val jsonArray = org.json.JSONArray()
        scenarios.forEach { scenario ->
            val obj = JSONObject()
            obj.put("id", scenario.id)
            obj.put("name", scenario.name)
            obj.put("description", scenario.description)
            obj.put("createdAt", scenario.createdAt)
            obj.put("isActive", scenario.isActive)

            val pricesObj = JSONObject()
            scenario.prices.forEach { (key, value) ->
                pricesObj.put(key, value)
            }
            obj.put("prices", pricesObj)
            jsonArray.put(obj)
        }

        val file = File(context.filesDir, SCENARIOS_FILE)
        FileWriter(file).use { it.write(jsonArray.toString()) }
    }

    fun applyScenario(context: Context, scenarioId: String): Int {
        val scenarios = loadAllScenarios(context)
        val scenario = scenarios.find { it.id == scenarioId } ?: return 0

        // Senaryoyu aktif olarak işaretle
        scenarios.forEach {
            if (it.id == scenarioId) {
                saveScenario(context, it.copy(isActive = true))
            } else {
                saveScenario(context, it.copy(isActive = false))
            }
        }

        return scenario.prices.size
    }

    // Fiyat uyarı sistemi
    fun checkPriceAlerts(
        feeds: List<Feed>,
        stocks: List<StockItem>,
        alerts: List<PriceAlert>
    ): List<PriceAlert> {
        val activeAlerts = mutableListOf<PriceAlert>()

        feeds.forEach { feed ->
            val stock = stocks.find { it.feedId == feed.id }

            alerts.forEach { alert ->
                if (alert.feedName == feed.name || alert.feedName == feed.id.toString()) {
                    when (alert.alertType) {
                        AlertType.PRICE_ABOVE_THRESHOLD -> {
                            if (feed.pricePerKg > alert.thresholdPrice) {
                                activeAlerts.add(alert)
                            }
                        }
                        AlertType.PRICE_BELOW_THRESHOLD -> {
                            if (feed.pricePerKg < alert.thresholdPrice && feed.pricePerKg > 0) {
                                activeAlerts.add(alert)
                            }
                        }
                        AlertType.STOCK_LOW -> {
                            if (stock != null && stock.currentStockKg < stock.minStockKg * 1.5) {
                                activeAlerts.add(alert.copy(
                                    message = "Stok kritik: ${stock.currentStockKg} kg (min: ${stock.minStockKg})"
                                ))
                            }
                        }
                        else -> {}
                    }
                }
            }
        }

        return activeAlerts
    }

    fun saveAlerts(context: Context, alerts: List<PriceAlert>) {
        val file = File(context.filesDir, ALERTS_FILE)
        val jsonArray = org.json.JSONArray()

        alerts.forEach { alert ->
            val obj = JSONObject()
            obj.put("feedName", alert.feedName)
            obj.put("currentPrice", alert.currentPrice)
            obj.put("thresholdPrice", alert.thresholdPrice)
            obj.put("alertType", alert.alertType.name)
            obj.put("message", alert.message)
            jsonArray.put(obj)
        }

        FileWriter(file).use { it.write(jsonArray.toString()) }
    }

    fun loadAlerts(context: Context): List<PriceAlert> {
        return try {
            val file = File(context.filesDir, ALERTS_FILE)
            if (!file.exists()) return emptyList()

            val list = mutableListOf<PriceAlert>()
            val jsonArray = org.json.JSONArray(file.readText())

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    PriceAlert(
                        feedName = obj.getString("feedName"),
                        currentPrice = obj.getDouble("currentPrice"),
                        thresholdPrice = obj.getDouble("thresholdPrice"),
                        alertType = AlertType.valueOf(obj.getString("alertType")),
                        message = obj.getString("message")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    // What-if analiz: Fiyat değişikliğinin rasyon maliyetine etkisi
    fun calculateWhatIf(
        basePrices: Map<String, Double>,
        priceChanges: Map<String, Double>,
        feedUsage: Map<String, Double> // kg kullanım
    ): WhatIfResult {
        var baseCost = 0.0
        var newCost = 0.0

        feedUsage.forEach { (feedName, usageKg) ->
            val basePrice = basePrices[feedName] ?: 0.0
            val newPrice = priceChanges[feedName] ?: basePrice

            baseCost += basePrice * usageKg
            newCost += newPrice * usageKg
        }

        val costDiff = newCost - baseCost
        val percentChange = if (baseCost > 0) (costDiff / baseCost) * 100 else 0.0

        return WhatIfResult(
            baseCost = baseCost,
            newCost = newCost,
            costDiff = costDiff,
            percentChange = percentChange
        )
    }

    data class WhatIfResult(
        val baseCost: Double,
        val newCost: Double,
        val costDiff: Double,
        val percentChange: Double
    )
}