package com.synapseguard.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.synapseguard.core.database.entity.EmaFeedbackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmaDao {

    @Upsert
    suspend fun upsertEma(ema: EmaFeedbackEntity)

    @Query(
        """
        SELECT * FROM ema_responses
        WHERE used_for_sgd_update = 0
        ORDER BY responded_at_epoch_ms ASC
        """
    )
    suspend fun getPendingSgdUpdates(): List<EmaFeedbackEntity>

    @Query("UPDATE ema_responses SET used_for_sgd_update = 1 WHERE ema_id IN (:emaIds)")
    suspend fun markAsUsedForSgd(emaIds: List<String>)

    @Query("SELECT * FROM ema_responses ORDER BY responded_at_epoch_ms DESC LIMIT :limit")
    fun observeRecentEma(limit: Int): Flow<List<EmaFeedbackEntity>>

    @Query("DELETE FROM ema_responses WHERE window_id < :cutoffEpochMs")
    suspend fun deleteEmaOlderThanWindow(cutoffEpochMs: Long): Int
}

