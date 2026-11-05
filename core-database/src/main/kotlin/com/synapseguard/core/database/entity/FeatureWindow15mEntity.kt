package com.synapseguard.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.synapseguard.core.model.ActivityState
import com.synapseguard.core.model.CircadianBlock
import com.synapseguard.core.model.FeatureWindow15m

/**
 * Entidad Room para vectores agregados en ventanas de 15 o 60 minutos.
 * Incluye índice compuesto para acelerar el cálculo de la línea base móvil de 14 días en reposo.
 */
@Entity(
    tableName = "feature_windows_15m",
    indices = [
        Index(
            name = "idx_window_circadian_rest",
            value = ["circadian_block", "is_weekend", "activity_state", "window_id"]
        )
    ]
)
data class FeatureWindow15mEntity(
    @PrimaryKey
    @ColumnInfo(name = "window_id")
    val windowId: Long,

    @ColumnInfo(name = "window_duration_minutes")
    val windowDurationMinutes: Int,

    @ColumnInfo(name = "circadian_block")
    val circadianBlock: CircadianBlock,

    @ColumnInfo(name = "is_weekend")
    val isWeekend: Boolean,

    @ColumnInfo(name = "activity_state")
    val activityState: ActivityState,

    @ColumnInfo(name = "rmssd_ms")
    val rmssdMs: Double?,

    @ColumnInfo(name = "resting_hr_bpm")
    val restingHrBpm: Double?,

    @ColumnInfo(name = "sedentary_hr_delta_bpm")
    val sedentaryHrDeltaBpm: Double?,

    @ColumnInfo(name = "sleep_debt_minutes")
    val sleepDebtMinutes: Double?,

    @ColumnInfo(name = "sleep_fragmentation_ratio")
    val sleepFragmentationRatio: Double?,

    @ColumnInfo(name = "eda_peaks_per_minute")
    val edaPeaksPerMinute: Double?,

    @ColumnInfo(name = "biometric_valid_beat_ratio")
    val biometricValidBeatRatio: Double,

    @ColumnInfo(name = "minutes_since_last_biometric_sync")
    val minutesSinceLastBiometricSync: Double?,

    @ColumnInfo(name = "unlock_count")
    val unlockCount: Int,

    @ColumnInfo(name = "micro_session_ratio")
    val microSessionRatio: Double,

    @ColumnInfo(name = "app_switch_count")
    val appSwitchCount: Int,

    @ColumnInfo(name = "app_switch_entropy")
    val appSwitchEntropy: Double,

    @ColumnInfo(name = "doomscroll_streak_minutes")
    val doomscrollStreakMinutes: Double,

    @ColumnInfo(name = "night_screen_minutes")
    val nightScreenMinutes: Double,

    @ColumnInfo(name = "iki_median_ms")
    val ikiMedianMs: Double?,

    @ColumnInfo(name = "iki_cv")
    val ikiCoefficientOfVariation: Double?,

    @ColumnInfo(name = "backspace_ratio")
    val backspaceRatio: Double?,

    @ColumnInfo(name = "scroll_velocity_px_per_sec")
    val scrollVelocityPxPerSec: Double?,

    @ColumnInfo(name = "svm_mean_g")
    val svmMeanG: Double,

    @ColumnInfo(name = "step_count")
    val stepCount: Int,

    @ColumnInfo(name = "in_hand_grip_tremor_power")
    val inHandGripTremorPower: Double
) {
    fun toDomain(): FeatureWindow15m = FeatureWindow15m(
        windowId = windowId,
        windowDurationMinutes = windowDurationMinutes,
        circadianBlock = circadianBlock,
        isWeekend = isWeekend,
        activityState = activityState,
        rmssdMs = rmssdMs,
        restingHrBpm = restingHrBpm,
        sedentaryHrDeltaBpm = sedentaryHrDeltaBpm,
        sleepDebtMinutes = sleepDebtMinutes,
        sleepFragmentationRatio = sleepFragmentationRatio,
        edaPeaksPerMinute = edaPeaksPerMinute,
        biometricValidBeatRatio = biometricValidBeatRatio,
        minutesSinceLastBiometricSync = minutesSinceLastBiometricSync,
        unlockCount = unlockCount,
        microSessionRatio = microSessionRatio,
        appSwitchCount = appSwitchCount,
        appSwitchEntropy = appSwitchEntropy,
        doomscrollStreakMinutes = doomscrollStreakMinutes,
        nightScreenMinutes = nightScreenMinutes,
        ikiMedianMs = ikiMedianMs,
        ikiCoefficientOfVariation = ikiCoefficientOfVariation,
        backspaceRatio = backspaceRatio,
        scrollVelocityPxPerSec = scrollVelocityPxPerSec,
        svmMeanG = svmMeanG,
        stepCount = stepCount,
        inHandGripTremorPower = inHandGripTremorPower
    )
}

fun FeatureWindow15m.toEntity(): FeatureWindow15mEntity = FeatureWindow15mEntity(
    windowId = windowId,
    windowDurationMinutes = windowDurationMinutes,
    circadianBlock = circadianBlock,
    isWeekend = isWeekend,
    activityState = activityState,
    rmssdMs = rmssdMs,
    restingHrBpm = restingHrBpm,
    sedentaryHrDeltaBpm = sedentaryHrDeltaBpm,
    sleepDebtMinutes = sleepDebtMinutes,
    sleepFragmentationRatio = sleepFragmentationRatio,
    edaPeaksPerMinute = edaPeaksPerMinute,
    biometricValidBeatRatio = biometricValidBeatRatio,
    minutesSinceLastBiometricSync = minutesSinceLastBiometricSync,
    unlockCount = unlockCount,
    microSessionRatio = microSessionRatio,
    appSwitchCount = appSwitchCount,
    appSwitchEntropy = appSwitchEntropy,
    doomscrollStreakMinutes = doomscrollStreakMinutes,
    nightScreenMinutes = nightScreenMinutes,
    ikiMedianMs = ikiMedianMs,
    ikiCoefficientOfVariation = ikiCoefficientOfVariation,
    backspaceRatio = backspaceRatio,
    scrollVelocityPxPerSec = scrollVelocityPxPerSec,
    svmMeanG = svmMeanG,
    stepCount = stepCount,
    inHandGripTremorPower = inHandGripTremorPower
)

