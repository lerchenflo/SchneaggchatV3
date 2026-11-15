package org.lerchenflo.schneaggchatv3mp.settings.domain

/**
 * Who may see the own profile picture. Synced with the server by the enum's name; the server
 * applies it on every way to the picture.
 */
enum class ProfilePicVisibility {
    PUBLIC,
    FRIENDS_AND_GROUPS,
    FRIENDS;

    companion object {
        // The server's default for every user, existing ones included
        val DEFAULT = FRIENDS_AND_GROUPS

        fun fromName(name: String?): ProfilePicVisibility = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
