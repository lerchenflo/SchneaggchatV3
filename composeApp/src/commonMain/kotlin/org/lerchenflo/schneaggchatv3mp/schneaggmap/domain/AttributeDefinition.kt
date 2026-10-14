package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed interface AttributeDefinition {
    val key: AttributeKey
    val required: Boolean

    @Serializable @SerialName("string")
    data class StringDef(
        override val key: AttributeKey,
        override val required: Boolean,
        val maxLength: Int? = null,
    ) : AttributeDefinition

    @Serializable @SerialName("int")
    data class IntDef(
        override val key: AttributeKey,
        override val required: Boolean,
        val min: Int? = null,
        val max: Int? = null,
    ) : AttributeDefinition

    @Serializable @SerialName("double")
    data class DoubleDef(
        override val key: AttributeKey,
        override val required: Boolean,
        val min: Double? = null,
        val max: Double? = null,
    ) : AttributeDefinition

    @Serializable @SerialName("long")
    data class LongDef(
        override val key: AttributeKey,
        override val required: Boolean,
        val min: Long? = null,
        val max: Long? = null,
    ) : AttributeDefinition

    @Serializable @SerialName("bool")
    data class BoolDef(
        override val key: AttributeKey,
        override val required: Boolean,
    ) : AttributeDefinition

    /**
     * One choice out of [options]. Stored as an [AttributeValue.StringValue] holding the option's
     * enum constant name (never its ordinal), so no new wire/DB value type is needed.
     * [options] is code-only (carries string resources) and therefore not serialized.
     */
    @Serializable @SerialName("enum")
    data class EnumDef(
        override val key: AttributeKey,
        override val required: Boolean,
        @Transient val options: List<AttributeOption> = emptyList(),
    ) : AttributeDefinition

    /** A point in time as epoch millis. Stored as an [AttributeValue.LongValue]. */
    @Serializable @SerialName("datetime")
    data class DateTimeDef(
        override val key: AttributeKey,
        override val required: Boolean,
    ) : AttributeDefinition

    /** A price in euros, never negative. Stored as an [AttributeValue.DoubleValue]. */
    @Serializable @SerialName("price")
    data class PriceDef(
        override val key: AttributeKey,
        override val required: Boolean,
        val max: Double? = null,
    ) : AttributeDefinition

    /** A distance or length in [unit], never negative. Stored as an [AttributeValue.DoubleValue]. */
    @Serializable @SerialName("distance")
    data class DistanceDef(
        override val key: AttributeKey,
        override val required: Boolean,
        val unit: DistanceUnit,
        val max: Double? = null,
    ) : AttributeDefinition

    /** A whole-number rating from [min] to [max] (inclusive). Stored as an [AttributeValue.IntValue]. */
    @Serializable @SerialName("rating")
    data class RatingDef(
        override val key: AttributeKey,
        override val required: Boolean,
        val min: Int,
        val max: Int,
    ) : AttributeDefinition

    /** Sensitive text (e.g. a password), hidden by default. Stored as an [AttributeValue.StringValue]. */
    @Serializable @SerialName("secret")
    data class SecretDef(
        override val key: AttributeKey,
        override val required: Boolean,
        val maxLength: Int? = null,
    ) : AttributeDefinition
}

@Serializable
enum class DistanceUnit(val symbol: String) {
    METERS("m"),
    KILOMETERS("km"),
}
