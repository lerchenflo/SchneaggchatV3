package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

data class MainType(
    val key: String,
    val displayName: String,
    val attributeDefinitions: List<AttributeDefinition>,
    val conditionalRules: List<ConditionalRule>,
)
