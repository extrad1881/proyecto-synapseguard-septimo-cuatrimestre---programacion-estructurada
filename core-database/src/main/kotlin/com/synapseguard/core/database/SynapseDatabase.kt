package com.synapseguard.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.synapseguard.core.database.crypto.KeystoreSecretManager
import com.synapseguard.core.database.dao.BaselineDao
import com.synapseguard.core.database.dao.EmaDao
import com.synapseguard.core.database.dao.InferenceDao
import com.synapseguard.core.database.dao.InterventionDao
import com.synapseguard.core.database.dao.WindowDao
import com.synapseguard.core.database.entity.CircadianBaselineEntity
import com.synapseguard.core.database.entity.DailySummaryEntity
import com.synapseguard.core.database.entity.EmaFeedbackEntity
import com.synapseguard.core.database.entity.FeatureWindow15mEntity
import com.synapseguard.core.database.entity.JitaiInterventionEntity
import com.synapseguard.core.database.entity.StressInferenceEntity
import com.synapseguard.core.database.entity.SynapseTypeConverters
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

/**
 * Base de datos local cifrada con AES-256 mediante Room + SQLCipher.
 * La llave de cifrado se protege mediante envolvente criptográfica en AndroidKeyStore (StrongBox/TEE).
 */
@Database(
    entities = [
        FeatureWindow15mEntity::class,
        CircadianBaselineEntity::class,
        StressInferenceEntity::class,
        JitaiInterventionEntity::class,
        EmaFeedbackEntity::class,
        DailySummaryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(SynapseTypeConverters::class)
abstract class SynapseDatabase : RoomDatabase() {

    abstract fun windowDao(): WindowDao
    abstract fun baselineDao(): BaselineDao
    abstract fun inferenceDao(): InferenceDao
    abstract fun interventionDao(): InterventionDao
    abstract fun emaDao(): EmaDao

    companion object {
        const val DATABASE_NAME: String = "synapseguard_encrypted.db"

        /**
         * Construye una instancia de [SynapseDatabase] cifrada con SQLCipher.
         * Nota: `clearPassphrase = false` en [SupportFactory] evita que SQLCipher limpie el arreglo
         * prematuramente si la conexión se reabre tras cierre por baja memoria.
         */
        fun buildEncryptedDatabase(
            context: Context,
            secretManager: KeystoreSecretManager,
            databaseName: String = DATABASE_NAME
        ): SynapseDatabase {
            SQLiteDatabase.loadLibs(context.applicationContext)
            val passphraseBytes = secretManager.getOrCreateDatabasePassphrase()
            val supportFactory = SupportFactory(passphraseBytes, null, false)

            return Room.databaseBuilder(
                context.applicationContext,
                SynapseDatabase::class.java,
                databaseName
            )
                .openHelperFactory(supportFactory)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}

