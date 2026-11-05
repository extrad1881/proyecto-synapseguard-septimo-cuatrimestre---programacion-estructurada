package com.synapseguard.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase as FrameworkSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.synapseguard.core.database.crypto.AndroidKeystoreMasterKeyCipher
import com.synapseguard.core.database.crypto.KeystoreSecretManager
import com.synapseguard.core.database.entity.toEntity
import com.synapseguard.core.model.ActivityState
import com.synapseguard.core.model.CircadianBlock
import com.synapseguard.core.model.FeatureWindow15m
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SynapseDatabaseEncryptedTest {

    private lateinit var context: Context
    private lateinit var dbFile: File
    private lateinit var keyFile: File
    private lateinit var database: SynapseDatabase

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        dbFile = context.getDatabasePath("test_synapseguard_cipher.db")
        dbFile.delete()
        keyFile = File(context.noBackupFilesDir, "test_synapse_key.bin")
        keyFile.delete()

        val masterCipher = AndroidKeystoreMasterKeyCipher(
            context = context,
            keyAlias = "test_synapseguard_keystore_alias"
        )
        val secretManager = KeystoreSecretManager(
            keyBlobFile = keyFile,
            masterKeyCipher = masterCipher
        )
        database = SynapseDatabase.buildEncryptedDatabase(
            context = context,
            secretManager = secretManager,
            databaseName = dbFile.name
        )
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
        dbFile.delete()
        keyFile.delete()
    }

    @Test
    fun encryptedDatabase_insertsAndReads15mWindow_andRejectsPlaintextSqliteReader() = runTest {
        val now = 1762300800000L
        val sampleWindow = FeatureWindow15m(
            windowId = now,
            windowDurationMinutes = 15,
            circadianBlock = CircadianBlock.MORNING,
            isWeekend = false,
            activityState = ActivityState.REST,
            rmssdMs = 42.0,
            restingHrBpm = 60.0,
            biometricValidBeatRatio = 0.95,
            unlockCount = 4,
            appSwitchCount = 9
        )

        database.windowDao().upsertWindow(sampleWindow.toEntity())
        val loaded = database.windowDao().getWindowById(now)?.toDomain()
        assertNotNull(loaded)
        assertEquals(sampleWindow, loaded)

        // Forzar volcado al archivo y cerrar conexión SQLCipher
        database.close()
        assertTrue(dbFile.exists() && dbFile.length() > 0L)

        // Verificar que la cabecera del archivo NO es "SQLite format 3" en texto plano
        val headerString = String(dbFile.readBytes().take(15).toByteArray(), Charsets.US_ASCII)
        assertTrue("El archivo .db no debe tener cabecera SQLite en texto plano", headerString != "SQLite format 3")

        // Intentar abrir con el lector SQLite estándar de Android sin clave debe fallar
        try {
            val plainDb = FrameworkSQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                FrameworkSQLiteDatabase.OPEN_READONLY
            )
            plainDb.rawQuery("SELECT COUNT(*) FROM feature_windows_15m", null).use { cursor ->
                cursor.moveToFirst()
            }
            plainDb.close()
            fail("Se esperaba excepción al intentar leer la BD SQLCipher sin la llave del Keystore")
        } catch (_: Exception) {
            // Comportamiento esperado: base de datos ilegible sin la llave criptográfica
        }
    }

    @Test
    fun ttlMaintenanceManager_summarizesExpiredWindowsIntoDailySummary_andDeletesGranularRows() = runTest {
        val now = 1762300800000L
        val expiredWindowId = now - TimeUnit.DAYS.toMillis(95)
        val recentWindowId = now - TimeUnit.DAYS.toMillis(2)

        val expiredWindow = FeatureWindow15m(
            windowId = expiredWindowId,
            circadianBlock = CircadianBlock.AFTERNOON,
            isWeekend = false,
            activityState = ActivityState.REST,
            rmssdMs = 36.0,
            restingHrBpm = 65.0,
            biometricValidBeatRatio = 0.90,
            unlockCount = 10,
            appSwitchCount = 25,
            doomscrollStreakMinutes = 14.0
        )
        val recentWindow = expiredWindow.copy(windowId = recentWindowId, unlockCount = 3)

        database.windowDao().upsertWindows(listOf(expiredWindow.toEntity(), recentWindow.toEntity()))

        val ttlManager = DatabaseTtlMaintenanceManager(database)
        val report = ttlManager.runRetentionAndVacuum(
            nowEpochMs = now,
            retentionDays = 90L,
            executeVacuum = true
        )

        assertEquals(1, report.dailySummariesCreated)
        assertEquals(1, report.windowsDeleted)
        assertNotNull(database.windowDao().getWindowById(recentWindowId))
    }
}

