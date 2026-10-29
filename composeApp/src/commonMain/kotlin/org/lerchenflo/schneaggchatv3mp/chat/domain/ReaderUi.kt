package org.lerchenflo.schneaggchatv3mp.chat.domain

import androidx.compose.runtime.Immutable

/**
 * Presentation model for one reader of a message: display name, cached profile-picture path and
 * read timestamp - resolved once by MessageDisplayMapper instead of being mutated onto this
 * MessageReader (which used to force Compose to treat every message row as unstable, since an
 * all-var domain type can't be trusted as immutable if something keeps writing to it).
 */
@Immutable
data class ReaderUi(
    val id: String,
    val name: String,
    val picturePath: String,
    val readAtMillis: Long,
)
