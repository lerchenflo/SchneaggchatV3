package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Minimal subset of the GitHub REST API "issue" object.
 * https://docs.github.com/en/rest/issues/issues#list-repository-issues
 *
 * GitHub returns pull requests through the same endpoint; those additionally carry a
 * "pull_request" field, which regular issues never have. [pullRequestMarker] is only used
 * to detect and filter those out.
 */
@Serializable
data class GithubIssueDto(
    val number: Int,
    val title: String,
    val body: String? = null,
    @SerialName("pull_request") val pullRequestMarker: GithubPullRequestMarker? = null
) {
    val isPullRequest: Boolean get() = pullRequestMarker != null
}

@Serializable
data class GithubPullRequestMarker(
    val url: String? = null
)
