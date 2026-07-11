package org.lerchenflo.schneaggchatv3mp.roadmap.presentation

import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.GithubIssueDto

/**
 * One planned/open item on the roadmap, derived from a GitHub issue.
 */
data class RoadmapUpcomingIssue(
    val issueNumber: Int,
    val title: String,
    val description: String
)

/**
 * Turns a live-fetched GitHub issue into a [RoadmapUpcomingIssue]: the issue title as-is,
 * and the first non-blank line of its body as a short description.
 */
fun GithubIssueDto.toRoadmapUpcomingIssue(): RoadmapUpcomingIssue {
    val description = body
        ?.lineSequence()
        ?.map { it.trim() }
        ?.firstOrNull { it.isNotEmpty() }
        ?.take(160)
        .orEmpty()

    return RoadmapUpcomingIssue(
        issueNumber = number,
        title = title,
        description = description
    )
}
