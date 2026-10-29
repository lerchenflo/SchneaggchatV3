package org.lerchenflo.schneaggchatv3mp.chat.domain

/**
 * Quick reactions offered on a long press of a message. The user's own list is a per-account
 * setting synced via [org.lerchenflo.schneaggchatv3mp.datasource.network.dto.PersonalUserSettings.quickReactions];
 * these values are what a user who never customised it sees. The stored list being null - not
 * empty - is what marks "never customised", so changing this set later reaches those users too.
 *
 * The first entry is the reaction a double tap on a message toggles; the long press popup shows
 * only the rest. The heart leads the defaults so double tap keeps giving the heart it gave before
 * the list was customisable.
 */
val DEFAULT_QUICK_REACTIONS = listOf("❤️", "👍", "👎", "😂", "🍻")

/** Max length of a single reaction, mirroring the server's ValidationUtils.validateReactionContent. */
const val MAX_REACTION_LENGTH = 10
