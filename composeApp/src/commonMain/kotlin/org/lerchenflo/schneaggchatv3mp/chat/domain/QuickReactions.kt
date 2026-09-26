package org.lerchenflo.schneaggchatv3mp.chat.domain

/**
 * Quick reactions offered on a long press of a message. The user's own list is a per-account
 * setting synced via [org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.PersonalUserSettings.quickReactions];
 * these values are what a user who never customised it sees. The stored list being null - not
 * empty - is what marks "never customised", so changing this set later reaches those users too.
 */
val DEFAULT_QUICK_REACTIONS = listOf("👍", "👎", "😂", "🍻")

/** Max length of a single reaction, mirroring the server's ValidationUtils.validateReactionContent. */
const val MAX_REACTION_LENGTH = 10
