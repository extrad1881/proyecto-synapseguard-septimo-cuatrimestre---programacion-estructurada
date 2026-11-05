package com.synapseguard.core.model

import kotlinx.serialization.Serializable

/**
 * Estado físico determinado por la Compuerta de Contexto (SVM de acelerómetro + pasos + decaimiento EPOC).
 */
@Serializable
enum class ActivityState(
    val inhibitsPhysiologicalStressAlerts: Boolean,
    val descriptionEs: String
) {
    /** Usuario en reposo o estado sedentario; apto para evaluación autonómica y calibración basal. */
    REST(
        inhibitsPhysiologicalStressAlerts = false,
        descriptionEs = "Reposo / Sedentario"
    ),

    /** Locomoción activa o ejercicio detectado en la ventana actual. */
    PHYSICAL_ACTIVITY(
        inhibitsPhysiologicalStressAlerts = true,
        descriptionEs = "Actividad física en curso"
    ),

    /** Ventana de enfriamiento dinámico post-ejercicio (EPOC: 5 a 45 minutos según intensidad). */
    EPOC_COOLDOWN(
        inhibitsPhysiologicalStressAlerts = true,
        descriptionEs = "Recuperación metabólica post-ejercicio (EPOC)"
    )
}

