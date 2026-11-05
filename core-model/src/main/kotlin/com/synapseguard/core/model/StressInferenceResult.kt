package com.synapseguard.core.model

import kotlinx.serialization.Serializable

/**
 * Factor explicable individual que contribuyó a disparar la probabilidad de estrés en la ventana actual.
 */
@Serializable
data class StressFactorContribution(
    val metricKey: String,
    val zScore: Double,
    val weightContribution: Double,
    val humanReadableLabelEs: String
) {
    init {
        require(metricKey.isNotBlank()) { "metricKey no puede estar vacío." }
        require(zScore in -10.0..10.0) { "zScore fuera de límites razonables [-10.0, 10.0]: $zScore" }
        require(humanReadableLabelEs.isNotBlank()) { "humanReadableLabelEs no puede estar vacío." }
    }
}

/**
 * Resultado inmutable del motor de Inferencia Multimodal (Fusión Tardía ponderada por calidad Q).
 */
@Serializable
data class StressInferenceResult(
    val inferenceId: String,
    val windowId: Long,
    val evaluatedAtEpochMs: Long,
    val physiologicalProbability: Double?,
    val behavioralProbability: Double,
    val biometricSignalQuality: Double,
    val appliedPhysioWeight: Double,
    val appliedBehavioralWeight: Double,
    val finalStressProbability: Double,
    val inhibitedByPhysicalActivity: Boolean,
    val topFactor1: StressFactorContribution,
    val topFactor2: StressFactorContribution? = null,
    val explanationSummaryEs: String
) {
    init {
        require(inferenceId.isNotBlank()) { "inferenceId no puede estar vacío." }
        require(windowId > 0L) { "windowId debe ser positivo." }
        require(evaluatedAtEpochMs > 0L) { "evaluatedAtEpochMs debe ser positivo." }
        physiologicalProbability?.let {
            require(it in 0.0..1.0) { "physiologicalProbability debe estar en [0.0, 1.0]: $it" }
        }
        require(behavioralProbability in 0.0..1.0) {
            "behavioralProbability debe estar en [0.0, 1.0]: $behavioralProbability"
        }
        require(biometricSignalQuality in 0.0..1.0) {
            "biometricSignalQuality debe estar en [0.0, 1.0]: $biometricSignalQuality"
        }
        require(appliedPhysioWeight in 0.0..1.0) {
            "appliedPhysioWeight debe estar en [0.0, 1.0]: $appliedPhysioWeight"
        }
        require(appliedBehavioralWeight in 0.0..1.0) {
            "appliedBehavioralWeight debe estar en [0.0, 1.0]: $appliedBehavioralWeight"
        }
        require(finalStressProbability in 0.0..1.0) {
            "finalStressProbability debe estar en [0.0, 1.0]: $finalStressProbability"
        }
        require(explanationSummaryEs.isNotBlank()) { "explanationSummaryEs no puede estar vacío." }
    }

    /**
     * Evalúa si el resultado supera el umbral configurado por el usuario y no está inhibido por ejercicio.
     */
    fun shouldTriggerJitai(threshold: Double = DEFAULT_JITAI_THRESHOLD): Boolean {
        require(threshold in 0.0..1.0) { "El umbral debe estar en [0.0, 1.0]: $threshold" }
        return !inhibitedByPhysicalActivity && finalStressProbability >= threshold
    }

    companion object {
        const val DEFAULT_JITAI_THRESHOLD: Double = 0.75
    }
}

