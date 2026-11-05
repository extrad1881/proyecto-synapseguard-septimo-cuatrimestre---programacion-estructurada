package com.synapseguard.core.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainModelsTest {

    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
    }

    @Test
    fun `CircadianBlock resolves all 24 hours accurately`() {
        for (hour in 6..11) {
            assertEquals(CircadianBlock.MORNING, CircadianBlock.fromHourOfDay(hour))
        }
        for (hour in 12..17) {
            assertEquals(CircadianBlock.AFTERNOON, CircadianBlock.fromHourOfDay(hour))
        }
        for (hour in 18..22) {
            assertEquals(CircadianBlock.EVENING, CircadianBlock.fromHourOfDay(hour))
        }
        assertEquals(CircadianBlock.LATE_NIGHT, CircadianBlock.fromHourOfDay(23))
        for (hour in 0..5) {
            assertEquals(CircadianBlock.LATE_NIGHT, CircadianBlock.fromHourOfDay(hour))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `CircadianBlock rejects invalid hour of day`() {
        CircadianBlock.fromHourOfDay(24)
    }

    @Test
    fun `ActivityState correctly flags physiological alert inhibition`() {
        assertFalse(ActivityState.REST.inhibitsPhysiologicalStressAlerts)
        assertTrue(ActivityState.PHYSICAL_ACTIVITY.inhibitsPhysiologicalStressAlerts)
        assertTrue(ActivityState.EPOC_COOLDOWN.inhibitsPhysiologicalStressAlerts)
    }

    @Test
    fun `FeatureWindow15m serializes and validates biometric and baseline eligibility`() {
        val window = FeatureWindow15m(
            windowId = 1762300800000L,
            windowDurationMinutes = 15,
            circadianBlock = CircadianBlock.AFTERNOON,
            isWeekend = false,
            activityState = ActivityState.REST,
            rmssdMs = 34.5,
            restingHrBpm = 64.0,
            biometricValidBeatRatio = 0.94,
            minutesSinceLastBiometricSync = 2.0,
            unlockCount = 6,
            microSessionRatio = 0.33,
            appSwitchCount = 18,
            appSwitchEntropy = 2.15,
            doomscrollStreakMinutes = 11.5,
            nightScreenMinutes = 0.0,
            ikiMedianMs = 142.0,
            ikiCoefficientOfVariation = 0.38,
            backspaceRatio = 0.14,
            scrollVelocityPxPerSec = 1850.0,
            svmMeanG = 0.03,
            stepCount = 4,
            inHandGripTremorPower = 0.12
        )

        assertTrue(window.hasBiometricSignal)
        assertTrue(window.isEligibleForBaselineCalibration)

        val encoded = json.encodeToString(window)
        val decoded = json.decodeFromString<FeatureWindow15m>(encoded)
        assertEquals(window, decoded)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `FeatureWindow15m rejects out of range RMSSD`() {
        FeatureWindow15m(
            windowId = 1762300800000L,
            circadianBlock = CircadianBlock.MORNING,
            isWeekend = false,
            activityState = ActivityState.REST,
            rmssdMs = 500.0
        )
    }

    @Test
    fun `StressInferenceResult respects threshold and physical activity inhibition`() {
        val factor1 = StressFactorContribution(
            metricKey = "rmssd_z",
            zScore = -1.9,
            weightContribution = 0.48,
            humanReadableLabelEs = "Caída de 1.9 SD en VFC"
        )
        val factor2 = StressFactorContribution(
            metricKey = "app_switches_z",
            zScore = 2.3,
            weightContribution = 0.35,
            humanReadableLabelEs = "22 cambios rápidos de app en 15 min"
        )

        val activeStressResult = StressInferenceResult(
            inferenceId = "inf-001",
            windowId = 1762300800000L,
            evaluatedAtEpochMs = 1762300805000L,
            physiologicalProbability = 0.84,
            behavioralProbability = 0.79,
            biometricSignalQuality = 0.92,
            appliedPhysioWeight = 0.55,
            appliedBehavioralWeight = 0.45,
            finalStressProbability = 0.82,
            inhibitedByPhysicalActivity = false,
            topFactor1 = factor1,
            topFactor2 = factor2,
            explanationSummaryEs = "Caída de 1.9 SD en VFC + 22 cambios rápidos de app en 15 min"
        )

        assertTrue(activeStressResult.shouldTriggerJitai(0.75))
        assertFalse(activeStressResult.shouldTriggerJitai(0.90))

        val exerciseInhibitedResult = activeStressResult.copy(inhibitedByPhysicalActivity = true)
        assertFalse(exerciseInhibitedResult.shouldTriggerJitai(0.75))
    }

    @Test
    fun `JitaiIntervention computes RMSSD recovery delta and serializes cleanly`() {
        val intervention = JitaiIntervention(
            interventionId = "jitai-001",
            inferenceId = "inf-001",
            windowId = 1762300800000L,
            type = InterventionType.HAPTIC_VAGAL_BIOFEEDBACK,
            triggeredAtEpochMs = 1762300810000L,
            outcome = InterventionOutcome.COMPLETED,
            preInterventionRmssdMs = 24.0,
            postInterventionRmssdMs = 39.5,
            banditReward = 1.0
        )

        assertEquals(15.5, intervention.rmssdRecoveryDeltaMs!!, 1e-6)
        assertNull(intervention.copy(postInterventionRmssdMs = null).rmssdRecoveryDeltaMs)

        val roundTrip = json.decodeFromString<JitaiIntervention>(json.encodeToString(intervention))
        assertEquals(intervention, roundTrip)
    }

    @Test
    fun `EmaFeedback normalizes Likert 1 to 5 score into 0 to 1 probability`() {
        val lowStress = EmaFeedback(
            emaId = "ema-1",
            windowId = 1762300800000L,
            respondedAtEpochMs = 1762300900000L,
            perceivedStressScore = 1,
            activeCircadianBlock = CircadianBlock.MORNING
        )
        val mediumStress = lowStress.copy(perceivedStressScore = 3)
        val highStress = lowStress.copy(perceivedStressScore = 5)

        assertEquals(0.0, lowStress.normalizedTargetProbability, 1e-6)
        assertEquals(0.5, mediumStress.normalizedTargetProbability, 1e-6)
        assertEquals(1.0, highStress.normalizedTargetProbability, 1e-6)
    }
}

