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

@Serializable
@Entity
data class TodoEntityDto(
    @PrimaryKey(autoGenerate = false)
    @SerialName("id")
    var id: Int = 0,

    @SerialName("senderId")
    var senderId: Int = 0,

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
