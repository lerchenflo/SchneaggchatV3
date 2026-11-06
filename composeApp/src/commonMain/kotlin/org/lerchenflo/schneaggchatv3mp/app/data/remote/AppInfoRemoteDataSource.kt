package org.lerchenflo.schneaggchatv3mp.app.data.remote

import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult

/** Calls outside the logged-in session: the server reachability test and the GitHub changelog / release lookups. */
interface AppInfoRemoteDataSource {

    suspend fun testServer(serverUrl: String) : DataResult<String, DataError>

    suspend fun getChangeLog(githubUrl: String) : DataResult<String, DataError>

    suspend fun getGitHubVersionJson(url: String) : DataResult<String, DataError>
}
