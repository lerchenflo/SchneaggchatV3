package org.lerchenflo.schneaggchatv3mp.app.data.remote

import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError

/** Calls outside the logged-in session: the server reachability test and the GitHub changelog / release lookups. */
interface AppInfoRemoteDataSource {

    suspend fun testServer(serverUrl: String) : NetworkResult<String, NetworkingError>

    suspend fun getChangeLog(githubUrl: String) : NetworkResult<String, NetworkingError>

    suspend fun getGitHubVersionJson(url: String) : NetworkResult<String, NetworkingError>
}
