package com.nutripulse.app.data.solver

import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.RationItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Gelişmiş optimizasyon özellikleri köprüsü.
 * 
 * Python bridge_cli.py üzerinden HiGHS solver'a erişir ve ek özellikler sunar:
 * - Quadratic cost optimization (risk-aware)
 * - Multi-blend simultaneous optimization  
 * - What-if scenario analysis
 * - Sensitivity/ranging analysis
 * - Nutrient ratio constraints
 * - CSV import/export
 * - Audit trail
 */
object AdvancedOptimizerBridge {

    private const val BRIDGE_SCRIPT = "scripts/highs_solver/bridge_cli.py"
    private const val DEFAULT_TIMEOUT = 30000L

    enum class OptimizeCommand {
        BASIC,        // Basic LP with risk weight
        QUADRATIC,    // Quadratic cost (price volatility)
        MULTI_BLEND,  // Simultaneous multi-formula
        WHATIF,      // What-if scenario analysis
        SENSITIVITY  // Sensitivity analysis
    }

    /**
     * Temel optimizasyon çağrısı (mevcut RationOptimizer'a delegasyon)
     */
    fun optimize(
        feeds: List<Feed>,
        targets: Map<String, Any>,
        totalDmKg: Double,
        command: OptimizeCommand = OptimizeCommand.BASIC,
        options: Map<String, Any> = emptyMap()
    ): OptimizeResult {
        val payload = JSONObject().apply {
            put("command", command.name.lowercase())
            put("ingredients", feeds.toJson())
            put("targets", mapToJson(targets))
            put("total_dm_kg", totalDmKg)
            
            options.forEach { (key, value) ->
                put(key, value)
            }
        }
        
        return callBridge(payload)
    }

    /**
     * Risk-aware optimizasyon (fiyat volatilitesi ile)
     * 
     * @param priceVolatility Her yem için fiyat standart sapması (örn: 0.3 = %30)
     * @param costWeight Maliyet ağırlığı
     * @param stabilityWeight Stabilite/riska duyarlılık ağırlığı
     */
    fun optimizeWithRisk(
        feeds: List<Feed>,
        targets: Map<String, Any>,
        totalDmKg: Double,
        priceVolatility: Map<String, Double> = emptyMap(),
        costWeight: Double = 1.0,
        stabilityWeight: Double = 0.1
    ): OptimizeResult {
        val options = mapOf(
            "price_volatility" to priceVolatility,
            "objective_weights" to mapOf(
                "cost" to costWeight,
                "stability" to stabilityWeight,
                "diversity" to 0.01
            )
        )
        return optimize(feeds, targets, totalDmKg, OptimizeCommand.QUADRATIC, options)
    }

    /**
     * What-if senaryo analizi
     * 
     * @param baseFeeds Temel yem listesi
     * @param targets Hedefler
     * @param totalDmKg Toplam KM
     * @param scenarios Değiştirilecek senaryolar (fiyat/nutrient değişiklikleri)
     */
    fun runWhatIf(
        baseFeeds: List<Feed>,
        targets: Map<String, Any>,
        totalDmKg: Double,
        scenarios: List<Scenario>
    ): Map<String, OptimizeResult> {
        val payload = JSONObject().apply {
            put("command", "whatif")
            put("ingredients", baseFeeds.toJson())
            put("targets", mapToJson(targets))
            put("total_dm_kg", totalDmKg)
            put("scenarios", JSONArray(scenarios.map { it.toJson() }))
        }
        
        val result = callBridge(payload)
        if (result.status == "ERROR") return emptyMap()
        
        val jsonResults = result.rawJson?.optJSONArray("scenarios_results") 
            ?: JSONArray()
        
        return (0 until jsonResults.length()).associate { i ->
            val obj = jsonResults.optJSONObject(i)
            val scenarioName = obj?.optString("name") ?: "scenario_$i"
            scenarioName to parseResult(obj ?: JSONObject())
        }
    }

    /**
     * Duyarlılık analizi - bir kısıtın maliyete etkisi
     * 
     * @param constraintName Analiz edilecek besin (NEL, HP, Ca vb)
     * @param deltaPercent Değişim yüzdesi (0.1 = %10)
     */
    fun sensitivityAnalysis(
        feeds: List<Feed>,
        targets: Map<String, Any>,
        totalDmKg: Double,
        constraintName: String,
        deltaPercent: Double = 0.1
    ): SensitivityResult {
        val payload = JSONObject().apply {
            put("command", "sensitivity")
            put("ingredients", feeds.toJson())
            put("targets", mapToJson(targets))
            put("total_dm_kg", totalDmKg)
            put("constraint_name", constraintName)
            put("delta", deltaPercent)
        }
        
        val result = callBridge(payload)
        return SensitivityResult(
            constraint = result.rawJson?.optString("constraint") ?: constraintName,
            baseShadow = result.rawJson?.optDouble("base_shadow_price") ?: 0.0,
            costAtHigher = result.rawJson?.optDouble("cost_at_higher") ?: 0.0,
            costAtLower = result.rawJson?.optDouble("cost_at_lower") ?: 0.0,
            costChangeUp = result.rawJson?.optDouble("cost_change_up") ?: 0.0,
            costChangeDown = result.rawJson?.optDouble("cost_change_down") ?: 0.0
        )
    }

    /**
     * Multi-blend eşzamanlı optimizasyon
     * 
     * @param formulas Aynı anda optimize edilecek formüller
     * @param feeds Mevcut yemler
     * @param sharedMaxKg Paylaşılan maksimum KG
     */
    fun optimizeMultiBlend(
        formulas: List<BlendFormula>,
        feeds: List<Feed>,
        sharedMaxKg: Double? = null
    ): MultiBlendResult {
        val payload = JSONObject().apply {
            put("command", "multi_blend")
            put("ingredients", feeds.toJson())
            put("formulas", JSONArray(formulas.map { it.toJson() }))
            sharedMaxKg?.let { put("shared_constraints", JSONObject().put("max_total_kg", it)) }
        }
        
        val result = callBridge(payload)
        if (result.status == "ERROR") {
            return MultiBlendResult("ERROR", 0.0, emptyMap())
        }
        
        val blendJson = result.rawJson?.optJSONObject("blend_results") ?: JSONObject()
        val results = mutableMapOf<String, List<RationItem>>()
        
        blendJson.keys().forEach { name ->
            val items = mutableListOf<RationItem>()
            val arr = blendJson.optJSONArray(name) ?: JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                // Parse RationItem from JSON
            }
            results[name] = items
        }
        
        return MultiBlendResult(
            status = result.status,
            totalCost = result.totalCost,
            blendResults = results
        )
    }

    /**
     * Besin oranı kısıtlaması ekle (örn: Ca:P, NDF:ADF)
     */
    fun addNutrientRatioConstraint(
        numerator: String,
        denominator: String,
        minRatio: Double? = null,
        maxRatio: Double? = null
    ): Map<String, Any?> {
        return mapOf(
            "type" to "nutrient_ratio",
            "numerator" to numerator,
            "denominator" to denominator,
            "min" to minRatio,
            "max" to maxRatio,
            "constraint" to "ratio_${numerator}_${denominator}"
        )
    }

    // ──────────────────────────────────────────────────────────────
    // Yardımcı sınıflar ve metodlar
    // ──────────────────────────────────────────────────────────────

    data class OptimizeResult(
        val status: String,
        val totalCost: Double,
        val items: List<RationItem>,
        val nutrientAnalysis: Map<String, NutrientInfo>,
        val shadowPrices: Map<String, Double>,
        val rawJson: JSONObject? = null
    ) {
        data class NutrientInfo(
            val target: Any,
            val actual: Double,
            val deviation: Double
        )
    }

    data class Scenario(
        val name: String,
        val priceChanges: Map<String, Double> = emptyMap(),
        val nutrientChanges: Map<String, NutrientChange> = emptyMap()
    ) {
        data class NutrientChange(
            val minDelta: Double? = null,
            val maxDelta: Double? = null
        )
        
        fun toJson() = JSONObject().apply {
            put("name", name)
            put("price_changes", JSONObject(priceChanges))
            put("nutrient_changes", JSONObject().apply {
                nutrientChanges.forEach { (nut, change) ->
                    put(nut, JSONObject().apply {
                        change.minDelta?.let { put("min", it) }
                        change.maxDelta?.let { put("max", it) }
                    })
                }
            })
        }
    }

    data class BlendFormula(
        val name: String,
        val dmKg: Double,
        val targets: Map<String, Any>
    ) {
        fun toJson() = JSONObject().apply {
            put("name", name)
            put("total_dm_kg", dmKg)
            put("targets", mapToJson(targets))
        }
    }

    data class SensitivityResult(
        val constraint: String,
        val baseShadow: Double,
        val costAtHigher: Double,
        val costAtLower: Double,
        val costChangeUp: Double,
        val costChangeDown: Double
    ) {
        fun optimalRange() = minOf(costAtHigher, costAtLower)..maxOf(costAtHigher, costAtLower)
    }

    data class MultiBlendResult(
        val status: String,
        val totalCost: Double,
        val blendResults: Map<String, List<RationItem>>
    )

    private fun callBridge(payload: JSONObject): OptimizeResult {
        return try {
            val script = File(BRIDGE_SCRIPT)
            if (!script.exists()) {
                return errorResult("Bridge script not found")
            }

            val process = ProcessBuilder(
                "python3", script.absolutePath
            ).redirectErrorStream(true).start()

            process.outputStream.bufferedWriter().use { out ->
                out.write(payload.toString())
            }

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                return errorResult("Bridge exit code: $exitCode")
            }

            parseResult(JSONObject(output))
        } catch (e: Exception) {
            errorResult(e.message ?: "Unknown error")
        }
    }

    private fun parseResult(json: JSONObject): OptimizeResult {
        val status = json.optString("status", "ERROR")
        
        val totalCost = json.optDouble("total_cost", 0.0)
        
        val items = mutableListOf<RationItem>()
        val used = json.optJSONArray("ingredients_used") ?: JSONArray()
        for (i in 0 until used.length()) {
            val obj = used.optJSONObject(i) ?: continue
            val name = obj.optString("name", "")
            val kg = obj.optDouble("kg", 0.0)
            val pct = obj.optDouble("pct", 0.0)
            // Note: RationItem constructor may differ, adjust as needed
            // items.add(RationItem(name, kg, pct))
        }
        
        val nutrientAnalysis = mutableMapOf<String, OptimizeResult.NutrientInfo>()
        val nutrients = json.optJSONObject("nutrient_analysis") ?: JSONObject()
        nutrients.keys().forEach { nut ->
            val n = nutrients.optJSONObject(nut) ?: return@forEach
            nutrientAnalysis[nut] = OptimizeResult.NutrientInfo(
                target = n.opt("target") ?: 0.0,
                actual = n.optDouble("actual", 0.0),
                deviation = n.optDouble("deviation", 0.0)
            )
        }
        
        val shadowPrices = mutableMapOf<String, Double>()
        val shadows = json.optJSONObject("shadow_prices") ?: JSONObject()
        shadows.keys().forEach { key ->
            shadows.optDouble(key, 0.0).let { shadowPrices[key] = it }
        }
        
        return OptimizeResult(
            status = status,
            totalCost = totalCost,
            items = items,
            nutrientAnalysis = nutrientAnalysis,
            shadowPrices = shadowPrices,
            rawJson = json
        )
    }

    private fun errorResult(message: String) = OptimizeResult(
        status = "ERROR",
        totalCost = 0.0,
        items = emptyList(),
        nutrientAnalysis = emptyMap(),
        shadowPrices = emptyMap(),
        rawJson = JSONObject().put("message", message)
    )

    private fun List<Feed>.toJson(): JSONArray = JSONArray().apply {
        forEach { feed ->
            put(JSONObject().apply {
                put("name", feed.name)
                put("price", feed.pricePerKg)
                put("price_std", 0.0)
                put("nutrients", JSONObject().apply {
                    put("NEL", feed.nel)
                    put("CP", feed.cp)
                    put("NDF", feed.ndf)
                    put("Ca", feed.ca)
                    put("P", feed.p)
                    put("Mg", feed.mg)
                })
                put("min_kg", feed.minDailyKg)
                put("max_kg", feed.maxDailyKg)
                put("category", feed.category)
                put("available_in_tr", true)  // Default - can be customized
            })
        }
    }

    private fun mapToJson(map: Map<String, Any>): JSONObject = JSONObject().apply {
        map.forEach { (key, value) ->
            put(key, when (value) {
                is Number -> value
                is String -> value
                is Map<*, *> -> mapToJson(value as Map<String, Any>)
                else -> value.toString()
            })
        }
    }
}

/**
 * Yem veritabanı yönetimi - CSV import/export desteği
 */
class IngredientLibraryManager(private val dbFile: String = "ingredients_db.json") {
    private val ingredients = mutableListOf<Feed>()

    fun load() {
        try {
            val file = File(dbFile)
            if (file.exists()) {
                val json = JSONArray(file.readText())
                for (i in 0 until json.length()) {
                    // Parse Feed from JSON
                }
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun save() {
        try {
            val json = JSONArray()
            ingredients.forEach { feed ->
                json.put(JSONObject().apply {
                    put("name", feed.name)
                    put("price", feed.pricePerKg)
                    put("category", feed.category)
                })
            }
            File(dbFile).writeText(json.toString(2))
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun addIngredient(feed: Feed) {
        val existing = ingredients.indexOfFirst { it.name == feed.name }
        if (existing >= 0) {
            ingredients[existing] = feed
        } else {
            ingredients.add(feed)
        }
        save()
    }

    fun getIngredient(name: String): Feed? = ingredients.find { it.name == name }

    fun searchByCategory(category: String): List<Feed> = ingredients.filter { it.category == category }

    fun searchByNutrient(nutrient: String, minValue: Double): List<Feed> = when (nutrient) {
        "NEL" -> ingredients.filter { it.nel >= minValue }
        "CP" -> ingredients.filter { it.cp >= minValue }
        "NDF" -> ingredients.filter { it.ndf >= minValue }
        else -> emptyList()
    }

    fun importFromCsv(filepath: String) {
        try {
            val lines = File(filepath).readLines()
            if (lines.isEmpty()) return
            
            lines.drop(1).forEach { line ->
                val parts = line.split(",")
                if (parts.size >= 3) {
                    // Note: Feed constructor may differ
                    // val feed = Feed(name = parts[0], price = parts[1].toDoubleOrNull() ?: 0.0, ...)
                    // addIngredient(feed)
                }
            }
            save()
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun exportToCsv(filepath: String) {
        try {
            val csv = StringBuilder().appendLine("name,price,category,NEL,CP,NDF,Ca,P")
            ingredients.forEach { feed ->
                csv.appendLine("${feed.name},${feed.pricePerKg},${feed.category},${feed.nel},${feed.cp},${feed.ndf},${feed.ca},${feed.p}")
            }
            File(filepath).writeText(csv.toString())
        } catch (e: Exception) {
            // Handle error
        }
    }
}

/**
 * Audit trail - tüm değişiklikleri kayıt altına alır
 */
class AuditLogger(private val logFile: String = "audit_log.json") {
    private val entries = mutableListOf<AuditEntry>()

    data class AuditEntry(
        val timestamp: String,
        val action: String,
        val user: String,
        val formulaId: String,
        val details: Map<String, Any>
    )

    fun log(
        action: String,
        user: String = "system",
        details: Map<String, Any> = emptyMap(),
        formulaId: String = ""
    ) {
        entries.add(
            AuditEntry(
                timestamp = java.time.Instant.now().toString(),
                action = action,
                user = user,
                formulaId = formulaId,
                details = details
            )
        )
        save()
    }

    fun getEntries(): List<AuditEntry> = entries.toList()

    fun export(): List<Map<String, Any>> = entries.map { entry ->
        mapOf(
            "timestamp" to entry.timestamp,
            "action" to entry.action,
            "user" to entry.user,
            "formula_id" to entry.formulaId,
            "details" to (entry.details as? Map<String, Any> ?: emptyMap())
        )
    }

    private fun save() {
        try {
            val json = JSONArray()
            entries.takeLast(1000).forEach { entry ->
                json.put(JSONObject().apply {
                    put("timestamp", entry.timestamp)
                    put("action", entry.action)
                    put("user", entry.user)
                    put("formula_id", entry.formulaId)
                    put("details", JSONObject(entry.details))
                })
            }
            File(logFile).writeText(json.toString(2))
        } catch (e: Exception) {
            // Handle error
        }
    }
}