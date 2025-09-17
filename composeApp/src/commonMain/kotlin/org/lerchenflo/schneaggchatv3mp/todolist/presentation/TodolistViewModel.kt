package org.lerchenflo.schneaggchatv3mp.todolist.presentation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.todolist.data.TodoRepository
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugStatus
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugType
import org.lerchenflo.schneaggchatv3mp.todolist.domain.TodoEntry
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import org.lerchenflo.schneaggchatv3mp.utilities.getCurrentTimeMillisString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.bug_sort_all
import schneaggchatv3mp.composeapp.generated.resources.bug_sort_assigned_to_me
import schneaggchatv3mp.composeapp.generated.resources.bug_sort_bug
import schneaggchatv3mp.composeapp.generated.resources.bug_sort_feature
import schneaggchatv3mp.composeapp.generated.resources.bug_sort_important
import schneaggchatv3mp.composeapp.generated.resources.bug_sort_mine
import schneaggchatv3mp.composeapp.generated.resources.bug_sort_todo
import schneaggchatv3mp.composeapp.generated.resources.bug_sort_unfinished
import kotlin.time.Clock

class TodolistViewModel(
    private val todoRepository: TodoRepository,
    private val pictureManager: PictureManager
): ViewModel() {

    val globalViewModel: GlobalViewModel = KoinPlatform.getKoin().get()


    init {
        refresh()
    }

    fun refresh() {

        globalViewModel.viewModelScope.launch {
            //TODO: TODOIDSYNC

        }
    }



    var sortType = mutableStateOf(BugSorttype.ALL)


    var popupVisible = mutableStateOf(false)
        private set

    var selectedTodo = mutableStateOf<TodoEntry?>(null)
        private set

    fun showPopup(todo: TodoEntry?) {
        selectedTodo.value = todo
        popupVisible.value = true
    }

    fun hidePopup() {
        popupVisible.value = false
        selectedTodo.value = null
    }

    fun changeItem(newtodo: TodoEntry, oldtodo: TodoEntry){

        if (newtodo != oldtodo){
            globalViewModel.viewModelScope.launch {
                CoroutineScope(Dispatchers.IO).launch {
                    todoRepository.upsertTodoServer(newtodo, getCurrentTimeMillisString())
                    println("Todo update: true")
                }

            }
        }else{
            println("Todo update: false")
        }
    }

    fun deleteItem(todoId: Long){
        globalViewModel.viewModelScope.launch {
            todoRepository.deleteTodoServer(todoId)
        }
    }

    fun addItem(todoItem: TodoEntry){
        globalViewModel.viewModelScope.launch {
            todoRepository.upsertTodoServer(todoItem, getCurrentTimeMillisString())
        }
    }


    fun getProfilePicfileNameFromId(userid: Int) : String {
        return pictureManager.getProfilePicFilePath(userid.toLong(), false)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val todoFlow: Flow<List<TodoEntry>> = todoRepository
        .getTodoItemsFlow()
        // combine repo items with the current sort type (snapshotFlow observes the Compose mutableStateOf)
        .combine(snapshotFlow { sortType.value }) { list, sort ->
            // Filter according to the selected sort type
            val filtered = when (sort) {
                BugSorttype.MINE -> {
                    list.filter { it.senderId == SessionCache.getOwnIdValue() }
                }
                BugSorttype.UNFINISHED -> {
                    // assume a boolean property 'finished' (replace with your actual name, e.g. isDone/isCompleted)
                    list.filter { it.status != BugStatus.Finished.value }
                }
                BugSorttype.IMPORTANT -> {
                    // example: either a boolean `important` or priority threshold; adjust as needed
                    list.filter { it.priority >= 2 }
                }
                BugSorttype.BUG -> {
                    list.filter { it.type == BugType.BugReport.value }
                }
                BugSorttype.FEATURE -> {
                    list.filter { it.type == BugType.FeatureRequest.value }
                }
                BugSorttype.TODO -> {
                    list.filter { it.type == BugType.Todo.value }
                }
                BugSorttype.ASSIGNED_TO_ME -> {
                    list.filter { it.editorId.toLong() == SessionCache.getOwnIdValue() }
                }
                BugSorttype.ALL -> list
            }

            // then sort (keeps your existing sort by priority, then lastChanged)
            filtered.sortedWith(
                compareBy<TodoEntry> { it.status == BugStatus.Finished.value } // unfinished (false) come first
                    .thenByDescending { it.priority }                         // then by priority (high -> low)
                    .thenByDescending { it.lastChanged }                      // optional: newest changed first
            )

        }
        .flowOn(Dispatchers.Default)

    val todoflowState: StateFlow<List<TodoEntry>> = todoFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

}


enum class BugSorttype{
    ALL,
    IMPORTANT,
    ASSIGNED_TO_ME,
    UNFINISHED,
    BUG,
    FEATURE,
    TODO,
    MINE;


    fun toUiText(): UiText = when (this) {
        ALL -> UiText.StringResourceText(Res.string.bug_sort_all)
        MINE -> UiText.StringResourceText(Res.string.bug_sort_mine)
        UNFINISHED -> UiText.StringResourceText(Res.string.bug_sort_unfinished)
        IMPORTANT -> UiText.StringResourceText(Res.string.bug_sort_important)
        BUG -> UiText.StringResourceText(Res.string.bug_sort_bug)
        FEATURE -> UiText.StringResourceText(Res.string.bug_sort_feature)
        TODO -> UiText.StringResourceText(Res.string.bug_sort_todo)
        ASSIGNED_TO_ME -> UiText.StringResourceText(Res.string.bug_sort_assigned_to_me)
    }

}