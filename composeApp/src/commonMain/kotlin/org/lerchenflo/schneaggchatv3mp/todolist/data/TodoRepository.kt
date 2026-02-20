package org.lerchenflo.schneaggchatv3mp.todolist.data

import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.todolist.domain.TodoEntry

class TodoRepository(
    private val database: AppDatabase,
    private val networkUtils: NetworkUtils
) {

    fun getTodoItemsFlow() : Flow<List<TodoEntry>>{
        return database.todoListDao().getAllTodos().map { entityList ->
            entityList.map { entity ->
                entity.toTodoEntry()
            }
        }
    }

    @Transaction
    suspend fun gettodochangeid(): List<IdChangeDate>{
        return database.todoListDao().getTodoIdsWithChangeDates()
    }

    suspend fun deleteTodo(id: String){
        database.todoListDao().delete(id)
    }

    @Transaction
    suspend fun upsertTodo(todo: TodoEntry) {
        if (database.todoListDao().getTodoById(todo.id) != null) {
            database.todoListDao().updateTodo(todo.toTodoEntityDto())
        } else {
            database.todoListDao().insertTodo(todo.toTodoEntityDto())
        }
    }

    suspend fun upsertTodoServer(todo: TodoEntry, timestamp: String){
        todo.lastChanged = timestamp
        /*
        val networkrequest = networkUtils.upsertTodo(todo, timestamp)
        networkrequest.onSuccessWithBody { bool, body ->
            if (bool){
                todo.id = body.toLong()
                upsertTodo(todo)
            }
        }
         */

    }

    suspend fun deleteTodoServer(todoID: String){
        /*
        networkUtils.deleteTodo(todoID)

            database.todolistdao().delete(todoID)

         */


    }
}