package com.synapseguard.core.model

import kotlinx.serialization.Serializable

/**
 * Resumen diario compactado para preservación histórica de largo plazo tras aplicar
 * la política de retención TTL (90 días) sobre las ventanas granulares de 15 minutos.
 */
@Serializable
data class DailySummary(
    /** Día calendario expresado como días desde Epoch (UTC/Local floor day). */
    val dayEpochDate: Long,
    val meanRmssdMs: Double?,
    val meanRestingHrBpm: Double?,
    val totalUnlocks: Int,
    val totalAppSwitches: Int,
    val totalDoomscrollMinutes: Double,
    val meanStressProbability: Double,
    val interventionsTriggered: Int,
    val interventionsCompleted: Int
) {
    init {
        require(dayEpochDate >= 0L) { "dayEpochDate no puede ser negativo." }
        require(totalUnlocks >= 0) { "totalUnlocks no puede ser negativo." }
        require(totalAppSwitches >= 0) { "totalAppSwitches no puede ser negativo." }
        require(totalDoomscrollMinutes >= 0.0) { "totalDoomscrollMinutes no puede ser negativo." }
        require(meanStressProbability in 0.0..1.0) {
            "meanStressProbability debe estar en [0.0, 1.0]: $meanStressProbability"
        }
        require(interventionsTriggered >= 0) { "interventionsTriggered no puede ser negativo." }
        require(interventionsCompleted in 0..interventionsTriggered) {
            "interventionsCompleted ($interventionsCompleted) debe estar entre 0 y interventionsTriggered ($interventionsTriggered)."
        }
    }
}

