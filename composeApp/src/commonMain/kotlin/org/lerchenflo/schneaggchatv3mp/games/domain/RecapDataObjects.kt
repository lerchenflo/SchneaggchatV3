package org.lerchenflo.schneaggchatv3mp.games.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat

data class TopContact(
    val selectedChat: SelectedChat?,
    val msgCount: Int
)

data class RecapData(
    val year: Int = 2026,
    val totalMessagesSent: Int,
    val totalMessagesReceived: Int,
    val topContacts: List<TopContact>,

)