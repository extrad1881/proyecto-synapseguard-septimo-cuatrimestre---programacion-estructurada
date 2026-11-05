package com.synapseguard.core.model

import kotlinx.serialization.Serializable

/**
 * Respuesta a micro-encuesta de Evaluación Momentánea Ecológica (EMA) en escala Likert de 1 a 5.
 * Utilizada para personalizar los pesos del Meta-Clasificador en el dispositivo (Online SGD).
 */
@Serializable
data class EmaFeedback(
    val emaId: String,
    val windowId: Long,
    val respondedAtEpochMs: Long,
    val perceivedStressScore: Int,
    val activeCircadianBlock: CircadianBlock,
    val usedForOnlineSgdUpdate: Boolean = false
) {
    init {
        require(emaId.isNotBlank()) { "emaId no puede estar vacío." }
        require(windowId > 0L) { "windowId debe ser positivo." }
        require(respondedAtEpochMs > 0L) { "respondedAtEpochMs debe ser positivo." }
        require(perceivedStressScore in MIN_SCORE..MAX_SCORE) {
            "perceivedStressScore debe estar entre $MIN_SCORE y $MAX_SCORE, recibido: $perceivedStressScore"
        }
    }

    /**
     * Convierte la calificación subjetiva (1..5) en una etiqueta de probabilidad suave en [0.0, 1.0]
     * para alimentar el paso de Descenso de Gradiente Estocástico (Online SGD).
     */
    val normalizedTargetProbability: Double
        get() = (perceivedStressScore - MIN_SCORE).toDouble() / (MAX_SCORE - MIN_SCORE).toDouble()

    companion object {
        const val MIN_SCORE: Int = 1
        const val MAX_SCORE: Int = 5
    }
}

