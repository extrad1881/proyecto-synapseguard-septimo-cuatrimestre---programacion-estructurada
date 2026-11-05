package com.synapseguard.core.model

import kotlinx.serialization.Serializable

/**
 * Vector inmutable de características agregadas en una ventana temporal (por defecto 15 o 60 minutos).
 * Combina señales biométricas, telemetría de pantalla, cinemática táctil Zero-Keylogging y contexto físico.
 */
@Serializable
data class FeatureWindow15m(
    val windowId: Long,
    val windowDurationMinutes: Int = 15,
    val circadianBlock: CircadianBlock,
    val isWeekend: Boolean,
    val activityState: ActivityState,

    // --- Rama A: Señales Fisiológicas (Opcionales según disponibilidad de Wearable o Cámara PPG) ---
    val rmssdMs: Double? = null,
    val restingHrBpm: Double? = null,
    val sedentaryHrDeltaBpm: Double? = null,
    val sleepDebtMinutes: Double? = null,
    val sleepFragmentationRatio: Double? = null,
    val edaPeaksPerMinute: Double? = null,
    val biometricValidBeatRatio: Double = 0.0,
    val minutesSinceLastBiometricSync: Double? = null,

    // --- Rama B: Telemetría de Uso y Fragmentación Atencional ---
    val unlockCount: Int = 0,
    val microSessionRatio: Double = 0.0,
    val appSwitchCount: Int = 0,
    val appSwitchEntropy: Double = 0.0,
    val doomscrollStreakMinutes: Double = 0.0,
    val nightScreenMinutes: Double = 0.0,

    // --- Rama B: Cinemática Táctil Zero-Keylogging (Opcionales si se usa Fallback sin Accesibilidad) ---
    val ikiMedianMs: Double? = null,
    val ikiCoefficientOfVariation: Double? = null,
    val backspaceRatio: Double? = null,
    val scrollVelocityPxPerSec: Double? = null,

    // --- Compuerta Física y Micro-Temblor Postural ---
    val svmMeanG: Double = 0.0,
    val stepCount: Int = 0,
    val inHandGripTremorPower: Double = 0.0
) {
    init {
        require(windowId > 0L) { "windowId debe ser un epoch timestamp positivo." }
        require(windowDurationMinutes == 15 || windowDurationMinutes == 60) {
            "windowDurationMinutes debe ser 15 o 60 minutos, recibido: $windowDurationMinutes"
        }
        rmssdMs?.let {
            require(it in 5.0..350.0) { "rmssdMs fuera del rango fisiológico [5.0, 350.0]: $it" }
        }
        restingHrBpm?.let {
            require(it in 30.0..220.0) { "restingHrBpm fuera del rango fisiológico [30.0, 220.0]: $it" }
        }
        sleepDebtMinutes?.let {
            require(it >= 0.0) { "sleepDebtMinutes no puede ser negativo: $it" }
        }
        sleepFragmentationRatio?.let {
            require(it in 0.0..1.0) { "sleepFragmentationRatio debe estar en [0.0, 1.0]: $it" }
        }
        edaPeaksPerMinute?.let {
            require(it >= 0.0) { "edaPeaksPerMinute no puede ser negativo: $it" }
        }
        require(biometricValidBeatRatio in 0.0..1.0) {
            "biometricValidBeatRatio debe estar en [0.0, 1.0]: $biometricValidBeatRatio"
        }
        minutesSinceLastBiometricSync?.let {
            require(it >= 0.0) { "minutesSinceLastBiometricSync no puede ser negativo: $it" }
        }
        require(unlockCount >= 0) { "unlockCount no puede ser negativo: $unlockCount" }
        require(microSessionRatio in 0.0..1.0) {
            "microSessionRatio debe estar en [0.0, 1.0]: $microSessionRatio"
        }
        require(appSwitchCount >= 0) { "appSwitchCount no puede ser negativo: $appSwitchCount" }
        require(appSwitchEntropy >= 0.0) { "appSwitchEntropy no puede ser negativo: $appSwitchEntropy" }
        require(doomscrollStreakMinutes >= 0.0) {
            "doomscrollStreakMinutes no puede ser negativo: $doomscrollStreakMinutes"
        }
        require(nightScreenMinutes >= 0.0) {
            "nightScreenMinutes no puede ser negativo: $nightScreenMinutes"
        }
        ikiMedianMs?.let {
            require(it > 0.0) { "ikiMedianMs debe ser mayor que 0: $it" }
        }
        ikiCoefficientOfVariation?.let {
            require(it >= 0.0) { "ikiCoefficientOfVariation no puede ser negativo: $it" }
        }
        backspaceRatio?.let {
            require(it in 0.0..1.0) { "backspaceRatio debe estar en [0.0, 1.0]: $it" }
        }
        scrollVelocityPxPerSec?.let {
            require(it >= 0.0) { "scrollVelocityPxPerSec no puede ser negativo: $it" }
        }
        require(svmMeanG >= 0.0) { "svmMeanG no puede ser negativo: $svmMeanG" }
        require(stepCount >= 0) { "stepCount no puede ser negativo: $stepCount" }
        require(inHandGripTremorPower >= 0.0) {
            "inHandGripTremorPower no puede ser negativo: $inHandGripTremorPower"
        }
    }

    /** Indica si la ventana cuenta con al menos un marcador autonómico primario (RMSSD o pulso basal/delta). */
    val hasBiometricSignal: Boolean
        get() = (rmssdMs != null || restingHrBpm != null || sedentaryHrDeltaBpm != null) &&
            biometricValidBeatRatio > 0.0

    /** Indica si la ventana puede alimentar el cálculo de la línea base móvil de 14 días (solo en reposo). */
    val isEligibleForBaselineCalibration: Boolean
        get() = activityState == ActivityState.REST
}

