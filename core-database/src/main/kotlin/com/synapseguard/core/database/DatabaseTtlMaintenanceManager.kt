package com.synapseguard.core.database

import androidx.room.withTransaction
import com.synapseguard.core.database.entity.DailySummaryEntity
import com.synapseguard.core.model.InterventionOutcome
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resultado estadístico de una ejecución de mantenimiento TTL y compactación.
 */
data class TtlCompactionReport(
    val cutoffEpochMs: Long,
    val dailySummariesCreated: Int,
    val windowsDeleted: Int,
    val inferencesDeleted: Int,
    val interventionsDeleted: Int,
    val emaDeleted: Int,
    val vacuumExecuted: Boolean
)

/**
 * Ejecuta la política de retención local de 90 días (TTL):
 * 1. Consolida las ventanas de 15m anteriores al corte en resúmenes diarios (`daily_summaries`).
 * 2. Elimina los registros granulares antiguos (`feature_windows_15m`, `stress_inferences`,
 *    `jitai_interventions`, `ema_responses`).
 * 3. Ejecuta `PRAGMA wal_checkpoint(TRUNCATE)` y `VACUUM` para compactar el archivo SQLCipher en disco.
 */
@Singleton
class DatabaseTtlMaintenanceManager @Inject constructor(
    private val database: SynapseDatabase
) {

    suspend fun runRetentionAndVacuum(
        nowEpochMs: Long = System.currentTimeMillis(),
        retentionDays: Long = DEFAULT_RETENTION_DAYS,
        executeVacuum: Boolean = true
    ): TtlCompactionReport {
        require(retentionDays >= 14L) {
            "La retención mínima debe ser de al menos 14 días para preservar la línea base circadiana."
        }
        val cutoffEpochMs = nowEpochMs - TimeUnit.DAYS.toMillis(retentionDays)

        var summariesCreated = 0
        var windowsDeleted = 0
        var inferencesDeleted = 0
        var interventionsDeleted = 0
        var emaDeleted = 0

        database.withTransaction {
            val windowDao = database.windowDao()
            val inferenceDao = database.inferenceDao()
            val interventionDao = database.interventionDao()
            val emaDao = database.emaDao()

            val expiredWindows = windowDao.getWindowsOlderThan(cutoffEpochMs)
            if (expiredWindows.isNotEmpty()) {
                val expiredInferencesByWindow = inferenceDao
                    .getInferencesOlderThanWindow(cutoffEpochMs)
                    .associateBy { it.windowId }
                val expiredInterventionsByWindow = interventionDao
                    .getInterventionsOlderThanWindow(cutoffEpochMs)
                    .groupBy { it.windowId }

                val groupedByDay = expiredWindows.groupBy { window ->
                    window.windowId / MILLIS_PER_DAY
                }

                val summaries = groupedByDay.map { (dayEpochDate, dayWindows) ->
                    val rmssdValues = dayWindows.mapNotNull { it.rmssdMs }
                    val rhrValues = dayWindows.mapNotNull { it.restingHrBpm }
                    val stressProbs = dayWindows.mapNotNull { w ->
                        expiredInferencesByWindow[w.windowId]?.finalStressProbability
                    }
                    val dayInterventions = dayWindows.flatMap { w ->
                        expiredInterventionsByWindow[w.windowId].orEmpty()
                    }
                    val completedCount = dayInterventions.count {
                        it.outcome == InterventionOutcome.COMPLETED
                    }

                    DailySummaryEntity(
                        dayEpochDate = dayEpochDate,
                        meanRmssdMs = if (rmssdValues.isNotEmpty()) rmssdValues.average() else null,
                        meanRestingHrBpm = if (rhrValues.isNotEmpty()) rhrValues.average() else null,
                        totalUnlocks = dayWindows.sumOf { it.unlockCount },
                        totalAppSwitches = dayWindows.sumOf { it.appSwitchCount },
                        totalDoomscrollMinutes = dayWindows.sumOf { it.doomscrollStreakMinutes },
                        meanStressProbability = if (stressProbs.isNotEmpty()) {
                            stressProbs.average().coerceIn(0.0, 1.0)
                        } else {
                            0.0
                        },
                        interventionsTriggered = dayInterventions.size,
                        interventionsCompleted = completedCount
                    )
                }

                windowDao.upsertDailySummaries(summaries)
                summariesCreated = summaries.size
            }

            emaDeleted = emaDao.deleteEmaOlderThanWindow(cutoffEpochMs)
            interventionsDeleted = interventionDao.deleteInterventionsOlderThanWindow(cutoffEpochMs)
            inferencesDeleted = inferenceDao.deleteInferencesOlderThanWindow(cutoffEpochMs)
            windowsDeleted = windowDao.deleteWindowsOlderThan(cutoffEpochMs)
        }

        var vacuumRan = false
        if (executeVacuum && (windowsDeleted > 0 || inferencesDeleted > 0 || interventionsDeleted > 0)) {
            val writableDb = database.openHelper.writableDatabase
            writableDb.query("PRAGMA wal_checkpoint(TRUNCATE)").close()
            writableDb.execSQL("VACUUM")
            vacuumRan = true
        }

        return TtlCompactionReport(
            cutoffEpochMs = cutoffEpochMs,
            dailySummariesCreated = summariesCreated,
            windowsDeleted = windowsDeleted,
            inferencesDeleted = inferencesDeleted,
            interventionsDeleted = interventionsDeleted,
            emaDeleted = emaDeleted,
            vacuumExecuted = vacuumRan
        )
    }

    companion object {
        const val DEFAULT_RETENTION_DAYS: Long = 90L
        private val MILLIS_PER_DAY: Long = TimeUnit.DAYS.toMillis(1L)
    }
}

