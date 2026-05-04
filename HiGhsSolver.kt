package com.nutripulse.app.data.solver

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.abs

/**
 * NutriPulse — HiGHS Solver Wrapper
 *
 * Features:
 * - LP (continuous amounts)
 * - Nutrient constraints (min/max/target)
 * - Shadow prices / dual values
 * - Multiple rations (multi-blend)
 * - Feed count constraint (MILP)
 * - Degeneracy recovery
 * - Numerical stability (large problems)
 * - Revised Simplex
 * - Parametric analysis
 */
object HiGhsSolver {

    enum class Status {
        OPTIMAL,
        SUBOPTIMAL,
        INFEASIBLE,
        UNBOUNDED,
        TIMEOUT,
        ERROR
    }

    data class Constraint(
        val name: String,
        val min: Double? = null,
        val max: Double? = null,
        val coefficients: Map<String, Double> = emptyMap()
    )

    data class Variable(
        val name: String,
        val cost: Double,
        val lb: Double = 0.0,
        val ub: Double? = null,
        val isInteger: Boolean = false,
        val isBinary: Boolean = false
    )

    data class Solution(
        val status: Status,
        val objectiveValue: Double,
        val variableValues: Map<String, Double>,
        val shadowPrices: Map<String, Double>,
        val reducedCosts: Map<String, Double>,
        val message: String,
        val solveTimeMs: Long,
        val iterations: Int
    )

    data class ParametricResult(
        val parameterValue: Double,
        val status: Status,
        val objectiveValue: Double,
        val variableValues: Map<String, Double>,
        val shadowPrices: Map<String, Double>
    )

    data class MultiBlendResult(
        val status: Status,
        val totalCost: Double,
        val blendSolutions: Map<String, Solution>,
        val message: String
    )

    data class SolverConfig(
        val method: Method = Method.REVISED_SIMPLEX,
        val timeLimitSec: Int = 30,
        val tolerance: Double = 1e-7,
        val enablePresolve: Boolean = true,
        val enableParallel: Boolean = true,
        val maxIterations: Int = 10000,
        val degeneracyRecovery: Boolean = true,
        val numericalScaling: Boolean = true
    )

    enum class Method {
        REVISED_SIMPLEX,
        IPM,
        PDLP
    }

    private const val BRIDGE_SCRIPT = "scripts/highs_solver/highs_bridge.py"

    private fun findBridgeScript(): String {
        // Try relative path first
        var script = File(BRIDGE_SCRIPT)
        if (script.exists()) return script.absolutePath
        
        // Try absolute paths
        val possiblePaths = listOf(
            "/storage/internal_new/project/NutriPulse/scripts/highs_solver/highs_bridge.py",
            "/storage/emulated/0/NutriPulse/scripts/highs_solver/highs_bridge.py",
            File(System.getProperty("user.dir"), BRIDGE_SCRIPT).absolutePath,
            File(System.getProperty("user.home"), "NutriPulse/$BRIDGE_SCRIPT").absolutePath
        )
        
        for (path in possiblePaths) {
            script = File(path)
            if (script.exists()) return script.absolutePath
        }
        
        // Return original so error message is meaningful
        return BRIDGE_SCRIPT
    }

    fun solve(
        variables: List<Variable>,
        constraints: List<Constraint>,
        config: SolverConfig = SolverConfig()
    ): Solution {
        return try {
            val payload = buildLpPayload(variables, constraints, config)
            val result = callBridge(payload)
            parseSolution(result)
        } catch (e: Exception) {
            Solution(
                status = Status.ERROR,
                objectiveValue = 0.0,
                variableValues = emptyMap(),
                shadowPrices = emptyMap(),
                reducedCosts = emptyMap(),
                message = "HiGHS solve error: ${e.message}",
                solveTimeMs = 0,
                iterations = 0
            )
        }
    }

    fun solveMILP(
        variables: List<Variable>,
        constraints: List<Constraint>,
        maxFeedCount: Int? = null,
        config: SolverConfig = SolverConfig()
    ): Solution {
        return try {
            val payload = buildMilpPayload(variables, constraints, maxFeedCount, config)
            val result = callBridge(payload)
            parseSolution(result)
        } catch (e: Exception) {
            Solution(
                status = Status.ERROR,
                objectiveValue = 0.0,
                variableValues = emptyMap(),
                shadowPrices = emptyMap(),
                reducedCosts = emptyMap(),
                message = "HiGHS MILP solve error: ${e.message}",
                solveTimeMs = 0,
                iterations = 0
            )
        }
    }

    fun solveMultipleRations(
        formulas: List<FormulaSpec>,
        sharedVariables: List<Variable>,
        config: SolverConfig = SolverConfig()
    ): MultiBlendResult {
        return try {
            val payload = buildMultiBlendPayload(formulas, sharedVariables, config)
            val result = callBridge(payload)
            parseMultiBlendResult(result)
        } catch (e: Exception) {
            MultiBlendResult(
                status = Status.ERROR,
                totalCost = 0.0,
                blendSolutions = emptyMap(),
                message = "HiGHS multi-blend error: ${e.message}"
            )
        }
    }

    fun parametricAnalysis(
        variables: List<Variable>,
        constraints: List<Constraint>,
        paramName: String,
        minValue: Double,
        maxValue: Double,
        steps: Int = 20,
        config: SolverConfig = SolverConfig()
    ): List<ParametricResult> {
        val results = mutableListOf<ParametricResult>()
        val stepSize = (maxValue - minValue) / steps

        for (i in 0..steps) {
            val currentValue = minValue + i * stepSize
            val modifiedConstraints = constraints.map { c ->
                if (c.name == paramName) {
                    c.copy(
                        min = c.min?.let { currentValue },
                        max = c.max?.let { currentValue }
                    )
                } else c
            }

            val solution = solve(variables, modifiedConstraints, config)
            results.add(
                ParametricResult(
                    parameterValue = currentValue,
                    status = solution.status,
                    objectiveValue = solution.objectiveValue,
                    variableValues = solution.variableValues,
                    shadowPrices = solution.shadowPrices
                )
            )
        }

        return results
    }

    private fun buildLpPayload(
        variables: List<Variable>,
        constraints: List<Constraint>,
        config: SolverConfig
    ): JSONObject {
        return JSONObject().apply {
            put("command", "lp")
            put("variables", JSONArray(variables.map { v ->
                JSONObject().apply {
                    put("name", v.name)
                    put("cost", v.cost)
                    put("lb", v.lb)
                    if (v.ub != null) put("ub", v.ub)
                }
            }))
            put("constraints", JSONArray(constraints.map { c ->
                JSONObject().apply {
                    put("name", c.name)
                    if (c.min != null) put("min", c.min)
                    if (c.max != null) put("max", c.max)
                    put("coefficients", JSONObject(c.coefficients))
                }
            }))
            put("config", JSONObject().apply {
                put("method", config.method.name.lowercase())
                put("time_limit", config.timeLimitSec)
                put("tolerance", config.tolerance)
                put("presolve", config.enablePresolve)
                put("parallel", config.enableParallel)
                put("max_iterations", config.maxIterations)
                put("degeneracy_recovery", config.degeneracyRecovery)
                put("numerical_scaling", config.numericalScaling)
            })
        }
    }

    private fun buildMilpPayload(
        variables: List<Variable>,
        constraints: List<Constraint>,
        maxFeedCount: Int?,
        config: SolverConfig
    ): JSONObject {
        return JSONObject().apply {
            put("command", "milp")
            put("variables", JSONArray(variables.map { v ->
                JSONObject().apply {
                    put("name", v.name)
                    put("cost", v.cost)
                    put("lb", v.lb)
                    if (v.ub != null) put("ub", v.ub)
                    put("is_integer", v.isInteger)
                    put("is_binary", v.isBinary)
                }
            }))
            put("constraints", JSONArray(constraints.map { c ->
                JSONObject().apply {
                    put("name", c.name)
                    if (c.min != null) put("min", c.min)
                    if (c.max != null) put("max", c.max)
                    put("coefficients", JSONObject(c.coefficients))
                }
            }))
            if (maxFeedCount != null) {
                put("max_feed_count", maxFeedCount)
            }
            put("config", JSONObject().apply {
                put("method", config.method.name.lowercase())
                put("time_limit", config.timeLimitSec)
                put("tolerance", config.tolerance)
                put("presolve", config.enablePresolve)
                put("parallel", config.enableParallel)
                put("max_iterations", config.maxIterations)
                put("degeneracy_recovery", config.degeneracyRecovery)
                put("numerical_scaling", config.numericalScaling)
            })
        }
    }

    private fun buildMultiBlendPayload(
        formulas: List<FormulaSpec>,
        sharedVariables: List<Variable>,
        config: SolverConfig
    ): JSONObject {
        return JSONObject().apply {
            put("command", "multi_blend")
            put("formulas", JSONArray(formulas.map { f ->
                JSONObject().apply {
                    put("name", f.name)
                    put("variables", JSONArray(f.variables.map { v ->
                        JSONObject().apply {
                            put("name", v.name)
                            put("cost", v.cost)
                            put("lb", v.lb)
                            if (v.ub != null) put("ub", v.ub)
                        }
                    }))
                    put("constraints", JSONArray(f.constraints.map { c ->
                        JSONObject().apply {
                            put("name", c.name)
                            if (c.min != null) put("min", c.min)
                            if (c.max != null) put("max", c.max)
                            put("coefficients", JSONObject(c.coefficients))
                        }
                    }))
                }
            }))
            put("shared_variables", JSONArray(sharedVariables.map { v ->
                JSONObject().apply {
                    put("name", v.name)
                    put("cost", v.cost)
                    put("lb", v.lb)
                    if (v.ub != null) put("ub", v.ub)
                }
            }))
            put("config", JSONObject().apply {
                put("method", config.method.name.lowercase())
                put("time_limit", config.timeLimitSec)
                put("tolerance", config.tolerance)
                put("presolve", config.enablePresolve)
                put("parallel", config.enableParallel)
                put("degeneracy_recovery", config.degeneracyRecovery)
                put("numerical_scaling", config.numericalScaling)
            })
        }
    }

    private fun callBridge(payload: JSONObject): JSONObject {
        val scriptPath = findBridgeScript()
        val script = File(scriptPath)
        if (!script.exists()) {
            throw IllegalStateException("HiGHS bridge script not found at $scriptPath (tried: $BRIDGE_SCRIPT)")
        }

        val process = ProcessBuilder("python3", script.absolutePath)
            .redirectErrorStream(true)
            .start()

        process.outputStream.bufferedWriter().use { out ->
            out.write(payload.toString())
        }

        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            throw IllegalStateException("HiGHS bridge failed with exit code $exitCode: $output")
        }

        return JSONObject(output)
    }

    private fun parseSolution(json: JSONObject): Solution {
        val statusText = json.optString("status", "ERROR")
        val status = when {
            statusText.equals("Optimal", ignoreCase = true) -> Status.OPTIMAL
            statusText.contains("Infeasible", ignoreCase = true) -> Status.INFEASIBLE
            statusText.contains("Unbounded", ignoreCase = true) -> Status.UNBOUNDED
            statusText.contains("Timeout", ignoreCase = true) -> Status.TIMEOUT
            statusText.contains("Error", ignoreCase = true) -> Status.ERROR
            else -> Status.SUBOPTIMAL
        }

        val objectiveValue = json.optDouble("objective", 0.0)

        val variableValues = mutableMapOf<String, Double>()
        val varArray = json.optJSONArray("variable_values") ?: JSONArray()
        for (i in 0 until varArray.length()) {
            val obj = varArray.getJSONObject(i)
            variableValues[obj.getString("name")] = obj.getDouble("value")
        }

        val shadowPrices = mutableMapOf<String, Double>()
        val shadowObj = json.optJSONObject("shadow_prices") ?: JSONObject()
        val shadowKeys = shadowObj.keys()
        while (shadowKeys.hasNext()) {
            val key = shadowKeys.next()
            shadowPrices[key] = shadowObj.getDouble(key)
        }

        val reducedCosts = mutableMapOf<String, Double>()
        val rcObj = json.optJSONObject("reduced_costs") ?: JSONObject()
        val rcKeys = rcObj.keys()
        while (rcKeys.hasNext()) {
            val key = rcKeys.next()
            reducedCosts[key] = rcObj.getDouble(key)
        }

        val message = json.optString("message", "")
        val solveTimeMs = json.optLong("solve_time_ms", 0)
        val iterations = json.optInt("iterations", 0)

        return Solution(
            status = status,
            objectiveValue = objectiveValue,
            variableValues = variableValues,
            shadowPrices = shadowPrices,
            reducedCosts = reducedCosts,
            message = message,
            solveTimeMs = solveTimeMs,
            iterations = iterations
        )
    }

    private fun parseMultiBlendResult(json: JSONObject): MultiBlendResult {
        val statusText = json.optString("status", "ERROR")
        val status = when {
            statusText.equals("Optimal", ignoreCase = true) -> Status.OPTIMAL
            statusText.contains("Infeasible", ignoreCase = true) -> Status.INFEASIBLE
            statusText.contains("Unbounded", ignoreCase = true) -> Status.UNBOUNDED
            statusText.contains("Timeout", ignoreCase = true) -> Status.TIMEOUT
            statusText.contains("Error", ignoreCase = true) -> Status.ERROR
            else -> Status.SUBOPTIMAL
        }

        val totalCost = json.optDouble("total_cost", 0.0)
        val message = json.optString("message", "")

        val blendSolutions = mutableMapOf<String, Solution>()
        val blendsObj = json.optJSONObject("blends") ?: JSONObject()
        val blendKeys = blendsObj.keys()
        while (blendKeys.hasNext()) {
            val key = blendKeys.next()
            val blendJson = blendsObj.getJSONObject(key)
            blendSolutions[key] = parseSolution(blendJson)
        }

        return MultiBlendResult(
            status = status,
            totalCost = totalCost,
            blendSolutions = blendSolutions,
            message = message
        )
    }

    data class FormulaSpec(
        val name: String,
        val variables: List<Variable>,
        val constraints: List<Constraint>
    )
}
