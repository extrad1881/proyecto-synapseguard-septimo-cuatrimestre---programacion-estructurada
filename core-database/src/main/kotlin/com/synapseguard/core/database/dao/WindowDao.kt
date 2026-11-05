package com.synapseguard.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.synapseguard.core.database.entity.DailySummaryEntity
import com.synapseguard.core.database.entity.FeatureWindow15mEntity
import com.synapseguard.core.model.ActivityState
import com.synapseguard.core.model.CircadianBlock
import kotlinx.coroutines.flow.Flow

@Dao
interface WindowDao {

    @Upsert
    suspend fun upsertWindow(window: FeatureWindow15mEntity)

    @Upsert
    suspend fun upsertWindows(windows: List<FeatureWindow15mEntity>)

    @Query("SELECT * FROM feature_windows_15m WHERE window_id = :windowId LIMIT 1")
    suspend fun getWindowById(windowId: Long): FeatureWindow15mEntity?

    @Query("SELECT * FROM feature_windows_15m ORDER BY window_id DESC LIMIT :limit")
    fun observeRecentWindows(limit: Int): Flow<List<FeatureWindow15mEntity>>

    /**
     * Consulta optimizada por el índice compuesto `idx_window_circadian_rest` para extraer
     * las ventanas de los últimos 14 días en estado de reposo dentro del bloque circadiano dado.
     */
    @Query(
        """
        SELECT * FROM feature_windows_15m
        WHERE circadian_block = :circadianBlock
          AND is_weekend = :isWeekend
          AND activity_state = :restState
          AND window_id >= :sinceEpochMs
        ORDER BY window_id ASC
        """
    )
    suspend fun getRestingWindowsForBaseline(
        sinceEpochMs: Long,
        circadianBlock: CircadianBlock,
        isWeekend: Boolean,
        restState: ActivityState = ActivityState.REST
    ): List<FeatureWindow15mEntity>

    @Query(
        """
        SELECT * FROM feature_windows_15m
        WHERE window_id BETWEEN :startEpochMs AND :endEpochMs
        ORDER BY window_id ASC
        """
    )
    suspend fun getWindowsBetween(startEpochMs: Long, endEpochMs: Long): List<FeatureWindow15mEntity>

    @Query("SELECT * FROM feature_windows_15m WHERE window_id < :cutoffEpochMs ORDER BY window_id ASC")
    suspend fun getWindowsOlderThan(cutoffEpochMs: Long): List<FeatureWindow15mEntity>

    @Query("DELETE FROM feature_windows_15m WHERE window_id < :cutoffEpochMs")
    suspend fun deleteWindowsOlderThan(cutoffEpochMs: Long): Int

    @Upsert
    suspend fun upsertDailySummaries(summaries: List<DailySummaryEntity>)

    @Query(
        """
        SELECT * FROM daily_summaries
        WHERE day_epoch_date BETWEEN :startDayEpoch AND :endDayEpoch
        ORDER BY day_epoch_date ASC
        """
    )
    suspend fun getDailySummariesBetween(
        startDayEpoch: Long,
        endDayEpoch: Long
    ): List<DailySummaryEntity>
}

