package org.lerchenflo.schneaggchatv3mp.app.domain

import org.lerchenflo.schneaggchatv3mp.BASE_SERVER_URL
import org.lerchenflo.schneaggchatv3mp.utilities.ChangelogEntry

/** Information about the app itself: installed version, changelog, latest release, server reachability. */
interface AppInfoRepository {

    /** Version name of the installed app, e.g. "3.4.1". */
    fun getVersionName(): String

    /** @return true if the server at [serverUrl] answers */
    suspend fun testServer(serverUrl: String = BASE_SERVER_URL): Boolean

    /** The changelog entry for [version] from the README on GitHub, null if offline or not found. */
    suspend fun getChangeLog(version: String): ChangelogEntry?

    /** Fetches the name of the current GitHub release, null on error. */
    suspend fun getLatestGitHubVersionAsString(): String?
}
