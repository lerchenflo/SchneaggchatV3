package org.lerchenflo.schneaggchatv3mp.todolist.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.ChatSelectorItem
import org.lerchenflo.schneaggchatv3mp.todolist.data.TodoRepository
import org.lerchenflo.schneaggchatv3mp.todolist.domain.TodoEntry

class TodolistViewModel(
    private val todoRepository: TodoRepository
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
                todoRepository.upsertTodoServer(newtodo)
                println("Todo update: true")
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
            todoRepository.upsertTodoServer(todoItem)
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    val todoFlow: Flow<List<TodoEntry>> = todoRepository
        .getTodoItemsFlow()
        .map {
            list -> list.sortedWith(
            compareByDescending<TodoEntry> { it.priority }
                .thenByDescending { it.lastChanged }
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