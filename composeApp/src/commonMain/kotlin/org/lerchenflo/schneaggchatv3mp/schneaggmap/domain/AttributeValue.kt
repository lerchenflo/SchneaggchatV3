package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class AttributeValue {
    @Serializable @SerialName("string") data class StringVal(val value: String) : AttributeValue()
    @Serializable @SerialName("int") data class IntVal(val value: Int) : AttributeValue()
    @Serializable @SerialName("double") data class DoubleVal(val value: Double) : AttributeValue()
    @Serializable @SerialName("bool") data class BoolVal(val value: Boolean) : AttributeValue()
}
