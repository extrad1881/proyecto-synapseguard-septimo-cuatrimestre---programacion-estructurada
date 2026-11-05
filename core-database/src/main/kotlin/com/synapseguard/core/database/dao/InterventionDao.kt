package com.synapseguard.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.synapseguard.core.database.entity.JitaiInterventionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InterventionDao {

    @Upsert
    suspend fun upsertIntervention(intervention: JitaiInterventionEntity)

    @Query(
        """
        SELECT * FROM jitai_interventions
        WHERE triggered_at_epoch_ms >= :sinceEpochMs
        ORDER BY triggered_at_epoch_ms DESC
        """
    )
    suspend fun getInterventionsSince(sinceEpochMs: Long): List<JitaiInterventionEntity>

    @Query("SELECT MAX(triggered_at_epoch_ms) FROM jitai_interventions")
    suspend fun getLastInterventionTimestamp(): Long?

    @Query("SELECT * FROM jitai_interventions ORDER BY triggered_at_epoch_ms DESC LIMIT :limit")
    fun observeInterventions(limit: Int): Flow<List<JitaiInterventionEntity>>

    @Query("SELECT * FROM jitai_interventions WHERE window_id < :cutoffEpochMs")
    suspend fun getInterventionsOlderThanWindow(cutoffEpochMs: Long): List<JitaiInterventionEntity>

    @Query("DELETE FROM jitai_interventions WHERE window_id < :cutoffEpochMs")
    suspend fun deleteInterventionsOlderThanWindow(cutoffEpochMs: Long): Int
}

