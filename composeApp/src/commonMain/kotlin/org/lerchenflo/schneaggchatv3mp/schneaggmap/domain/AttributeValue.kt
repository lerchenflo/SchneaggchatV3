package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class AttributeValue {

    @Serializable
    @SerialName("string")
    data class StringValue(val value: String) : AttributeValue()

    @Serializable
    @SerialName("int")
    data class IntValue(val value: Int) : AttributeValue()

    @Serializable
    @SerialName("double")
    data class DoubleValue(val value: Double) : AttributeValue()

    @Serializable
    @SerialName("long")
    data class LongValue(val value: Long) : AttributeValue()

    @Serializable
    @SerialName("bool")
    data class BoolValue(val value: Boolean) : AttributeValue()
}

// Convenience getters — throw with a clear message if the wrong type is accessed
val AttributeValue.asString: String
    get() = (this as? AttributeValue.StringValue)?.value
        ?: error("Expected StringValue but was ${this::class.simpleName}")

val AttributeValue.asInt: Int
    get() = (this as? AttributeValue.IntValue)?.value
        ?: error("Expected IntValue but was ${this::class.simpleName}")

val AttributeValue.asDouble: Double
    get() = (this as? AttributeValue.DoubleValue)?.value
        ?: error("Expected DoubleValue but was ${this::class.simpleName}")

val AttributeValue.asLong: Long
    get() = (this as? AttributeValue.LongValue)?.value
        ?: error("Expected LongValue but was ${this::class.simpleName}")

val AttributeValue.asBool: Boolean
    get() = (this as? AttributeValue.BoolValue)?.value
        ?: error("Expected BoolValue but was ${this::class.simpleName}")