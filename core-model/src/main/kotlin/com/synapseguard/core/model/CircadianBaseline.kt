package com.synapseguard.core.model

import kotlinx.serialization.Serializable

/**
 * Estadísticos robustos de la línea base móvil de 14 días (Mediana + MAD) para una métrica,
 * estratificados por bloque horario circadiano y tipo de día (laboral vs. fin de semana).
 */
@Serializable
data class CircadianBaseline(
    val metricKey: String,
    val circadianBlock: CircadianBlock,
    val isWeekend: Boolean,
    val medianValue: Double,
    val madValue: Double,
    val sampleCount: Int,
    val updatedAtEpochMs: Long
) {
    init {
        require(metricKey.isNotBlank()) { "metricKey no puede estar vacío." }
        require(madValue >= 0.0) { "madValue no puede ser negativo: $madValue" }
        require(sampleCount >= 0) { "sampleCount no puede ser negativo: $sampleCount" }
        require(updatedAtEpochMs > 0L) { "updatedAtEpochMs debe ser positivo." }
    }
}

