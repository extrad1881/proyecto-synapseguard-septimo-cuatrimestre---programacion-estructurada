package com.synapseguard.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.synapseguard.core.model.DailySummary

/**
 * Entidad Room para resúmenes diarios consolidados (preservados indefinidamente tras la poda TTL de 90 días).
 */
@Entity(tableName = "daily_summaries")
data class DailySummaryEntity(
    @PrimaryKey
    @ColumnInfo(name = "day_epoch_date")
    val dayEpochDate: Long,

    @ColumnInfo(name = "mean_rmssd_ms")
    val meanRmssdMs: Double?,

    @ColumnInfo(name = "mean_resting_hr_bpm")
    val meanRestingHrBpm: Double?,

    @ColumnInfo(name = "total_unlocks")
    val totalUnlocks: Int,

    @ColumnInfo(name = "total_app_switches")
    val totalAppSwitches: Int,

    @ColumnInfo(name = "total_doomscroll_minutes")
    val totalDoomscrollMinutes: Double,

    @ColumnInfo(name = "mean_stress_probability")
    val meanStressProbability: Double,

    @ColumnInfo(name = "interventions_triggered")
    val interventionsTriggered: Int,

    @ColumnInfo(name = "interventions_completed")
    val interventionsCompleted: Int
) {
    fun toDomain(): DailySummary = DailySummary(
        dayEpochDate = dayEpochDate,
        meanRmssdMs = meanRmssdMs,
        meanRestingHrBpm = meanRestingHrBpm,
        totalUnlocks = totalUnlocks,
        totalAppSwitches = totalAppSwitches,
        totalDoomscrollMinutes = totalDoomscrollMinutes,
        meanStressProbability = meanStressProbability,
        interventionsTriggered = interventionsTriggered,
        interventionsCompleted = interventionsCompleted
    )
}

fun DailySummary.toEntity(): DailySummaryEntity = DailySummaryEntity(
    dayEpochDate = dayEpochDate,
    meanRmssdMs = meanRmssdMs,
    meanRestingHrBpm = meanRestingHrBpm,
    totalUnlocks = totalUnlocks,
    totalAppSwitches = totalAppSwitches,
    totalDoomscrollMinutes = totalDoomscrollMinutes,
    meanStressProbability = meanStressProbability,
    interventionsTriggered = interventionsTriggered,
    interventionsCompleted = interventionsCompleted
)

