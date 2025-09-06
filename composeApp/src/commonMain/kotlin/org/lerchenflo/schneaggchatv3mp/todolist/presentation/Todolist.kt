package org.lerchenflo.schneaggchatv3mp.todolist.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.ChatSelectorViewModel
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.sharedUi.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.UserButton
import org.lerchenflo.schneaggchatv3mp.todolist.domain.TodoEntry
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.todolist


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodolistScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .safeContentPadding()
){

    val viewModel = koinViewModel<TodolistViewModel>()
    val todoliste by viewModel.todoflowState.collectAsStateWithLifecycle(emptyList())


    Column(
        modifier = modifier
    ) {
        ActivityTitle(
            title = stringResource(Res.string.todolist),
            onBackClick = onBackClick
        )

        //Do vlt no sortiera

        PullToRefreshBox( // needs experimental opt in
            isRefreshing = false,
            onRefresh = { viewModel.refresh() }, // Trigger refresh
            indicator = {
                null //Überschrieba dassa garned kut
            },

            ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
            ) {
                items(
                    todoliste
                ) { todo ->
                    TodoEntryUI(
                        todoEntry = todo
                    )
                    HorizontalDivider(
                        thickness = 0.5.dp
                    )
                }
            }
        }


    }
}