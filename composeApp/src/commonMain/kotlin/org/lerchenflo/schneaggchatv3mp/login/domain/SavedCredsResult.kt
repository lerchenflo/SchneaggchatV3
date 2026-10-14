package org.lerchenflo.schneaggchatv3mp.login.domain

/**
 * Result of [org.lerchenflo.schneaggchatv3mp.login.domain.AuthRepository.loadSavedLoginConfig]:
 * whether a usable session is stored on this device and whether the own user's email is verified.
 */
data class SavedCredsResult(
    val credsSaved: Boolean,
    val emailVerified: Boolean
)
