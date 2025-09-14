package org.lerchenflo.schneaggchatv3mp.todolist.data

import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.network.util.onError
import org.lerchenflo.schneaggchatv3mp.network.util.onSuccessWithBody
import org.lerchenflo.schneaggchatv3mp.todolist.domain.TodoEntry

class TodoRepository(
    private val database: AppDatabase,
    private val networkUtils: NetworkUtils
) {

    fun getTodoItemsFlow() : Flow<List<TodoEntry>>{
        return database.todolistdao().getAllTodos().map { entityList ->
            entityList.map { entity ->
                entity.toTodoEntry()
            }
        }
    }

    @Transaction
    suspend fun gettodochangeid(): List<IdChangeDate>{
        return database.todolistdao().getTodoIdsWithChangeDates()
    }

    suspend fun deleteTodo(id: Long){
        database.todolistdao().delete(id)
    }

    @Transaction
    suspend fun upsertTodo(todo: TodoEntry) {
        if (database.todolistdao().getTodoById(todo.id) != null) {
            database.todolistdao().updateTodo(todo.toTodoEntityDto())
        } else {
            database.todolistdao().insertTodo(todo.toTodoEntityDto())
        }
    }

    suspend fun upsertTodoServer(todo: TodoEntry, timestamp: String){
        todo.lastChanged = timestamp
        val networkrequest = networkUtils.upsertTodo(todo, timestamp)

        networkrequest.onSuccessWithBody { bool, body ->
            if (bool){
                todo.id = body.toLong()
                upsertTodo(todo)
            }
        }
    }

    suspend fun deleteTodoServer(todoID: Long){
        networkUtils.deleteTodo(todoID)

        database.todolistdao().delete(todoID)
    }
}