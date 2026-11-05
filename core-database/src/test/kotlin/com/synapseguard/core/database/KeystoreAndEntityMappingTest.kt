package com.synapseguard.core.database

import com.synapseguard.core.database.crypto.EncryptedPayload
import com.synapseguard.core.database.crypto.KeystoreSecretManager
import com.synapseguard.core.database.crypto.MasterKeyCipher
import com.synapseguard.core.database.entity.SynapseTypeConverters
import com.synapseguard.core.database.entity.toEntity
import com.synapseguard.core.model.ActivityState
import com.synapseguard.core.model.CircadianBaseline
import com.synapseguard.core.model.CircadianBlock
import com.synapseguard.core.model.DailySummary
import com.synapseguard.core.model.EmaFeedback
import com.synapseguard.core.model.FeatureWindow15m
import com.synapseguard.core.model.InterventionOutcome
import com.synapseguard.core.model.InterventionType
import com.synapseguard.core.model.JitaiIntervention
import com.synapseguard.core.model.StressFactorContribution
import com.synapseguard.core.model.StressInferenceResult
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreAndEntityMappingTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    /**
     * Proveedor JCE AES-256-GCM en memoria para probar el flujo completo de cifrado envolvente
     * de [KeystoreSecretManager] en JVM estándar.
     */
    private class JvmAesGcmMasterKeyCipher : MasterKeyCipher {
        private val secretKey: SecretKey = KeyGenerator.getInstance("AES").apply {
            init(256)
        }.generateKey()

        override val isStrongBoxBacked: Boolean = true

        override fun encrypt(plaintext: ByteArray): EncryptedPayload {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            return EncryptedPayload(
                iv = cipher.iv,
                ciphertextWithTag = cipher.doFinal(plaintext)
            )
        }

        override fun decrypt(payload: EncryptedPayload): ByteArray {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, payload.iv))
            return cipher.doFinal(payload.ciphertextWithTag)
        }
    }

    @Test
    fun `KeystoreSecretManager generates 32-byte passphrase and persists encrypted blob`() {
        val keyFile = tempFolder.newFile("synapse_sqlcipher_key.bin")
        keyFile.delete() // Asegurar primer inicio limpio
        val cipher = JvmAesGcmMasterKeyCipher()
        val manager = KeystoreSecretManager(keyBlobFile = keyFile, masterKeyCipher = cipher)

        val firstPassphrase = manager.getOrCreateDatabasePassphrase()
        assertEquals(32, firstPassphrase.size)
        assertTrue(keyFile.exists() && keyFile.length() > 32L)

        // El archivo en disco NO contiene la passphrase en texto plano
        val rawDiskBytes = keyFile.readBytes()
        assertFalse(rawDiskBytes.TakeSliceEquals(firstPassphrase))

        // Segunda invocación recupera exactamente la misma passphrase descifrada
        val secondPassphrase = manager.getOrCreateDatabasePassphrase()
        assertArrayEquals(firstPassphrase, secondPassphrase)

        // Limpieza segura en RAM
        KeystoreSecretManager.zeroOut(firstPassphrase)
        assertTrue(firstPassphrase.all { it == 0.toByte() })
    }

    @Test(expected = AEADBadTagException::class)
    fun `KeystoreSecretManager detects tampering in encrypted key blob via GCM authentication tag`() {
        val keyFile = tempFolder.newFile("tampered_key.bin")
        keyFile.delete()
        val cipher = JvmAesGcmMasterKeyCipher()
        val manager = KeystoreSecretManager(keyBlobFile = keyFile, masterKeyCipher = cipher)

        manager.getOrCreateDatabasePassphrase()

        // Corromper el último byte (etiqueta de autenticación GCM)
        val bytes = keyFile.readBytes()
        bytes[bytes.lastIndex] = (bytes[bytes.lastIndex].toInt() xor 0xFF).toByte()
        keyFile.writeBytes(bytes)

        manager.getOrCreateDatabasePassphrase()
    }

    @Test
    fun `Room Entities and TypeConverters map bidirectionally without data loss`() {
        val converters = SynapseTypeConverters()
        for (block in CircadianBlock.entries) {
            assertEquals(block, converters.toCircadianBlock(converters.fromCircadianBlock(block)))
        }
        for (state in ActivityState.entries) {
            assertEquals(state, converters.toActivityState(converters.fromActivityState(state)))
        }

        val window = FeatureWindow15m(
            windowId = 1762300800000L,
            windowDurationMinutes = 15,
            circadianBlock = CircadianBlock.EVENING,
            isWeekend = true,
            activityState = ActivityState.REST,
            rmssdMs = 41.2,
            restingHrBpm = 61.5,
            sedentaryHrDeltaBpm = 3.2,
            sleepDebtMinutes = 45.0,
            sleepFragmentationRatio = 0.12,
            edaPeaksPerMinute = 1.5,
            biometricValidBeatRatio = 0.96,
            minutesSinceLastBiometricSync = 1.0,
            unlockCount = 8,
            microSessionRatio = 0.25,
            appSwitchCount = 19,
            appSwitchEntropy = 1.95,
            doomscrollStreakMinutes = 12.0,
            nightScreenMinutes = 5.0,
            ikiMedianMs = 155.0,
            ikiCoefficientOfVariation = 0.31,
            backspaceRatio = 0.11,
            scrollVelocityPxPerSec = 1420.0,
            svmMeanG = 0.02,
            stepCount = 0,
            inHandGripTremorPower = 0.08
        )
        assertEquals(window, window.toEntity().toDomain())

        val baseline = CircadianBaseline(
            metricKey = "rmssd_ln",
            circadianBlock = CircadianBlock.EVENING,
            isWeekend = true,
            medianValue = 3.71,
            madValue = 0.24,
            sampleCount = 42,
            updatedAtEpochMs = 1762300900000L
        )
        assertEquals(baseline, baseline.toEntity().toDomain())

        val inference = StressInferenceResult(
            inferenceId = "inf-100",
            windowId = window.windowId,
            evaluatedAtEpochMs = 1762300810000L,
            physiologicalProbability = 0.81,
            behavioralProbability = 0.77,
            biometricSignalQuality = 0.95,
            appliedPhysioWeight = 0.52,
            appliedBehavioralWeight = 0.48,
            finalStressProbability = 0.80,
            inhibitedByPhysicalActivity = false,
            topFactor1 = StressFactorContribution("rmssd_ln", -1.85, 0.45, "Caída de 1.85 SD en VFC"),
            topFactor2 = StressFactorContribution("app_switches", 2.10, 0.35, "19 cambios de app en 15m"),
            explanationSummaryEs = "Caída de 1.85 SD en VFC + 19 cambios de app en 15m"
        )
        assertEquals(inference, inference.toEntity().toDomain())

        val intervention = JitaiIntervention(
            interventionId = "jitai-100",
            inferenceId = inference.inferenceId,
            windowId = window.windowId,
            type = InterventionType.MIND_FRICTION_OVERLAY_8S,
            triggeredAtEpochMs = 1762300820000L,
            outcome = InterventionOutcome.COMPLETED,
            preInterventionRmssdMs = 28.0,
            postInterventionRmssdMs = 40.0,
            banditReward = 1.0
        )
        assertEquals(intervention, intervention.toEntity().toDomain())

        val ema = EmaFeedback(
            emaId = "ema-100",
            windowId = window.windowId,
            respondedAtEpochMs = 1762300950000L,
            perceivedStressScore = 4,
            activeCircadianBlock = CircadianBlock.EVENING,
            usedForOnlineSgdUpdate = false
        )
        assertEquals(ema, ema.toEntity().toDomain())

        val summary = DailySummary(
            dayEpochDate = 20396L,
            meanRmssdMs = 38.4,
            meanRestingHrBpm = 62.0,
            totalUnlocks = 54,
            totalAppSwitches = 140,
            totalDoomscrollMinutes = 42.5,
            meanStressProbability = 0.48,
            interventionsTriggered = 3,
            interventionsCompleted = 2
        )
        assertEquals(summary, summary.toEntity().toDomain())
    }

    private fun ByteArray.TakeSliceEquals(subArray: ByteArray): Boolean {
        if (this.size < subArray.size) return false
        for (i in 0..(this.size - subArray.size)) {
            var match = true
            for (j in subArray.indices) {
                if (this[i + j] != subArray[j]) {
                    match = false
                    break
                }
            }
            if (match) return true
        }
        return false
    }
}

