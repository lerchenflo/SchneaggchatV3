package org.lerchenflo.schneaggchatv3mp.datasource.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class SystemEventResponse(
    val eventType: String,
    val actorId: String,
    val actorName: String,
    val targets: List<SystemEventParticipantResponse> = emptyList(),
    val text: String? = null,
    val previousText: String? = null,
)

@Serializable
data class SystemEventParticipantResponse(
    val userId: String,
    val userName: String,
)
