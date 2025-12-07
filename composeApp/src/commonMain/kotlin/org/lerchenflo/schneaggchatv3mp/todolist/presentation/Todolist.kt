package org.lerchenflo.schneaggchatv3mp.todolist.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.sharedUi.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.ChipSelection
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugStatus
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.add
import schneaggchatv3mp.composeapp.generated.resources.todolist


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodolistScreen(
    modifier: Modifier = Modifier
        .fillMaxSize()
){

    val viewModel = koinViewModel<TodolistViewModel>()
    val todoliste by viewModel.todoflowState.collectAsStateWithLifecycle(emptyList())

    val popupVisible by viewModel.popupVisible
    val selectedTodo by viewModel.selectedTodo

    //Zoagts grad a popup
    if (popupVisible ) {

        //Wenn a tod usgwählt isch, denn a details zoaga
        if (selectedTodo != null){
            ShowTodoDetails(
                todoEntry = selectedTodo!!,
                onDismiss = { viewModel.hidePopup() },
                onSave = { newtodoitem ->
                    viewModel.changeItem(newtodoitem, selectedTodo!!)
                    viewModel.hidePopup()
                },
                onDelete = {
                    viewModel.deleteItem(selectedTodo!!.id)
                    viewModel.hidePopup()
                },
                editable = SessionCache.developer
            )
        }else{
            ShowAddPopup(
                onDismiss = {
                    viewModel.hidePopup()
                },
                onSubmit = {
                    viewModel.addItem(it)
                    viewModel.hidePopup()
                }
            )
        }


    }


    Box(
        modifier = modifier
    ){
        Column{
            ActivityTitle(
                title = stringResource(Res.string.todolist),
                onBackClick ={
                    viewModel.onBackClick()
                }
            )

            //Sortieren
            ChipSelection(BugSorttype.entries.toList().map { it.toUiText().asString() }) {
                viewModel.sortType.value = BugSorttype.entries[it]
            }


            PullToRefreshBox( // needs experimental opt in
                isRefreshing = false,
                onRefresh = { viewModel.refresh() }, // Trigger refresh
                indicator = {
                    null //Überschrieba dassa garned kut
                }
            ) {

                //Do vlt no sortiera
                //TODO: Sortiera iboua
                /*
                ChipSelection(
                    listOf("Alle", "Für mich", "Wichtig", "Unwichtig", "Lastchanged"),
                    {}
                )

                 */


                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    itemsIndexed(todoliste) { index, todo ->
                        TodoEntryUI(
                            todoEntry = todo,
                            onClick = { viewModel.showPopup(todo) },
                            profilepicfilepath = viewModel.getProfilePicfileNameFromId(todo.editorId)
                        )

                        // Add space if next item has a lower priority
                        val isNotLast = index < todoliste.lastIndex
                        if (isNotLast) {
                            val next = todoliste[index + 1]
                            val currentPriority = todo.priority
                            val nextPriority = next.priority

                            val currentFinished = todo.status == BugStatus.Finished.value
                            val nextFinished = next.status == BugStatus.Finished.value

                            //Wenn beide fertig sind denn kuan spacer
                            if (currentFinished && nextFinished){

                            }else{ //Sunsch scho wenn sich was ändert
                                if (nextPriority < currentPriority || currentFinished != nextFinished) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }

                        }
                    }
                }
            }

        }

        FloatingActionButton(
            onClick = {
                viewModel.showPopup(null)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)

            ){
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(Res.string.add),
            )
        }
    }
}
