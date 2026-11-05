package com.synapseguard.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.synapseguard.core.model.InterventionOutcome
import com.synapseguard.core.model.InterventionType
import com.synapseguard.core.model.JitaiIntervention

/**
 * Entidad Room para intervenciones adaptativas (JITAI) y sus métricas de recuperación vagal.
 */
@Entity(
    tableName = "jitai_interventions",
    indices = [
        Index(name = "idx_jitai_inference", value = ["inference_id"]),
        Index(name = "idx_jitai_window", value = ["window_id"]),
        Index(name = "idx_jitai_triggered_at", value = ["triggered_at_epoch_ms"])
    ]
)
data class JitaiInterventionEntity(
    @PrimaryKey
    @ColumnInfo(name = "intervention_id")
    val interventionId: String,

    @ColumnInfo(name = "inference_id")
    val inferenceId: String,

    @ColumnInfo(name = "window_id")
    val windowId: Long,

    @ColumnInfo(name = "intervention_type")
    val type: InterventionType,

    @ColumnInfo(name = "triggered_at_epoch_ms")
    val triggeredAtEpochMs: Long,

    @ColumnInfo(name = "outcome")
    val outcome: InterventionOutcome,

    @ColumnInfo(name = "pre_intervention_rmssd_ms")
    val preInterventionRmssdMs: Double?,

    @ColumnInfo(name = "post_intervention_rmssd_ms")
    val postInterventionRmssdMs: Double?,

    @ColumnInfo(name = "bandit_reward")
    val banditReward: Double
) {
    fun toDomain(): JitaiIntervention = JitaiIntervention(
        interventionId = interventionId,
        inferenceId = inferenceId,
        windowId = windowId,
        type = type,
        triggeredAtEpochMs = triggeredAtEpochMs,
        outcome = outcome,
        preInterventionRmssdMs = preInterventionRmssdMs,
        postInterventionRmssdMs = postInterventionRmssdMs,
        banditReward = banditReward
    )
}

fun JitaiIntervention.toEntity(): JitaiInterventionEntity = JitaiInterventionEntity(
    interventionId = interventionId,
    inferenceId = inferenceId,
    windowId = windowId,
    type = type,
    triggeredAtEpochMs = triggeredAtEpochMs,
    outcome = outcome,
    preInterventionRmssdMs = preInterventionRmssdMs,
    postInterventionRmssdMs = postInterventionRmssdMs,
    banditReward = banditReward
)

