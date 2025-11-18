package org.lerchenflo.schneaggchatv3mp.todolist.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugPlatform
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugPriority
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugStatus
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugType
import org.lerchenflo.schneaggchatv3mp.todolist.domain.TodoEntry

@Serializable
@Entity
data class TodoEntityDto(
    @PrimaryKey(autoGenerate = false)
    @SerialName("id")
    var id: String,

    @SerialName("senderId")
    var senderId: String = "",

    @SerialName("platform")
    var platform: Int = BugPlatform.Multiplatform.value,

    @SerialName("type")
    var type: Int = BugType.BugReport.value,

    @SerialName("editorId")
    var editorId: Int = 0,

    @SerialName("content")
    var content: String = "",

    @SerialName("title")
    var title: String = "",

    @SerialName("lastChanged")
    @ColumnInfo(name = "changedate")
    var lastChanged: String = "",

    @SerialName("createDate")
    var createDate: String = "",

    @SerialName("senderAsString")
    var senderAsString: String = "Unknown",

    @SerialName("status")
    var status: Int = BugStatus.Unfinished.value,

    @SerialName("priority")
    var priority: Int = BugPriority.Normal.value
)

fun TodoEntityDto.toTodoEntry(): TodoEntry {
    return TodoEntry(
        id = this.id,
        senderId = this.senderId,
        createDate = this.createDate,
        platform = this.platform,
        type = this.type,
        editorId = this.editorId,
        content = this.content,
        title = this.title,
        lastChanged = this.lastChanged,
        senderAsString = this.senderAsString,
        status = this.status,
        priority = this.priority
    )
}

fun TodoEntry.toTodoEntityDto(): TodoEntityDto {
    return TodoEntityDto(
        id = this.id,
        senderId = this.senderId,
        createDate = this.createDate.ifBlank { "0" },
        platform = this.platform,
        type = this.type,
        editorId = this.editorId,
        content = this.content,
        title = this.title,
        lastChanged = this.lastChanged.ifBlank { "0" },
        senderAsString = this.senderAsString,
        status = this.status,
        priority = this.priority
    )
}
