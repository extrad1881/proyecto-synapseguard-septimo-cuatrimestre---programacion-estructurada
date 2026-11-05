package com.synapseguard.core.database.di

import android.content.Context
import com.synapseguard.core.database.SynapseDatabase
import com.synapseguard.core.database.crypto.AndroidKeystoreMasterKeyCipher
import com.synapseguard.core.database.crypto.KeystoreSecretManager
import com.synapseguard.core.database.crypto.MasterKeyCipher
import com.synapseguard.core.database.dao.BaselineDao
import com.synapseguard.core.database.dao.EmaDao
import com.synapseguard.core.database.dao.InferenceDao
import com.synapseguard.core.database.dao.InterventionDao
import com.synapseguard.core.database.dao.WindowDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMasterKeyCipher(
        @ApplicationContext context: Context
    ): MasterKeyCipher = AndroidKeystoreMasterKeyCipher(context)

    @Provides
    @Singleton
    fun provideKeystoreSecretManager(
        @ApplicationContext context: Context,
        masterKeyCipher: MasterKeyCipher
    ): KeystoreSecretManager {
        val secretFile = File(context.noBackupFilesDir, KeystoreSecretManager.DEFAULT_KEY_FILE_NAME)
        return KeystoreSecretManager(
            keyBlobFile = secretFile,
            masterKeyCipher = masterKeyCipher
        )
    }

    @Provides
    @Singleton
    fun provideSynapseDatabase(
        @ApplicationContext context: Context,
        secretManager: KeystoreSecretManager
    ): SynapseDatabase = SynapseDatabase.buildEncryptedDatabase(context, secretManager)

    @Provides
    fun provideWindowDao(database: SynapseDatabase): WindowDao = database.windowDao()

    @Provides
    fun provideBaselineDao(database: SynapseDatabase): BaselineDao = database.baselineDao()

    @Provides
    fun provideInferenceDao(database: SynapseDatabase): InferenceDao = database.inferenceDao()

    @Provides
    fun provideInterventionDao(database: SynapseDatabase): InterventionDao = database.interventionDao()

    @Provides
    fun provideEmaDao(database: SynapseDatabase): EmaDao = database.emaDao()
}

