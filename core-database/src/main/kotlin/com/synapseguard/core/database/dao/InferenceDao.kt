package com.synapseguard.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.synapseguard.core.database.entity.StressInferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InferenceDao {

    @Upsert
    suspend fun upsertInference(inference: StressInferenceEntity)

    @Query("SELECT * FROM stress_inferences ORDER BY evaluated_at_epoch_ms DESC LIMIT 1")
    suspend fun getLatestInference(): StressInferenceEntity?

    @Query("SELECT * FROM stress_inferences ORDER BY evaluated_at_epoch_ms DESC LIMIT 1")
    fun observeLatestInference(): Flow<StressInferenceEntity?>

    @Query(
        """
        SELECT * FROM stress_inferences
        WHERE evaluated_at_epoch_ms BETWEEN :startEpochMs AND :endEpochMs
        ORDER BY evaluated_at_epoch_ms ASC
        """
    )
    suspend fun getInferencesBetween(
        startEpochMs: Long,
        endEpochMs: Long
    ): List<StressInferenceEntity>

    @Query("SELECT * FROM stress_inferences WHERE window_id < :cutoffEpochMs")
    suspend fun getInferencesOlderThanWindow(cutoffEpochMs: Long): List<StressInferenceEntity>

    @Query("DELETE FROM stress_inferences WHERE window_id < :cutoffEpochMs")
    suspend fun deleteInferencesOlderThanWindow(cutoffEpochMs: Long): Int
}

