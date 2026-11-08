package org.lerchenflo.schneaggchatv3mp.desktopupdate.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.flow.first
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.safeNetworkCall
import org.lerchenflo.schneaggchatv3mp.desktopupdate.data.remote.parseGitHubReleases
import org.lerchenflo.schneaggchatv3mp.desktopupdate.domain.AppRelease
import org.lerchenflo.schneaggchatv3mp.desktopupdate.domain.DesktopUpdateRepository
import org.lerchenflo.schneaggchatv3mp.desktopupdate.domain.isBetaVersion

private const val GITHUB_RELEASES_API_URL = "https://api.github.com/repos/lerchenflo/SchneaggchatV3/releases?per_page=20"

private val DISMISSED_BETA_VERSION = stringPreferencesKey("dismissed_beta_version")

/** [DesktopUpdateRepository] reading the releases from the GitHub API and the closed beta from DataStore. */
class GitHubUpdateRepository(
    private val httpClient: HttpClient, //Without the bearer, GitHub must not get the access token
    private val prefs: DataStore<Preferences>,
    private val loggingRepository: Logger,
) : DesktopUpdateRepository {

    override suspend fun getReleases(): List<AppRelease> {
        val result = safeNetworkCall<String>(loggingRepository, "GET $GITHUB_RELEASES_API_URL") {
            httpClient.get(GITHUB_RELEASES_API_URL)
        }
        return when (result) {
            is DataResult.Error -> emptyList()
            is DataResult.Success -> parseGitHubReleases(result.data).map { release ->
                AppRelease(
                    tagName = release.tagName,
                    // A tag with "beta" counts as beta, also on a full release
                    isBeta = release.prerelease || isBetaVersion(release.tagName),
                    pageUrl = release.htmlUrl,
                )
            }
        }
    }

    override suspend fun getDismissedBetaVersion(): String? = prefs.data.first()[DISMISSED_BETA_VERSION]

    override suspend fun saveDismissedBetaVersion(tagName: String) {
        prefs.edit { it[DISMISSED_BETA_VERSION] = tagName }
    }
}
