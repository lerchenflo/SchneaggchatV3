package org.lerchenflo.schneaggchatv3mp.fakes

import org.lerchenflo.schneaggchatv3mp.app.domain.AppInfoRepository
import org.lerchenflo.schneaggchatv3mp.utilities.ChangelogEntry

/** [AppInfoRepository] answering with the values set on it; nothing goes over the network. */
class FakeAppInfoRepository(
    var installedVersion: String = "1.0.0",
    var changelog: ChangelogEntry? = null,
    var latestGitHubVersion: String? = null,
    var serverReachable: Boolean = true,
) : AppInfoRepository {

    val testedServers = CallLog<String>()
    val changelogRequests = CallLog<String>()

    override fun getVersionName(): String = installedVersion

    override suspend fun testServer(serverUrl: String): Boolean {
        testedServers.record(serverUrl)
        return serverReachable
    }

    override suspend fun getChangeLog(version: String): ChangelogEntry? {
        changelogRequests.record(version)
        return changelog
    }

    override suspend fun getLatestGitHubVersionAsString(): String? = latestGitHubVersion
}
