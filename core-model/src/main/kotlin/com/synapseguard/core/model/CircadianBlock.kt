package com.synapseguard.core.model

import kotlinx.serialization.Serializable

/**
 * Bloques horarios circadianos para estratificar la línea base intraindividual (Mediana + MAD).
 * Evita comparar métricas autonómicas matutinas con lecturas nocturnas.
 */
@Serializable
enum class CircadianBlock(
    val id: Int,
    val startHourInclusive: Int,
    val endHourInclusive: Int,
    val displayNameEs: String
) {
    /** 06:00 a 11:59 - Activación matutina por respuesta de cortisol al despertar (CAR). */
    MORNING(id = 1, startHourInclusive = 6, endHourInclusive = 11, displayNameEs = "Mañana (06:00–11:59)"),

    /** 12:00 a 17:59 - Meseta diurna de actividad cognitiva y laboral. */
    AFTERNOON(id = 2, startHourInclusive = 12, endHourInclusive = 17, displayNameEs = "Tarde (12:00–17:59)"),

    /** 18:00 a 22:59 - Transición vespertina y desaceleración parasimpática. */
    EVENING(id = 3, startHourInclusive = 18, endHourInclusive = 22, displayNameEs = "Noche (18:00–22:59)"),

    /** 23:00 a 05:59 - Fase de descanso profundo y vulnerabilidad circadiana. */
    LATE_NIGHT(id = 4, startHourInclusive = 23, endHourInclusive = 5, displayNameEs = "Madrugada (23:00–05:59)");

    companion object {
        /**
         * Resuelve el bloque circadiano correspondiente a una hora del día en formato 24h (0..23).
         */
        fun fromHourOfDay(hourOfDay: Int): CircadianBlock {
            require(hourOfDay in 0..23) {
                "La hora del día debe estar en el rango 0..23, pero se recibió: $hourOfDay"
            }
            return when (hourOfDay) {
                in 6..11 -> MORNING
                in 12..17 -> AFTERNOON
                in 18..22 -> EVENING
                else -> LATE_NIGHT
            }
        }

        fun fromId(id: Int): CircadianBlock =
            entries.firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("Identificador de CircadianBlock inválido: $id")
    }
}

