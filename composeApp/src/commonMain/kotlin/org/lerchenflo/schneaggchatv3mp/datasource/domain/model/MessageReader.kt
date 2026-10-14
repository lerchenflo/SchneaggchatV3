package org.lerchenflo.schneaggchatv3mp.datasource.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class MessageReader(
    val readerEntryId: Long = 0L,
    val messageId: String,
    val readerId: String,
    val readDate: String = "",
) {
    fun getReadDateAsLong(): Long = readDate.toLongOrNull() ?: 0L
}
