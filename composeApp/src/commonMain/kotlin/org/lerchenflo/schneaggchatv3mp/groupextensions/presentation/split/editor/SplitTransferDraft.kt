package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.editor

import kotlinx.serialization.Serializable

/** Prefill of a new money transfer, e.g. from a "settle up" suggestion. Part of the editor route. */
@Serializable
data class SplitTransferDraft(
    val fromUserId: String,
    val toUserId: String,
    val amountCents: Long,
)
