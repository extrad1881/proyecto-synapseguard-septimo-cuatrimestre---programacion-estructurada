package com.synapseguard.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.synapseguard.core.model.CircadianBaseline
import com.synapseguard.core.model.CircadianBlock

/**
 * Entidad Room para almacenar la Mediana y MAD de los últimos 14 días en reposo,
 * estratificadas por métrica, bloque horario circadiano y tipo de día.
 */
@Entity(
    tableName = "circadian_baselines",
    primaryKeys = ["metric_key", "circadian_block", "is_weekend"]
)
data class CircadianBaselineEntity(
    @ColumnInfo(name = "metric_key")
    val metricKey: String,

    @ColumnInfo(name = "circadian_block")
    val circadianBlock: CircadianBlock,

    @ColumnInfo(name = "is_weekend")
    val isWeekend: Boolean,

    @ColumnInfo(name = "median_value")
    val medianValue: Double,

    @ColumnInfo(name = "mad_value")
    val madValue: Double,

    @ColumnInfo(name = "sample_count")
    val sampleCount: Int,

    @ColumnInfo(name = "updated_at_epoch_ms")
    val updatedAtEpochMs: Long
) {
    fun toDomain(): CircadianBaseline = CircadianBaseline(
        metricKey = metricKey,
        circadianBlock = circadianBlock,
        isWeekend = isWeekend,
        medianValue = medianValue,
        madValue = madValue,
        sampleCount = sampleCount,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

fun CircadianBaseline.toEntity(): CircadianBaselineEntity = CircadianBaselineEntity(
    metricKey = metricKey,
    circadianBlock = circadianBlock,
    isWeekend = isWeekend,
    medianValue = medianValue,
    madValue = madValue,
    sampleCount = sampleCount,
    updatedAtEpochMs = updatedAtEpochMs
)

