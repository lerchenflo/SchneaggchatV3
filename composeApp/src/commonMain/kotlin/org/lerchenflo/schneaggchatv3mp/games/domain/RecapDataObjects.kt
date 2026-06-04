package org.lerchenflo.schneaggchatv3mp.games.domain

import androidx.compose.ui.graphics.Color

data class TopContact(
    val name: String,
    val totalMessages: Int,
    val dynamicColor: Color
)

data class ChatWrappedData(
    val year: Int = 2026,
    val totalMessages: Int,
    val totalHoursVoice: Int,
    val topContacts: List<TopContact>,
    val lateNightPercentage: Int, // e.g., 42% of texts sent after 11 PM
    val topEmojis: List<String>
)