package org.lerchenflo.schneaggchatv3mp.events.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.onboarding.tapTarget
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.presentation.uielements.EventEditPopup
import org.lerchenflo.schneaggchatv3mp.events.presentation.uielements.EventItem
import org.lerchenflo.schneaggchatv3mp.events.presentation.uielements.EventJoinPopup
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.events_empty
import schneaggchatv3mp.composeapp.generated.resources.events_screen_title

@Composable
fun EventsRoot(
    initialEntryId: String?,
    initialEntry: Event?
) {
    val viewModel: EventsViewModel = koinViewModel<EventsViewModel> { parametersOf(initialEntryId, initialEntry) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    EventsScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    state: EventsState,
    onAction: (EventsAction) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ActivityTitle(
                title = stringResource(Res.string.events_screen_title),
                showBackButton = false
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.tapTarget("events_create_button"),
                onClick = {
                    onAction(EventsAction.OnCreateNewEventButtonClick)
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
            }
        }
    ) { innerPadding ->
        SessionCache.authStateValue // reactive read: recompose once autologin finishes instead of staying stale
        val ownId = SessionCache.requireLoggedIn()?.userId

        if (state.events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.events_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = state.events,
                    key = { it.id }
                ) { event ->
                    val creatorFriend = state.friendsById[event.creatorId]
                    EventItem(
                        event = event,
                        creatorProfilePictureUrl = creatorFriend?.profilePictureUrl,
                        isOwnEvent = event.creatorId == ownId,
                        onClick = { onAction(EventsAction.OnEventClick(event.id)) }
                    )
                }
            }
        }

        // ownId is null until autologin finishes. Rendering then would pick the popup by comparing
        // against null and open the read-only join sheet for the user's own event, so wait for the
        // id instead of guessing - the reactive read above recomposes us once it arrives.
        state.selectedEvent?.takeIf { ownId != null }?.let { selectedEvent ->
            if (selectedEvent.creatorId == ownId) {
                EventEditPopup(
                    event = selectedEvent,
                    onSave = { event, typeIcon -> onAction(EventsAction.OnSaveEvent(event, typeIcon)) },
                    onDismiss = { onAction(EventsAction.OnEventPopupDismiss) },
                    friendsById = state.friendsById,
                    groups = state.groups,
                    onPickLocation = { onAction(EventsAction.OnPickLocationClick(it)) },
                    onDelete = { deleteGroup -> onAction(EventsAction.OnDeleteEvent(event = selectedEvent, deleteGroup = deleteGroup)) },
                    isMobile = state.isMobile,
                )
            } else {
                EventJoinPopup(
                    event = selectedEvent,
                    onDismiss = { onAction(EventsAction.OnEventPopupDismiss) },
                    onJoin = { onAction(EventsAction.OnJoinEvent(it)) },
                    isJoining = state.isJoiningEvent,
                    friendsById = state.friendsById
                )
            }
        }
    }
}

