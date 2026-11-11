package org.lerchenflo.schneaggchatv3mp.feedback.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntry
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackMark
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackStatus
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackTag
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackVote
import org.lerchenflo.schneaggchatv3mp.feedback.presentation.uielements.FeedbackRow
import org.lerchenflo.schneaggchatv3mp.feedback.presentation.uielements.FeedbackCreateDialog
import org.lerchenflo.schneaggchatv3mp.feedback.presentation.uielements.FeedbackDetailDialog
import org.lerchenflo.schneaggchatv3mp.feedback.presentation.uielements.previewFeedbackEntry
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.CountSegmentedSwitch
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.feedback_create
import schneaggchatv3mp.composeapp.generated.resources.feedback_empty
import schneaggchatv3mp.composeapp.generated.resources.feedback_filter_all_statuses
import schneaggchatv3mp.composeapp.generated.resources.feedback_filter_all_tags
import schneaggchatv3mp.composeapp.generated.resources.feedback_filter_mine
import schneaggchatv3mp.composeapp.generated.resources.feedback_filter_status
import schneaggchatv3mp.composeapp.generated.resources.feedback_filter_tag
import schneaggchatv3mp.composeapp.generated.resources.feedback_load_error
import schneaggchatv3mp.composeapp.generated.resources.feedback_retry
import schneaggchatv3mp.composeapp.generated.resources.feedback_title

@Composable
fun FeedbackScreenRoot() {
    val viewModel = koinViewModel<FeedbackViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    FeedbackScreen(
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
fun FeedbackScreen(
    state: FeedbackState,
    onAction: (FeedbackAction) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ActivityTitle(
                title = stringResource(Res.string.feedback_title),
                onBackClick = { onAction(FeedbackAction.OnBackClick) }
            )

            HorizontalDivider()

            CountSegmentedSwitch(
                options = FeedbackTab.entries,
                selected = state.selectedTab,
                label = { stringResource(it.labelRes()) },
                count = { tab -> if (state.isLoading) null else state.tabCounts[tab] ?: 0 },
                onSelect = { onAction(FeedbackAction.OnTabSelected(it)) },
                // Four labels with counts: the check icon would not fit on a phone
                showLeadingIcon = false,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (state.selectedTab == FeedbackTab.GAMES) {
                GamesSectionChips(state = state, onAction = onAction)
            }

            FilterBar(state = state, onAction = onAction)

            // weight: the list takes what is left below the title, switch and chips. fillMaxWidth so
            // the centered spinner and messages sit in the middle, not at the left edge
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                    state.loadFailed -> CenteredMessage(text = stringResource(Res.string.feedback_load_error)) {
                        Button(onClick = { onAction(FeedbackAction.OnRetryClick) }) {
                            Text(stringResource(Res.string.feedback_retry))
                        }
                    }

                    state.visibleEntries.isEmpty() -> CenteredMessage(text = stringResource(Res.string.feedback_empty))

                    else -> LazyColumn(
                        // Bottom padding keeps the last row clear of the FAB
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(state.visibleEntries, key = { _, entry -> entry.id }) { index, entry ->
                            Column(modifier = Modifier.animateItem()) {
                                if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                FeedbackRow(
                                    entry = entry,
                                    mark = entry.boardMark(state),
                                    onClick = { onAction(FeedbackAction.OnEntryClick(entry)) },
                                    onVoteClick = { vote -> onAction(FeedbackAction.OnVoteClick(entry.id, vote)) },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!state.isLoading && !state.loadFailed) {
            ExtendedFloatingActionButton(
                onClick = { onAction(FeedbackAction.OnCreateClick) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(Res.string.feedback_create)) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }

    state.detail?.let { detail ->
        FeedbackDetailDialog(
            detailState = detail,
            viewerIsAdmin = state.viewerIsAdmin,
            onAction = onAction,
        )
    }

    if (state.isCreateDialogOpen) {
        FeedbackCreateDialog(
            initialType = state.section.createType,
            isSubmitting = state.isCreating,
            onSubmit = { onAction(FeedbackAction.OnCreateSubmit(it)) },
            onDismiss = { onAction(FeedbackAction.OnCreateDismiss) },
            // From the Games tab the new entry should land there again
            initialTags = if (state.selectedTab == FeedbackTab.GAMES) listOf(FeedbackTag.GAMES) else emptyList(),
        )
    }
}

/**
 * New / Updated label of a row: news since the last visit, and own entries younger than two days
 * (pinned on top in the Top sort) count as new too.
 */
private fun FeedbackEntry.boardMark(state: FeedbackState): FeedbackMark? =
    state.lastSeenBeforeVisit?.let { markSince(it) }
        ?: FeedbackMark.NEW.takeIf { state.sort == FeedbackSort.TOP && isPinned(state.nowMillis) }

/** Order of the chips on the Games tab. */
private val GAMES_SECTIONS = listOf(FeedbackSection.IMPLEMENTED, FeedbackSection.REQUESTS, FeedbackSection.BUGS)

/** Single-choice chips of the Games tab: implemented games, game wishes or game bugs. */
@Composable
private fun GamesSectionChips(
    state: FeedbackState,
    onAction: (FeedbackAction) -> Unit,
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GAMES_SECTIONS.forEach { section ->
            val label = stringResource(section.labelRes())
            val count = state.gamesSectionCounts[section]
            FilterChip(
                selected = section == state.gamesSection,
                onClick = { onAction(FeedbackAction.OnGamesSectionSelected(section)) },
                label = { Text(if (state.isLoading || count == null) label else "$label ($count)") },
            )
        }
    }
}

/**
 * GitHub-like row of dropdown filters: sort, status (not on Implemented), tag (not on Games, where
 * every entry is tagged GAMES), and "mine".
 */
@Composable
private fun FilterBar(
    state: FeedbackState,
    onAction: (FeedbackAction) -> Unit,
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DropdownFilterChip(
            options = FeedbackSort.entries,
            selected = state.sort,
            // Always a sort, so the chip always looks selected
            isActive = true,
            chipLabel = { stringResource(state.sort.labelRes()) },
            optionLabel = { stringResource(it.labelRes()) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
            onSelect = { it?.let { sort -> onAction(FeedbackAction.OnSortSelected(sort)) } },
        )

        if (state.section.filterStatuses.isNotEmpty()) {
            DropdownFilterChip(
                options = state.section.filterStatuses,
                selected = state.statusFilter,
                isActive = state.statusFilter != null,
                chipLabel = { it?.let { status -> stringResource(status.labelRes()) } ?: stringResource(Res.string.feedback_filter_status) },
                optionLabel = { stringResource(it.labelRes()) },
                allLabel = stringResource(Res.string.feedback_filter_all_statuses),
                onSelect = { onAction(FeedbackAction.OnStatusFilterSelected(it)) },
            )
        }

        if (state.selectedTab != FeedbackTab.GAMES) {
            DropdownFilterChip(
                options = FeedbackTag.entries,
                selected = state.tagFilter,
                isActive = state.tagFilter != null,
                chipLabel = { it?.let { tag -> stringResource(tag.labelRes()) } ?: stringResource(Res.string.feedback_filter_tag) },
                optionLabel = { stringResource(it.labelRes()) },
                allLabel = stringResource(Res.string.feedback_filter_all_tags),
                onSelect = { onAction(FeedbackAction.OnTagFilterSelected(it)) },
            )
        }

        FilterChip(
            selected = state.onlyMine,
            onClick = { onAction(FeedbackAction.OnOnlyMineToggle) },
            label = { Text(stringResource(Res.string.feedback_filter_mine)) },
        )
    }
}

/**
 * Filter chip that opens a menu of [options]. With [allLabel] the menu starts with an entry that
 * clears the filter (selects null). The current choice is marked with a check.
 */
@Composable
private fun <T : Any> DropdownFilterChip(
    options: List<T>,
    selected: T?,
    isActive: Boolean,
    chipLabel: @Composable (T?) -> String,
    optionLabel: @Composable (T) -> String,
    onSelect: (T?) -> Unit,
    allLabel: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = isActive,
            onClick = { menuOpen = true },
            label = { Text(chipLabel(selected)) },
            leadingIcon = leadingIcon,
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            allLabel?.let { label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    trailingIcon = { if (selected == null) Icon(Icons.Default.Check, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onSelect(null)
                    }
                )
            }
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    trailingIcon = { if (option == selected) Icon(Icons.Default.Check, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun CenteredMessage(
    text: String,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        action?.invoke()
    }
}

@Preview
@Composable
private fun FeedbackScreenPreview() {
    val entries = listOf(
        previewFeedbackEntry,
        previewFeedbackEntry.copy(
            id = "2",
            title = "Share your live location",
            description = "Share your location with chosen friends and see them on the map.",
            status = FeedbackStatus.IMPLEMENTED,
            myVote = FeedbackVote.DIDNT_KNOW,
            upvotes = 30,
            downvotes = 1,
            didntKnowCount = 7,
            notNeededCount = 2,
            latestDevComment = null,
            isSeeded = true,
            isPremium = true,
        ),
    )
    SchneaggchatTheme {
        FeedbackScreen(
            state = FeedbackState(
                entries = entries,
                visibleEntries = entries,
                isLoading = false,
                lastSeenBeforeVisit = 0L,
            ),
            onAction = {}
        )
    }
}
