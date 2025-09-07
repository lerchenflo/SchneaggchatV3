package org.lerchenflo.schneaggchatv3mp.todolist.domain

import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugEditor.Fabi
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugEditor.Flo
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.not_assigned

data class TodoEntry(
    var id: Int = 0,
    var senderId: Int = 0,
    var createDate: String = "",
    var platform: Int = BugPlatform.Multiplatform.value,
    var type: Int = BugType.BugReport.value,
    var editorId: Int = 0,
    var content: String = "",
    var title: String = "",
    var lastChanged: String = "",
    var senderAsString: String = "",
    var status: Int = BugStatus.Unfinished.value,
    var priority: Int = BugPriority.Normal.value
)


enum class BugPlatform(val value: Int) {
    Multiplatform(0),
    Server(1),
    Android(2),
    IOS(3),
    Desktop(4);

    companion object {
        fun fromInt(value: Int): BugPlatform =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown BugPlatform value: $value")

        fun fromIntOrNull(value: Int): BugPlatform? =
            entries.firstOrNull { it.value == value }
    }
}

enum class BugType(val value: Int) {
    BugReport(0),
    FeatureRequest(1);

    override fun toString(): String = when (this) {
        BugReport -> "Bug Report"
        FeatureRequest -> "Feature Request"
    }

    companion object {
        fun fromInt(value: Int): BugType =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown BugType value: $value")

        fun fromIntOrNull(value: Int): BugType? =
            entries.firstOrNull { it.value == value }
    }
}

enum class BugStatus(val value: Int) {
    Unfinished(0),
    InProgress(1),
    Finished(2);

    companion object {
        fun fromInt(value: Int): BugStatus =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown BugStatus value: $value")

        fun fromIntOrNull(value: Int): BugStatus? =
            entries.firstOrNull { it.value == value }
    }
}

enum class BugPriority(val value: Int) {
    Low(0),
    Normal(1),
    High(2),
    Extreme(3);

    companion object {
        fun fromInt(value: Int): BugPriority =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown BugPriority value: $value")

        fun fromIntOrNull(value: Int): BugPriority? =
            entries.firstOrNull { it.value == value }
    }
}


enum class BugEditor(val value: Int) {
    not_assigned(0),
    Flo(1),
    Fabi(2);

    override fun toString(): String = when (this) {
        not_assigned -> "-"
        Flo -> "Flo"
        Fabi -> "Fabi"
    }

    companion object {
        fun fromInt(value: Int): BugEditor =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown BugPriority value: $value")

        fun fromIntOrNull(value: Int): BugEditor? =
            entries.firstOrNull { it.value == value }


    }
}

