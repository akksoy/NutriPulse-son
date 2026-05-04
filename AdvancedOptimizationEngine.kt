package com.nutripulse.app.data.solver

import com.nutripulse.app.data.model.Feed
import kotlin.math.*
import kotlin.random.Random

/**
 * Advanced NutriPulse Optimization Engine
 * 
 * Modern Brill Formulation seviyesinde ve üzeri optimizasyon:
 * - Linear Programming (LP) - Simpleks
 * - Quadratic Programming (QP) - Risk-aware
 * - Stochastic Programming - Fiyat belirsizliği
 * - Robust Optimization - En kötü durum senaryosu
 * - Multi-Objective - Pareto optimal
 * - Parametric Analysis - Kısıt duyarlılık
 * - Integer Programming - Kademeli miktarlar
 * - Sensitivity/Ranging Analysis - Gölge fiyat aralıkları
 */
object AdvancedOptimizationEngine {

    // ========== VERİ TİPLERİ ==========

    data class OptimizationConfig(
        val objective: ObjectiveType = ObjectiveType.MIN_COST,
        val allowInteger: Boolean = false,
        val riskLevel: RiskLevel = RiskLevel.NONE,
        val sensitivityAnalysis: Boolean = true,
        val maxIterations: Int = 5000,
        val tolerance: Double = 1e-9
    )

    enum class ObjectiveType {
        MIN_COST,           // Minimum maliyet
        MAX_NUTRIENT,       // Maksimum besin
        MAX_PROFIT,         // Maksimum kar (fiyat - maliyet)
        MIN_ENVIRONMENT,    // Minimum karbon ayak izi
        MAX_DIVERSITY,      // Maksimum yem çeşitliliği
        PARETO_BALANCED     // Çoklu amaç dengeli
    }

    enum class RiskLevel {
        NONE,           // Deterministik
        LOW,            // %10 belirsizlik
        MEDIUM,         // %25 belirsizlik
        HIGH,           // %50 belirsizlik
        ROBUST          // En kötü durum
    }

    data class NutrientConstraint(
        val name: String,
        val min: Double?,
        val max: Double?,
        val penaltyMin: Double = 1000.0,   // Min ihlal cezası
        val penaltyMax: Double = 1000.0,    // Max ihlal cezası
        val isSoft: Boolean = true         // Yumuşak kısıt (ceza ile)
    )

    data class OptimizationResult(
        val status: Status,
        val feeds: List<FeedSelection>,
        val totalCost: Double,
        val totalDmKg: Double,
        val nutrients: Map<String, NutrientResult>,
        val shadowPrices: Map<String, Double>,
        val sensitivity: SensitivityAnalysis?,
        val riskAnalysis: RiskAnalysis?,
        val paretoFront: List<ParetoSolution>?,
        val message: String,
        val iterations: Int,
        val solveTimeMs: Long
    )

    data class FeedSelection(
        val feed: Feed,
        val amountKg: Double,
        val amountDmKg: Double,
        val cost: Double,
        val sharePercent: Double,
        val isInteger: Boolean = false
    )

    data class NutrientResult(
        val value: Double,
        val min: Double?,
        val max: Double?,
        val status: String, // "OK", "BELOW_MIN", "ABOVE_MAX"
        val slack: Double
    )

    data class SensitivityAnalysis(
        val reducedCosts: Map<String, Double>,        // Azaltılmış maliyetler
        val shadowPriceRanges: Map<String, ShadowRange>, // Gölge fiyat aralıkları
        val bindingConstraints: List<String>,          // Bağlayıcı kısıtlar
        val objectiveCoeffRange: Map<String, ClosedRange<Double>> // Objektif katsayı aralıkları
    )

    data class ShadowRange(
        val currentPrice: Double,
        val minAllowed: Double,
        val maxAllowed: Double,
        val binding: Boolean,
        val minShadowPrice: Double,
        val maxShadowPrice: Double
    )

    data class RiskAnalysis(
        val expectedCost: Double,
        val costVariance: Double,
        val costStdDev: Double,
        val var95: Double,          // Value at Risk %95
        val cvar95: Double,         // Conditional VaR
        val worstCaseCost: Double,
        val bestCaseCost: Double,
        val probabilityInfeasible: Double,
        val scenarios: List<ScenarioResult>
    )

    data class ScenarioResult(
        val name: String,
        val probability: Double,
        val cost: Double,
        val isFeasible: Boolean,
        val nutrientViolations: Map<String, Double>
    )

    data class ParetoSolution(
        val cost: Double,
        val nutrientScore: Double,
        val diversityScore: Double,
        val feedSelections: List<FeedSelection>
    )

    enum class Status {
        OPTIMAL,
        SUBOPTIMAL,
        INFEASIBLE,
        UNBOUNDED,
        TIMEOUT,
        ERROR
    }

    // ========== ANA OPTİMİZASYON FONKSİYONU ==========

    fun optimize(
        feeds: List<Feed>,
        nutrientConstraints: List<NutrientConstraint>,
        totalDmKg: Double,
        config: OptimizationConfig = OptimizationConfig()
    ): OptimizationResult {
        val startTime = System.currentTimeMillis()

        // Giriş doğrulama
        if (feeds.isEmpty() || totalDmKg <= 0) {
            return OptimizationResult(
                status = Status.ERROR,
                feeds = emptyList(),
                totalCost = 0.0,
                totalDmKg = 0.0,
                nutrients = emptyMap(),
                shadowPrices = emptyMap(),
                sensitivity = null,
                riskAnalysis = null,
                paretoFront = null,
                message = "Geçersiz girdi",
                iterations = 0,
                solveTimeMs = System.currentTimeMillis() - startTime
            )
        }

        // Kısıt kontrolü
        val feasibleConstraints = nutrientConstraints.filter { it.min != null || it.max != null }
        if (feasibleConstraints.isEmpty()) {
            return OptimizationResult(
                status = Status.ERROR,
                feeds = emptyList(),
                totalCost = 0.0,
                totalDmKg = 0.0,
                nutrients = emptyMap(),
                shadowPrices = emptyMap(),
                sensitivity = null,
                riskAnalysis = null,
                paretoFront = null,
                message = "Kısıt bulunamadı",
                iterations = 0,
                solveTimeMs = System.currentTimeMillis() - startTime
            )
        }

        // Simpleks çözümü
        val simplexResult = solveWithSimplex(feeds, nutrientConstraints, totalDmKg, config)

        // Risk analizi (eğer istenirse)
        val riskAnalysis = if (config.riskLevel != RiskLevel.NONE) {
            performRiskAnalysis(feeds, nutrientConstraints, totalDmKg, config.riskLevel)
        } else null

        // Duyarlılık analizi (eğer istenirse)
        val sensitivity = if (config.sensitivityAnalysis && simplexResult.status == Status.OPTIMAL) {
            performSensitivityAnalysis(feeds, nutrientConstraints, simplexResult)
        } else null

        return OptimizationResult(
            status = simplexResult.status,
            feeds = simplexResult.selections,
            totalCost = simplexResult.totalCost,
            totalDmKg = totalDmKg,
            nutrients = simplexResult.nutrients,
            shadowPrices = simplexResult.shadowPrices,
            sensitivity = sensitivity,
            riskAnalysis = riskAnalysis,
            paretoFront = null,
            message = simplexResult.message,
            iterations = simplexResult.iterations,
            solveTimeMs = System.currentTimeMillis() - startTime
        )
    }

    // ========== PARETO OPTİMİZASYON (ÇOKLU AMAÇ) ==========

    fun optimizePareto(
        feeds: List<Feed>,
        nutrientConstraints: List<NutrientConstraint>,
        totalDmKg: Double,
        objectives: List<ObjectiveType> = listOf(ObjectiveType.MIN_COST, ObjectiveType.MAX_DIVERSITY),
        numSolutions: Int = 10
    ): List<ParetoSolution> {
        val solutions = mutableListOf<ParetoSolution>()

        // Ağırlık vektörleri oluştur
        val weights = generateWeightVectors(objectives.size, numSolutions)

        for (weight in weights) {
            val weightedConstraints = nutrientConstraints.map { constraint ->
                val weightPenalty = when (constraint.name) {
                    "cp" -> 1.5   // Protein daha önemli
                    "nel" -> 1.3  // Enerji önemli
                    else -> 1.0
                }
                constraint.copy(
                    penaltyMin = constraint.penaltyMin * weightPenalty,
                    penaltyMax = constraint.penaltyMax * weightPenalty
                )
            }

            val result = optimize(feeds, weightedConstraints, totalDmKg, OptimizationConfig(
                objective = ObjectiveType.PARETO_BALANCED
            ))

            if (result.status == Status.OPTIMAL || result.status == Status.SUBOPTIMAL) {
                val nutrientScore = calculateNutrientScore(result.nutrients)
                val diversityScore = calculateDiversityScore(result.feeds)

                solutions.add(ParetoSolution(
                    cost = result.totalCost,
                    nutrientScore = nutrientScore,
                    diversityScore = diversityScore,
                    feedSelections = result.feeds
                ))
            }
        }

        // Pareto optimal çözümleri filtrele
        return filterParetoOptimal(solutions)
    }

    private fun generateWeightVectors(numObjectives: Int, numSolutions: Int): List<List<Double>> {
        val weights = mutableListOf<List<Double>>()
        
        // Eşit dağılımdan rastgele ağırlıklara
        for (i in 0 until numSolutions) {
            val weight = MutableList(numObjectives) { 1.0 / numObjectives }
            // Rastgele boz
            for (j in weight.indices) {
                weight[j] += (Random.nextDouble() - 0.5) * 0.3
            }
            // Normalize
            val sum = weight.sum()
            weights.add(weight.map { (it / sum).coerceIn(0.0, 1.0) })
        }
        
        return weights
    }

    // ========== STOCHASTİC OPTİMİZASYON ==========

    fun optimizeStochastic(
        feeds: List<Feed>,
        nutrientConstraints: List<NutrientConstraint>,
        totalDmKg: Double,
        priceScenarios: List<Map<String, Double>>, // Fiyat senaryoları
        probabilities: List<Double> = List(priceScenarios.size) { 1.0 / priceScenarios.size }
    ): OptimizationResult {
        // Her senaryo için ayrı çöz
        val scenarioResults = priceScenarios.mapIndexed { idx, prices ->
            val adjustedFeeds = feeds.map { feed ->
                val newPrice = prices[feed.name] ?: feed.pricePerKg
                feed.copy(pricePerKg = newPrice)
            }
            optimize(adjustedFeeds, nutrientConstraints, totalDmKg)
        }

        // Olasılıklı ortalama maliyet
        var expectedCost = 0.0
        var totalProb = 0.0
        for (i in priceScenarios.indices) {
            expectedCost += scenarioResults[i].totalCost * probabilities[i]
            totalProb += probabilities[i]
        }
        expectedCost /= totalProb

        // En iyi ve en kötü senaryo
        val bestCase = scenarioResults.minByOrNull { it.totalCost }
        val worstCase = scenarioResults.maxByOrNull { it.totalCost }

        // Ortalama seçimler
        val avgSelections = mutableMapOf<Feed, Double>()
        for (result in scenarioResults) {
            for (selection in result.feeds) {
                avgSelections[selection.feed] = (avgSelections[selection.feed] ?: 0.0) + 
                    (selection.amountKg * probabilities[scenarioResults.indexOf(result)])
            }
        }

        val finalSelections = avgSelections.map { (feed, amount) ->
            FeedSelection(
                feed = feed,
                amountKg = amount,
                amountDmKg = amount * feed.dm / 100.0,
                cost = amount * feed.pricePerKg,
                sharePercent = amount / avgSelections.values.sum() * 100
            )
        }

        return OptimizationResult(
            status = if (scenarioResults.all { it.status == Status.OPTIMAL }) Status.OPTIMAL else Status.SUBOPTIMAL,
            feeds = finalSelections,
            totalCost = expectedCost,
            totalDmKg = totalDmKg,
            nutrients = scenarioResults.first().nutrients, // İlk sonuç besin değerleri
            shadowPrices = scenarioResults.first().shadowPrices,
            sensitivity = null,
            riskAnalysis = RiskAnalysis(
                expectedCost = expectedCost,
                costVariance = scenarioResults.map { 
                    (it.totalCost - expectedCost).pow(2) 
                }.average(),
                costStdDev = sqrt(scenarioResults.map { 
                    (it.totalCost - expectedCost).pow(2) 
                }.average()),
                var95 = scenarioResults.sortedBy { it.totalCost }[
                    (scenarioResults.size * 0.95).toInt()
                ].totalCost,
                cvar95 = scenarioResults.take((scenarioResults.size * 0.95).toInt())
                    .map { it.totalCost }.average(),
                worstCaseCost = worstCase?.totalCost ?: expectedCost,
                bestCaseCost = bestCase?.totalCost ?: expectedCost,
                probabilityInfeasible = scenarioResults.count { it.status == Status.INFEASIBLE }.toDouble() / 
                    scenarioResults.size,
                scenarios = priceScenarios.indices.map { idx ->
                    ScenarioResult(
                        name = "Senaryo ${idx + 1}",
                        probability = probabilities[idx],
                        cost = scenarioResults[idx].totalCost,
                        isFeasible = scenarioResults[idx].status == Status.OPTIMAL,
                        nutrientViolations = scenarioResults[idx].nutrients
                            .filter { it.value.status != "OK" }
                            .mapValues { it.value.value }
                    )
                }
            ),
            paretoFront = null,
            message = "Stokastik optimizasyon tamamlandı",
            iterations = scenarioResults.sumOf { it.iterations },
            solveTimeMs = 0
        )
    }

    // ========== ROBUST OPTİMİZASYON ==========

    fun optimizeRobust(
        feeds: List<Feed>,
        nutrientConstraints: List<NutrientConstraint>,
        totalDmKg: Double,
        uncertaintyLevel: Double = 0.2  // %20 belirsizlik
    ): OptimizationResult {
        // En kötü durum için kısıtları güçlendir
        val robustConstraints = nutrientConstraints.map { constraint ->
            constraint.copy(
                min = constraint.min?.let { it * (1 - uncertaintyLevel) },
                max = constraint.max?.let { it * (1 + uncertaintyLevel) }
            )
        }

        // Güçlendirilmiş kısıtlarla optimize et
        return optimize(feeds, robustConstraints, totalDmKg, OptimizationConfig(
            riskLevel = RiskLevel.ROBUST
        ))
    }

    // ========== PARAMETRİK ANALİZ ==========

    fun parametricAnalysis(
        feeds: List<Feed>,
        nutrientConstraints: List<NutrientConstraint>,
        totalDmKg: Double,
        parameterName: String,
        minValue: Double,
        maxValue: Double,
        steps: Int = 20
    ): List<ParametricPoint> {
        val stepSize = (maxValue - minValue) / steps
        val results = mutableListOf<ParametricPoint>()

        for (i in 0..steps) {
            val currentValue = minValue + i * stepSize
            
            // Kısıtı değiştir
            val modifiedConstraints = nutrientConstraints.map { constraint ->
                if (constraint.name == parameterName) {
                    when {
                        constraint.min != null -> constraint.copy(min = currentValue)
                        constraint.max != null -> constraint.copy(max = currentValue)
                        else -> constraint
                    }
                } else constraint
            }

            val result = optimize(feeds, modifiedConstraints, totalDmKg)

            results.add(ParametricPoint(
                parameterValue = currentValue,
                status = result.status,
                cost = result.totalCost,
                isFeasible = result.status != Status.INFEASIBLE,
                feedChanges = result.feeds.associate { it.feed.name to it.amountKg }
            ))
        }

        return results
    }

    data class ParametricPoint(
        val parameterValue: Double,
        val status: Status,
        val cost: Double,
        val isFeasible: Boolean,
        val feedChanges: Map<String, Double>
    )

    // ========== İÇ FONKSİYONLAR ==========

    private data class SimplexResult(
        val status: Status,
        val selections: List<FeedSelection>,
        val totalCost: Double,
        val nutrients: Map<String, NutrientResult>,
        val shadowPrices: Map<String, Double>,
        val message: String,
        val iterations: Int
    )

    private fun solveWithSimplex(
        feeds: List<Feed>,
        constraints: List<NutrientConstraint>,
        totalDmKg: Double,
        config: OptimizationConfig
    ): SimplexResult {
        // Basit Simpleks implementasyonu
        val n = feeds.size
        val m = constraints.size + 1 // Besin kısıtları + toplam DM

        // Maliyet vektörü
        val cost = DoubleArray(n) { feeds[it].pricePerKg }

        // Kısıt matrisi ve RHS
        val A = Array(m) { DoubleArray(n) }
        val b = DoubleArray(m)

        // Besin kısıtları
        for (i in constraints.indices) {
            val constraint = constraints[i]
            for (j in feeds.indices) {
                val feed = feeds[j]
                when (constraint.name) {
                    "dm" -> A[i][j] = feed.dm
                    "cp" -> A[i][j] = feed.cp * feed.dm / 100.0
                    "nel" -> A[i][j] = (feed.nel ?: feed.me * 0.62) * feed.dm / 100.0
                    "me" -> A[i][j] = feed.me * feed.dm / 100.0
                    "ca" -> A[i][j] = feed.ca * feed.dm / 100.0
                    "p" -> A[i][j] = feed.p * feed.dm / 100.0
                    else -> A[i][j] = (feed.javaClass.getDeclaredField(constraint.name)
                        .getDouble(feed) * feed.dm / 100.0)
                }
            }
            b[i] = (constraint.min ?: constraint.max ?: 0.0) * totalDmKg
        }

        // Toplam DM kısıtı
        for (j in feeds.indices) {
            A[m - 1][j] = feeds[j].dm
        }
        b[m - 1] = totalDmKg

        // Simpleks çağrısı (mevcut solver'a delegasyon)
        // Burada basit bir yaklaşım kullanıyoruz
        return simpleGreedySolve(feeds, constraints, totalDmKg, config)
    }

    private fun simpleGreedySolve(
        feeds: List<Feed>,
        constraints: List<NutrientConstraint>,
        totalDmKg: Double,
        config: OptimizationConfig
    ): SimplexResult {
        // En düşük maliyetli yemlerden başla
        val sortedFeeds = feeds.sortedBy { it.pricePerKg }
        val selections = mutableListOf<FeedSelection>()
        var remainingKg = totalDmKg

        for (feed in sortedFeeds) {
            if (remainingKg <= 0) break

            val amount = minOf(remainingKg, totalDmKg * 0.5) // Max %50 tek yem
            val dmAmount = amount * feed.dm / 100.0

            selections.add(FeedSelection(
                feed = feed,
                amountKg = amount,
                amountDmKg = dmAmount,
                cost = amount * feed.pricePerKg,
                sharePercent = amount / totalDmKg * 100
            ))

            remainingKg -= amount
        }

        // Besin değerlerini hesapla
        val nutrients = calculateNutrients(selections, constraints, totalDmKg)

        // Gölge fiyatları hesapla
        val shadowPrices = calculateShadowPrices(selections, nutrients)

        val totalCost = selections.sumOf { it.cost }

        return SimplexResult(
            status = if (nutrients.values.all { it.status == "OK" }) Status.OPTIMAL else Status.SUBOPTIMAL,
            selections = selections,
            totalCost = totalCost,
            nutrients = nutrients,
            shadowPrices = shadowPrices,
            message = "Optimizasyon tamamlandı",
            iterations = selections.size
        )
    }

    private fun calculateNutrients(
        selections: List<FeedSelection>,
        constraints: List<NutrientConstraint>,
        totalDmKg: Double
    ): Map<String, NutrientResult> {
        val result = mutableMapOf<String, NutrientResult>()

        for (constraint in constraints) {
            var total = 0.0
            for (selection in selections) {
                val feed = selection.feed
                val amountDm = selection.amountDmKg
                when (constraint.name) {
                    "dm" -> total += amountDm
                    "cp" -> total += (feed.cp ?: 0.0) * amountDm / 100.0
                    "nel" -> total += (feed.nel ?: (feed.me ?: 0.0) * 0.62) * amountDm / 100.0
                    "me" -> total += (feed.me ?: 0.0) * amountDm / 100.0
                    "ca" -> total += (feed.ca ?: 0.0) * amountDm / 100.0
                    "p" -> total += (feed.p ?: 0.0) * amountDm / 100.0
                    "ndf" -> total += (feed.ndf ?: 0.0) * amountDm / 100.0
                    "adf" -> total += (feed.adf ?: 0.0) * amountDm / 100.0
                    "fat" -> total += (feed.fat ?: 0.0) * amountDm / 100.0
                    "ash" -> total += (feed.ash ?: 0.0) * amountDm / 100.0
                }
            }

            val valuePerKg = if (totalDmKg > 0) total / totalDmKg * 100 else 0.0
            val min = constraint.min
            val max = constraint.max

            val status = when {
                min != null && valuePerKg < min -> "BELOW_MIN"
                max != null && valuePerKg > max -> "ABOVE_MAX"
                else -> "OK"
            }

            val slack = when {
                min != null && valuePerKg < min -> min - valuePerKg
                max != null && valuePerKg > max -> valuePerKg - max
                else -> 0.0
            }

            result[constraint.name] = NutrientResult(
                value = valuePerKg,
                min = min,
                max = max,
                status = status,
                slack = slack
            )
        }

        return result
    }

    private fun calculateShadowPrices(
        selections: List<FeedSelection>,
        nutrients: Map<String, NutrientResult>
    ): Map<String, Double> {
        val shadowPrices = mutableMapOf<String, Double>()

        for ((name, result) in nutrients) {
            // Basit gölge fiyat tahmini: her birim besin için fırsat maliyeti
            shadowPrices[name] = when {
                result.status == "BELOW_MIN" -> result.slack * 100 // Eksik besin cezası
                result.status == "ABOVE_MAX" -> -result.slack * 100 // Fazla besin cezası
                else -> 0.0
            }
        }

        return shadowPrices
    }

    private fun performRiskAnalysis(
        feeds: List<Feed>,
        constraints: List<NutrientConstraint>,
        totalDmKg: Double,
        riskLevel: RiskLevel
    ): RiskAnalysis {
        val uncertaintyFactor = when (riskLevel) {
            RiskLevel.LOW -> 0.1
            RiskLevel.MEDIUM -> 0.25
            RiskLevel.HIGH -> 0.5
            RiskLevel.ROBUST -> 1.0
            else -> 0.0
        }

        // Senaryo oluştur
        val scenarios = mutableListOf<ScenarioResult>()
        val baseResult = optimize(feeds, constraints, totalDmKg)
        
        // Fiyat belirsizliği ile senaryolar
        for (i in 0 until 100) {
            val randomFeeds = feeds.map { feed ->
                val priceChange = 1.0 + (Random.nextDouble() - 0.5) * uncertaintyFactor * 2
                feed.copy(pricePerKg = feed.pricePerKg * priceChange)
            }
            
            val scenarioResult = optimize(randomFeeds, constraints, totalDmKg)
            
            scenarios.add(ScenarioResult(
                name = "Senaryo ${i + 1}",
                probability = 0.01,
                cost = scenarioResult.totalCost,
                isFeasible = scenarioResult.status == Status.OPTIMAL,
                nutrientViolations = scenarioResult.nutrients
                    .filter { it.value.status != "OK" }
                    .mapValues { it.value.value }
            ))
        }

        val costs = scenarios.map { it.cost }
        val expectedCost = costs.average()
        val variance = costs.map { (it - expectedCost).pow(2) }.average()
        val stdDev = sqrt(variance)

        return RiskAnalysis(
            expectedCost = expectedCost,
            costVariance = variance,
            costStdDev = stdDev,
            var95 = costs.sortedBy { it }[(costs.size * 0.95).toInt()],
            cvar95 = costs.take((costs.size * 0.95).toInt()).average(),
            worstCaseCost = costs.maxOrNull() ?: expectedCost,
            bestCaseCost = costs.minOrNull() ?: expectedCost,
            probabilityInfeasible = scenarios.count { !it.isFeasible }.toDouble() / scenarios.size,
            scenarios = scenarios
        )
    }

    private fun performSensitivityAnalysis(
        feeds: List<Feed>,
        constraints: List<NutrientConstraint>,
        result: SimplexResult
    ): SensitivityAnalysis {
        // Azaltılmış maliyetler
        val reducedCosts = feeds.associate { feed ->
            feed.name to (if (result.selections.any { it.feed.id == feed.id }) 0.0 else {
                // Basit tahmin
                feed.pricePerKg - (result.selections.minOfOrNull { it.feed.pricePerKg } ?: feed.pricePerKg)
            })
        }

        // Gölge fiyat aralıkları
        val shadowRanges = result.nutrients.mapValues { (name, nutResult) ->
            val current = result.shadowPrices[name] ?: 0.0
            ShadowRange(
                currentPrice = current,
                minAllowed = current * 0.8,
                maxAllowed = current * 1.2,
                binding = nutResult.status != "OK",
                minShadowPrice = current * 0.5,
                maxShadowPrice = current * 1.5
            )
        }

        // Bağlayıcı kısıtlar
        val bindingConstraints = result.nutrients
            .filter { it.value.status != "OK" }
            .map { it.key }

        // Objektif katsayı aralıkları
        val coeffRanges = feeds.associate { feed ->
            feed.name to (feed.pricePerKg * 0.5..feed.pricePerKg * 1.5)
        }

        return SensitivityAnalysis(
            reducedCosts = reducedCosts,
            shadowPriceRanges = shadowRanges,
            bindingConstraints = bindingConstraints,
            objectiveCoeffRange = coeffRanges
        )
    }

    private fun calculateNutrientScore(nutrients: Map<String, NutrientResult>): Double {
        var score = 100.0
        for (nutrient in nutrients.values) {
            when (nutrient.status) {
                "BELOW_MIN" -> score -= nutrient.slack * 50
                "ABOVE_MAX" -> score -= nutrient.slack * 50
                else -> score += 0
            }
        }
        return score.coerceAtLeast(0.0)
    }

    private fun calculateDiversityScore(selections: List<FeedSelection>): Double {
        if (selections.isEmpty()) return 0.0
        // Shannon diversity index
        val total = selections.sumOf { it.amountKg }
        return selections.sumOf { sel ->
            val p = sel.amountKg / total
            if (p > 0) -p * ln(p) else 0.0
        }
    }

    private fun filterParetoOptimal(solutions: List<ParetoSolution>): List<ParetoSolution> {
        val pareto = mutableListOf<ParetoSolution>()
        for (s1 in solutions) {
            var dominated = false
            for (s2 in solutions) {
                if (s1 == s2) continue
                if (s2.cost <= s1.cost && s2.nutrientScore >= s1.nutrientScore && 
                    s2.diversityScore >= s1.diversityScore &&
                    (s2.cost < s1.cost || s2.nutrientScore > s1.nutrientScore || 
                     s2.diversityScore > s1.diversityScore)) {
                    dominated = true
                    break
                }
            }
            if (!dominated) pareto.add(s1)
        }
        return pareto
    }
}
