package org.lerchenflo.schneaggchatv3mp.roadmap.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.utilities.ChangelogEntry

class RoadmapViewModel(
    private val appRepository: AppRepository
) : ViewModel() {

    var entries by mutableStateOf<List<ChangelogEntry>>(emptyList())
        private set

    var upcomingIssues by mutableStateOf<List<RoadmapUpcomingIssue>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var loadFailed by mutableStateOf(false)
        private set

    init {
        loadRoadmap()
    }

    fun loadRoadmap() {
        viewModelScope.launch {
            isLoading = true
            loadFailed = false

            // Fetched in parallel: the changelog (README) and the open GitHub issues are
            // independent requests to different hosts.
            val changelogDeferred = async { appRepository.getFullChangelog() }
            val issuesDeferred = async { appRepository.getOpenGithubIssues() }

            val changelog = changelogDeferred.await()
            if (changelog == null) {
                loadFailed = true
            } else {
                entries = changelog
            }

            // A failed issues fetch just leaves "Upcoming" empty rather than failing the
            // whole screen, since the shipped-version timeline is the more important part.
            upcomingIssues = issuesDeferred.await()
                ?.map { it.toRoadmapUpcomingIssue() }
                .orEmpty()

            isLoading = false
        }
    }
}
