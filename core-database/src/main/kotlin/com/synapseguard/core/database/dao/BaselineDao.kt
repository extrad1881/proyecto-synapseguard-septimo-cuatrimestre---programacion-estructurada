package com.synapseguard.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.synapseguard.core.database.entity.CircadianBaselineEntity
import com.synapseguard.core.model.CircadianBlock
import kotlinx.coroutines.flow.Flow

@Dao
interface BaselineDao {

    @Upsert
    suspend fun upsertBaseline(baseline: CircadianBaselineEntity)

    @Upsert
    suspend fun upsertBaselines(baselines: List<CircadianBaselineEntity>)

    @Query(
        """
        SELECT * FROM circadian_baselines
        WHERE metric_key = :metricKey
          AND circadian_block = :circadianBlock
          AND is_weekend = :isWeekend
        LIMIT 1
        """
    )
    suspend fun getBaseline(
        metricKey: String,
        circadianBlock: CircadianBlock,
        isWeekend: Boolean
    ): CircadianBaselineEntity?

    @Query(
        """
        SELECT * FROM circadian_baselines
        WHERE circadian_block = :circadianBlock
          AND is_weekend = :isWeekend
        """
    )
    suspend fun getBaselinesForSlot(
        circadianBlock: CircadianBlock,
        isWeekend: Boolean
    ): List<CircadianBaselineEntity>

    @Query("SELECT * FROM circadian_baselines ORDER BY circadian_block ASC, metric_key ASC")
    fun observeAllBaselines(): Flow<List<CircadianBaselineEntity>>
}

