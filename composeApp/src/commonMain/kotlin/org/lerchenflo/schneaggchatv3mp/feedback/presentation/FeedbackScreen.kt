package org.lerchenflo.schneaggchatv3mp.feedback.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackStatus
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackTag
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackType
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackVote
import org.lerchenflo.schneaggchatv3mp.feedback.presentation.uielements.FeedbackCard
import org.lerchenflo.schneaggchatv3mp.feedback.presentation.uielements.FeedbackCreateDialog
import org.lerchenflo.schneaggchatv3mp.feedback.presentation.uielements.FeedbackDetailDialog
import org.lerchenflo.schneaggchatv3mp.feedback.presentation.uielements.previewFeedbackEntry
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.CountSegmentedSwitch
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.feedback_create
import schneaggchatv3mp.composeapp.generated.resources.feedback_empty
import schneaggchatv3mp.composeapp.generated.resources.feedback_filter_all_tags
import schneaggchatv3mp.composeapp.generated.resources.feedback_filter_mine
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
                options = FeedbackType.entries,
                selected = state.selectedType,
                label = { stringResource(it.labelRes()) },
                count = { type ->
                    if (state.isLoading) null
                    else if (type == FeedbackType.FEATURE) state.features.size
                    else state.bugs.size
                },
                onSelect = { onAction(FeedbackAction.OnTypeSelected(it)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            FilterChipRow(state = state, onAction = onAction)

            // weight: the list takes what is left below the title, switch and chips
            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                    state.loadFailed -> CenteredMessage(text = stringResource(Res.string.feedback_load_error)) {
                        Button(onClick = { onAction(FeedbackAction.OnRetryClick) }) {
                            Text(stringResource(Res.string.feedback_retry))
                        }
                    }

                    state.visibleEntries.isEmpty() -> CenteredMessage(text = stringResource(Res.string.feedback_empty))

                    else -> LazyColumn(
                        // Bottom padding keeps the last card clear of the FAB
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.visibleEntries, key = { it.id }) { entry ->
                            FeedbackCard(
                                entry = entry,
                                isPinned = state.sort == FeedbackSort.TOP && entry.isPinned(state.nowMillis),
                                onClick = { onAction(FeedbackAction.OnEntryClick(entry)) },
                                onVoteClick = { vote -> onAction(FeedbackAction.OnVoteClick(entry.id, vote)) },
                                modifier = Modifier.animateItem()
                            )
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
            initialType = state.selectedType,
            isSubmitting = state.isCreating,
            onSubmit = { onAction(FeedbackAction.OnCreateSubmit(it)) },
            onDismiss = { onAction(FeedbackAction.OnCreateDismiss) },
        )
    }
}

/** Sort, status, tag and "mine" filters in one horizontally scrolling row. */
@Composable
private fun FilterChipRow(
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
        FeedbackSort.entries.forEach { sort ->
            FilterChip(
                selected = state.sort == sort,
                onClick = { onAction(FeedbackAction.OnSortSelected(sort)) },
                label = { Text(stringResource(sort.labelRes())) },
            )
        }

        VerticalDivider(modifier = Modifier.height(24.dp))

        FeedbackStatus.forType(state.selectedType).forEach { status ->
            val selected = state.statusFilter == status
            FilterChip(
                selected = selected,
                // Tapping the active status again clears the filter
                onClick = { onAction(FeedbackAction.OnStatusFilterSelected(if (selected) null else status)) },
                label = { Text(stringResource(status.labelRes())) },
            )
        }

        VerticalDivider(modifier = Modifier.height(24.dp))

        TagFilterChip(
            selected = state.tagFilter,
            onSelect = { onAction(FeedbackAction.OnTagFilterSelected(it)) },
        )

        FilterChip(
            selected = state.onlyMine,
            onClick = { onAction(FeedbackAction.OnOnlyMineToggle) },
            label = { Text(stringResource(Res.string.feedback_filter_mine)) },
        )
    }
}

@Composable
private fun TagFilterChip(
    selected: FeedbackTag?,
    onSelect: (FeedbackTag?) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = selected != null,
            onClick = { menuOpen = true },
            label = {
                Text(selected?.let { stringResource(it.labelRes()) } ?: stringResource(Res.string.feedback_filter_tag))
            },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.feedback_filter_all_tags)) },
                onClick = {
                    menuOpen = false
                    onSelect(null)
                }
            )
            FeedbackTag.entries.forEach { tag ->
                DropdownMenuItem(
                    text = { Text(stringResource(tag.labelRes())) },
                    onClick = {
                        menuOpen = false
                        onSelect(tag)
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
            descriptionPreview = "Share your location with chosen friends and see them on the map.",
            status = FeedbackStatus.IMPLEMENTED,
            myVote = FeedbackVote.DIDNT_KNOW,
            upvotes = 30,
            downvotes = 1,
            didntKnowCount = 7,
            latestDevComment = null,
            isSeeded = true,
        ),
    )
    SchneaggchatTheme {
        FeedbackScreen(
            state = FeedbackState(
                features = entries,
                visibleEntries = entries,
                isLoading = false,
            ),
            onAction = {}
        )
    }
}
