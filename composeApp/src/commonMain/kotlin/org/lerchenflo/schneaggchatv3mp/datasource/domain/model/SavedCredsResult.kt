package org.lerchenflo.schneaggchatv3mp.datasource.domain.model

/**
 * Result of [org.lerchenflo.schneaggchatv3mp.datasource.domain.AuthRepository.loadSavedLoginConfig]:
 * whether a usable session is stored on this device and whether the own user's email is verified.
 */
data class SavedCredsResult(
    val credsSaved: Boolean,
    val emailVerified: Boolean
)
