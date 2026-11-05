package com.synapseguard.core.database.entity

import androidx.room.TypeConverter
import com.synapseguard.core.model.ActivityState
import com.synapseguard.core.model.CircadianBlock
import com.synapseguard.core.model.InterventionOutcome
import com.synapseguard.core.model.InterventionType

/**
 * Conversores de tipos de dominio para almacenamiento compacto en Room/SQLCipher.
 */
class SynapseTypeConverters {

    @TypeConverter
    fun fromCircadianBlock(block: CircadianBlock): Int = block.id

    @TypeConverter
    fun toCircadianBlock(id: Int): CircadianBlock = CircadianBlock.fromId(id)

    @TypeConverter
    fun fromActivityState(state: ActivityState): String = state.name

    @TypeConverter
    fun toActivityState(value: String): ActivityState = ActivityState.valueOf(value)

    @TypeConverter
    fun fromInterventionType(type: InterventionType): String = type.name

    @TypeConverter
    fun toInterventionType(value: String): InterventionType = InterventionType.valueOf(value)

    @TypeConverter
    fun fromInterventionOutcome(outcome: InterventionOutcome): String = outcome.name

    @TypeConverter
    fun toInterventionOutcome(value: String): InterventionOutcome = InterventionOutcome.valueOf(value)
}

