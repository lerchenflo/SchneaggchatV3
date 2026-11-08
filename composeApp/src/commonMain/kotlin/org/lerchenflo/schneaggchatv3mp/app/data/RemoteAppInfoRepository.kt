package org.lerchenflo.schneaggchatv3mp.app.data

import org.lerchenflo.schneaggchatv3mp.GITHUB_URL
import org.lerchenflo.schneaggchatv3mp.app.domain.AppInfoRepository
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
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
            is DataResult.Error<*> -> false
            is DataResult.Success<*> -> true
        }
    }

    override suspend fun getChangeLog(version: String) : ChangelogEntry? {
        return when (val networkResult = appInfoRemoteDataSource.getChangeLog("$GITHUB_URL/main/README.md")) {
            is DataResult.Error<*> -> {
                println("get changelog network error: ${networkResult.error.message}")
                null
            }
            is DataResult.Success<*> -> {
                // A desktop beta (3.0.19_beta) shares the README section of its final version
                val readmeVersion = version.substringBefore('_').substringBefore('-')
                val changelog = ChangelogParser.getParsedChangelog(networkResult.data.toString(), readmeVersion)
                changelog
            }
        }
    }
}
