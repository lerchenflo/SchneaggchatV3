package org.lerchenflo.schneaggchatv3mp.desktopupdate.domain

/** GitHub releases for the desktop update banner, and the beta whose banner the user closed. */
interface DesktopUpdateRepository {

    /** The recent GitHub releases, betas included, empty on error. */
    suspend fun getReleases(): List<AppRelease>

    /** Tag of the beta whose banner the user closed, null if none. Local only, not synced. */
    suspend fun getDismissedBetaVersion(): String?

    suspend fun saveDismissedBetaVersion(tagName: String)
}
