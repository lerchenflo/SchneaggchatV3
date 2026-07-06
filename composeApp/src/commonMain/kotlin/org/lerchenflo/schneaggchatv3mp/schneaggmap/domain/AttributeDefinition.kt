package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface AttributeDefinition {
    val key: String
    val required: Boolean

    @Serializable @SerialName("string")
    data class StringDef(
        override val key: String,
        override val required: Boolean,
        val maxLength: Int? = null,
    ) : AttributeDefinition

    @Serializable @SerialName("int")
    data class IntDef(
        override val key: String,
        override val required: Boolean,
        val min: Int? = null,
        val max: Int? = null,
    ) : AttributeDefinition

    @Serializable @SerialName("double")
    data class DoubleDef(
        override val key: String,
        override val required: Boolean,
        val min: Double? = null,
        val max: Double? = null,
    ) : AttributeDefinition

    @Serializable @SerialName("long")
    data class LongDef(
        override val key: String,
        override val required: Boolean,
        val min: Long? = null,
        val max: Long? = null,
    ) : AttributeDefinition

    @Serializable @SerialName("bool")
    data class BoolDef(
        override val key: String,
        override val required: Boolean,
    ) : AttributeDefinition
}
