package org.lerchenflo.schneaggchatv3mp.app.data

import org.lerchenflo.schneaggchatv3mp.GITHUB_LATEST_RELEASE_API_URL
import org.lerchenflo.schneaggchatv3mp.GITHUB_URL
import org.lerchenflo.schneaggchatv3mp.app.domain.AppInfoRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.trackConnectivity
import org.lerchenflo.schneaggchatv3mp.utilities.AppVersion
import org.lerchenflo.schneaggchatv3mp.utilities.ChangelogEntry
import org.lerchenflo.schneaggchatv3mp.utilities.ChangelogParser
import org.lerchenflo.schneaggchatv3mp.app.data.remote.AppInfoRemoteDataSource

/** [AppInfoRepository] reading the installed version from [AppVersion] and everything else live via [AppInfoRemoteDataSource]. */
class RemoteAppInfoRepository(
    private val appInfoRemoteDataSource: AppInfoRemoteDataSource,
    private val appVersion: AppVersion,
) : AppInfoRepository {

    override fun getVersionName(): String = appVersion.getVersionName()

    /**
     * Test the server endpoint
     */
    override suspend fun testServer(serverUrl: String) : Boolean {
        println("Testing server: $serverUrl")
        return when(appInfoRemoteDataSource.testServer(serverUrl).trackConnectivity()){
            is NetworkResult.Error<*> -> false
            is NetworkResult.Success<*> -> true
        }
    }

    override suspend fun getChangeLog(version: String) : ChangelogEntry? {
        return when (val networkResult = appInfoRemoteDataSource.getChangeLog("$GITHUB_URL/main/README.md")) {
            is NetworkResult.Error<*> -> {
                println("get changelog network error: ${networkResult.error.message}")
                null
            }
            is NetworkResult.Success<*> -> {
                val changelog = ChangelogParser.getParsedChangelog(networkResult.data.toString(), version)
                changelog
            }
        }
    }

    /**
     * Fetches the name of the current GitHub release
     */
    override suspend fun getLatestGitHubVersionAsString(): String? {
        return when (val networkResult = appInfoRemoteDataSource.getGitHubVersionJson(GITHUB_LATEST_RELEASE_API_URL)) {
            is NetworkResult.Error<*> -> {
                println("get release json network error: ${networkResult.error.message}")
                null
            }
            is NetworkResult.Success<*> -> {
                networkResult.data.toString()
            }
        }
    }
}
