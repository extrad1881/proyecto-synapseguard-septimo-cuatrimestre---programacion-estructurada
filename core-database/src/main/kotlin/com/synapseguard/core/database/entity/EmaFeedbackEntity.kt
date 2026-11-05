package com.synapseguard.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.synapseguard.core.model.CircadianBlock
import com.synapseguard.core.model.EmaFeedback

/**
 * Entidad Room para respuestas de Evaluación Momentánea Ecológica (EMA) en escala 1 a 5.
 */
@Entity(
    tableName = "ema_responses",
    indices = [
        Index(name = "idx_ema_window", value = ["window_id"]),
        Index(name = "idx_ema_sgd_pending", value = ["used_for_sgd_update", "responded_at_epoch_ms"])
    ]
)
data class EmaFeedbackEntity(
    @PrimaryKey
    @ColumnInfo(name = "ema_id")
    val emaId: String,

    @ColumnInfo(name = "window_id")
    val windowId: Long,

    @ColumnInfo(name = "responded_at_epoch_ms")
    val respondedAtEpochMs: Long,

    @ColumnInfo(name = "perceived_stress_score")
    val perceivedStressScore: Int,

    @ColumnInfo(name = "active_circadian_block")
    val activeCircadianBlock: CircadianBlock,

    @ColumnInfo(name = "used_for_sgd_update")
    val usedForOnlineSgdUpdate: Boolean
) {
    fun toDomain(): EmaFeedback = EmaFeedback(
        emaId = emaId,
        windowId = windowId,
        respondedAtEpochMs = respondedAtEpochMs,
        perceivedStressScore = perceivedStressScore,
        activeCircadianBlock = activeCircadianBlock,
        usedForOnlineSgdUpdate = usedForOnlineSgdUpdate
    )
}

fun EmaFeedback.toEntity(): EmaFeedbackEntity = EmaFeedbackEntity(
    emaId = emaId,
    windowId = windowId,
    respondedAtEpochMs = respondedAtEpochMs,
    perceivedStressScore = perceivedStressScore,
    activeCircadianBlock = activeCircadianBlock,
    usedForOnlineSgdUpdate = usedForOnlineSgdUpdate
)

