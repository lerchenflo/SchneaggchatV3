package org.lerchenflo.schneaggchatv3mp.roadmap.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pushpal.jetlime.EventPointType
import com.pushpal.jetlime.ItemsList
import com.pushpal.jetlime.JetLimeColumn
import com.pushpal.jetlime.JetLimeDefaults
import com.pushpal.jetlime.JetLimeEvent
import com.pushpal.jetlime.JetLimeEventDefaults
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.utilities.ChangelogEntry
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.changelog_announcements
import schneaggchatv3mp.composeapp.generated.resources.changelog_bug_fixes
import schneaggchatv3mp.composeapp.generated.resources.changelog_features
import schneaggchatv3mp.composeapp.generated.resources.changelog_version
import schneaggchatv3mp.composeapp.generated.resources.roadmap
import schneaggchatv3mp.composeapp.generated.resources.roadmap_load_error
import schneaggchatv3mp.composeapp.generated.resources.roadmap_retry
import schneaggchatv3mp.composeapp.generated.resources.roadmap_status_done
import schneaggchatv3mp.composeapp.generated.resources.roadmap_status_upcoming
import schneaggchatv3mp.composeapp.generated.resources.roadmap_view_issue

@Composable
fun RoadmapScreen(
    modifier: Modifier = Modifier.fillMaxWidth(),
    roadmapViewModel: RoadmapViewModel,
    onBackClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ActivityTitle(
            title = stringResource(Res.string.roadmap),
            onBackClick = onBackClick
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        when {
            roadmapViewModel.isLoading -> {
                Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            roadmapViewModel.loadFailed -> {
                Column(
                    modifier = modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(Res.string.roadmap_load_error),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { roadmapViewModel.loadRoadmap() },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(stringResource(Res.string.roadmap_retry))
                    }
                }
            }

            else -> {
                RoadmapTimeline(
                    modifier = modifier,
                    entries = roadmapViewModel.entries,
                    upcomingIssues = roadmapViewModel.upcomingIssues
                )
            }
        }
    }
}

@Composable
private fun RoadmapTimeline(
    modifier: Modifier,
    entries: List<ChangelogEntry>,
    upcomingIssues: List<RoadmapUpcomingIssue>
) {
    // Upcoming items are the currently open GitHub issues, fetched live and shown ahead
    // of the shipped versions, which come from the README changelog newest-first.
    val items = upcomingIssues.map { RoadmapListItem.Upcoming(it) } +
        entries.map { RoadmapListItem.Shipped(it) }

    val listState = rememberLazyListState()

    // Jump straight to the most recently shipped version on open, past the (possibly long)
    // list of open issues, so the newest finished work is what's visible first.
    LaunchedEffect(Unit) {
        if (items.isNotEmpty()) {
            listState.scrollToItem(upcomingIssues.size.coerceIn(0, items.size - 1))
        }
    }

    JetLimeColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        itemsList = ItemsList(items),
        style = JetLimeDefaults.columnStyle(itemSpacing = 20.dp),
        listState = listState,
        key = { _, item ->
            when (item) {
                is RoadmapListItem.Upcoming -> "issue-${item.issue.issueNumber}"
                is RoadmapListItem.Shipped -> item.entry.version
            }
        }
    ) { _, item, position ->
        when (item) {
            is RoadmapListItem.Upcoming -> {
                JetLimeEvent(
                    style = JetLimeEventDefaults.eventStyle(
                        position = position,
                        pointType = EventPointType.EMPTY,
                        pointColor = MaterialTheme.colorScheme.surface,
                        pointStrokeColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    val uriHandler = LocalUriHandler.current
                    RoadmapCard(
                        title = item.issue.title,
                        statusLabel = stringResource(Res.string.roadmap_status_upcoming),
                        statusColor = MaterialTheme.colorScheme.secondary
                    ) {
                        Text(
                            text = item.issue.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(Res.string.roadmap_view_issue, item.issue.issueNumber),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                uriHandler.openUri(
                                    "https://github.com/lerchenflo/SchneaggchatV3/issues/${item.issue.issueNumber}"
                                )
                            }
                        )
                    }
                }
            }

            is RoadmapListItem.Shipped -> {
                JetLimeEvent(
                    style = JetLimeEventDefaults.eventStyle(
                        position = position,
                        pointType = EventPointType.filled(1f)
                    )
                ) {
                    RoadmapCard(
                        title = stringResource(Res.string.changelog_version, item.entry.version),
                        statusLabel = stringResource(Res.string.roadmap_status_done),
                        statusColor = MaterialTheme.colorScheme.primary
                    ) {
                        RoadmapEntryContent(item.entry)
                    }
                }
            }
        }
    }
}

private sealed interface RoadmapListItem {
    data class Upcoming(val issue: RoadmapUpcomingIssue) : RoadmapListItem
    data class Shipped(val entry: ChangelogEntry) : RoadmapListItem
}

@Composable
private fun RoadmapCard(
    title: String,
    statusLabel: String,
    statusColor: Color,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelMedium,
                color = statusColor,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun RoadmapEntryContent(entry: ChangelogEntry) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (entry.announcements.isNotEmpty()) {
            RoadmapSection(title = stringResource(Res.string.changelog_announcements), items = entry.announcements)
        }
        if (entry.features.isNotEmpty()) {
            RoadmapSection(title = stringResource(Res.string.changelog_features), items = entry.features)
        }
        if (entry.bugfixes.isNotEmpty()) {
            RoadmapSection(title = stringResource(Res.string.changelog_bug_fixes), items = entry.bugfixes)
        }
    }
}

@Composable
private fun RoadmapSection(title: String, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        items.forEach { item ->
            Text(
                text = "•  $item",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
