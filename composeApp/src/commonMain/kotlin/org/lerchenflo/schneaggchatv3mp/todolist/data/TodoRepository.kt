package org.lerchenflo.schneaggchatv3mp.todolist.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.todolist.domain.TodoEntry

class TodoRepository(
    private val database: AppDatabase
) {

    fun getTodoItemsFlow() : Flow<List<TodoEntry>>{
        return database.todolistdao().getAllTodos().map { entityList ->
            entityList.map { entity ->
                TodoEntry(
                    id = entity.id,
                    senderId = entity.senderId,
                    platform = entity.platform,
                    type = entity.type,
                    editorId = entity.editorId,
                    content = entity.content,
                    title = entity.title,
                    lastChanged = entity.lastChanged,
                    status = entity.status,
                    priority = entity.priority,
                )
            }
        }
    }

}