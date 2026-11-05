package com.synapseguard.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.synapseguard.core.model.StressFactorContribution
import com.synapseguard.core.model.StressInferenceResult

/**
 * Entidad Room para los resultados de inferencia multimodal (Fusión Tardía) y factores explicables.
 */
@Entity(
    tableName = "stress_inferences",
    indices = [
        Index(name = "idx_inference_window", value = ["window_id"]),
        Index(name = "idx_inference_evaluated_at", value = ["evaluated_at_epoch_ms"])
    ]
)
data class StressInferenceEntity(
    @PrimaryKey
    @ColumnInfo(name = "inference_id")
    val inferenceId: String,

    @ColumnInfo(name = "window_id")
    val windowId: Long,

    @ColumnInfo(name = "evaluated_at_epoch_ms")
    val evaluatedAtEpochMs: Long,

    @ColumnInfo(name = "physiological_probability")
    val physiologicalProbability: Double?,

    @ColumnInfo(name = "behavioral_probability")
    val behavioralProbability: Double,

    @ColumnInfo(name = "biometric_signal_quality")
    val biometricSignalQuality: Double,

    @ColumnInfo(name = "applied_physio_weight")
    val appliedPhysioWeight: Double,

    @ColumnInfo(name = "applied_behavioral_weight")
    val appliedBehavioralWeight: Double,

    @ColumnInfo(name = "final_stress_probability")
    val finalStressProbability: Double,

    @ColumnInfo(name = "inhibited_by_physical_activity")
    val inhibitedByPhysicalActivity: Boolean,

    @ColumnInfo(name = "top1_metric_key")
    val top1MetricKey: String,

    @ColumnInfo(name = "top1_z_score")
    val top1ZScore: Double,

    @ColumnInfo(name = "top1_weight_contribution")
    val top1WeightContribution: Double,

    @ColumnInfo(name = "top1_label_es")
    val top1LabelEs: String,

    @ColumnInfo(name = "top2_metric_key")
    val top2MetricKey: String?,

    @ColumnInfo(name = "top2_z_score")
    val top2ZScore: Double?,

    @ColumnInfo(name = "top2_weight_contribution")
    val top2WeightContribution: Double?,

    @ColumnInfo(name = "top2_label_es")
    val top2LabelEs: String?,

    @ColumnInfo(name = "explanation_summary_es")
    val explanationSummaryEs: String
) {
    fun toDomain(): StressInferenceResult = StressInferenceResult(
        inferenceId = inferenceId,
        windowId = windowId,
        evaluatedAtEpochMs = evaluatedAtEpochMs,
        physiologicalProbability = physiologicalProbability,
        behavioralProbability = behavioralProbability,
        biometricSignalQuality = biometricSignalQuality,
        appliedPhysioWeight = appliedPhysioWeight,
        appliedBehavioralWeight = appliedBehavioralWeight,
        finalStressProbability = finalStressProbability,
        inhibitedByPhysicalActivity = inhibitedByPhysicalActivity,
        topFactor1 = StressFactorContribution(
            metricKey = top1MetricKey,
            zScore = top1ZScore,
            weightContribution = top1WeightContribution,
            humanReadableLabelEs = top1LabelEs
        ),
        topFactor2 = if (
            top2MetricKey != null &&
            top2ZScore != null &&
            top2WeightContribution != null &&
            top2LabelEs != null
        ) {
            StressFactorContribution(
                metricKey = top2MetricKey,
                zScore = top2ZScore,
                weightContribution = top2WeightContribution,
                humanReadableLabelEs = top2LabelEs
            )
        } else {
            null
        },
        explanationSummaryEs = explanationSummaryEs
    )
}

fun StressInferenceResult.toEntity(): StressInferenceEntity = StressInferenceEntity(
    inferenceId = inferenceId,
    windowId = windowId,
    evaluatedAtEpochMs = evaluatedAtEpochMs,
    physiologicalProbability = physiologicalProbability,
    behavioralProbability = behavioralProbability,
    biometricSignalQuality = biometricSignalQuality,
    appliedPhysioWeight = appliedPhysioWeight,
    appliedBehavioralWeight = appliedBehavioralWeight,
    finalStressProbability = finalStressProbability,
    inhibitedByPhysicalActivity = inhibitedByPhysicalActivity,
    top1MetricKey = topFactor1.metricKey,
    top1ZScore = topFactor1.zScore,
    top1WeightContribution = topFactor1.weightContribution,
    top1LabelEs = topFactor1.humanReadableLabelEs,
    top2MetricKey = topFactor2?.metricKey,
    top2ZScore = topFactor2?.zScore,
    top2WeightContribution = topFactor2?.weightContribution,
    top2LabelEs = topFactor2?.humanReadableLabelEs,
    explanationSummaryEs = explanationSummaryEs
)

