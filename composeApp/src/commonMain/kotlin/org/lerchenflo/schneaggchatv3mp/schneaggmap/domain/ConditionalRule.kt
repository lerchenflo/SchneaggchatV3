package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

import kotlinx.serialization.Serializable

@Serializable
data class ConditionalRule(
    val attributeKey: String,
    val requiredIfSubtypeNames: Set<String>,
)
