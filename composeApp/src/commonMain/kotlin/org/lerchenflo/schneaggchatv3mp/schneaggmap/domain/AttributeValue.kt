package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class AttributeValue {
    @Serializable
    @SerialName("string") data class StringValue(val value: String) : AttributeValue()

    @Serializable
    @SerialName("int") data class IntValue(val value: Int) : AttributeValue()

    @Serializable
    @SerialName("double") data class DoubleValue(val value: Double) : AttributeValue()

    @Serializable
    @SerialName("bool") data class BoolValue(val value: Boolean) : AttributeValue()

    @Serializable
    @SerialName("enum")
    data class EnumValue(val value: String) : AttributeValue()
}
