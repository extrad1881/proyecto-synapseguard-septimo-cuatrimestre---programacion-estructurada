package com.synapseguard.core.model

import kotlinx.serialization.Serializable

/**
 * Modalidades de intervención adaptativa (Just-In-Time Adaptive Intervention) ejecutadas localmente.
 */
@Serializable
enum class InterventionType(val displayNameEs: String) {
    /** Pausa respiratoria consciente de 8s en Overlay antes de continuar en apps de doomscrolling. */
    MIND_FRICTION_OVERLAY_8S("Escudo de Fricción Consciente (8s)"),

    /** Guía háptica de suspiro fisiológico o frecuencia de resonancia vagal (6 rpm). */
    HAPTIC_VAGAL_BIOFEEDBACK("Biofeedback Vagal Háptico"),

    /** Retención temporal de notificaciones secundarias durante sobrecarga atencional. */
    SMART_NOTIFICATION_BATCHING("Agrupamiento Inteligente de Notificaciones"),

    /** Atenuación circadiana nocturna y cierre guiado de jornada. */
    CIRCADIAN_NIGHT_SHIELD("Protector Circadiano Nocturno")
}

/**
 * Respuesta conductual del usuario frente a una intervención JITAI.
 */
@Serializable
enum class InterventionOutcome {
    COMPLETED,
    SNOOZED,
    DISMISSED_IMMEDIATELY,
    EXPIRED
}

/**
 * Registro inmutable de una intervención adaptativa lanzada y su efectividad autonómica/conductual.
 */
@Serializable
data class JitaiIntervention(
    val interventionId: String,
    val inferenceId: String,
    val windowId: Long,
    val type: InterventionType,
    val triggeredAtEpochMs: Long,
    val outcome: InterventionOutcome,
    val preInterventionRmssdMs: Double? = null,
    val postInterventionRmssdMs: Double? = null,
    val banditReward: Double
) {
    init {
        require(interventionId.isNotBlank()) { "interventionId no puede estar vacío." }
        require(inferenceId.isNotBlank()) { "inferenceId no puede estar vacío." }
        require(windowId > 0L) { "windowId debe ser positivo." }
        require(triggeredAtEpochMs > 0L) { "triggeredAtEpochMs debe ser positivo." }
        preInterventionRmssdMs?.let {
            require(it in 5.0..350.0) { "preInterventionRmssdMs fuera de rango [5.0, 350.0]: $it" }
        }
        postInterventionRmssdMs?.let {
            require(it in 5.0..350.0) { "postInterventionRmssdMs fuera de rango [5.0, 350.0]: $it" }
        }
        require(banditReward in -1.0..1.0) {
            "banditReward debe estar acotado en [-1.0, 1.0]: $banditReward"
        }
    }

    /** Diferencia neta de recuperación vagal (en milisegundos de RMSSD) tras finalizar la intervención. */
    val rmssdRecoveryDeltaMs: Double?
        get() = if (preInterventionRmssdMs != null && postInterventionRmssdMs != null) {
            postInterventionRmssdMs - preInterventionRmssdMs
        } else {
            null
        }
}

