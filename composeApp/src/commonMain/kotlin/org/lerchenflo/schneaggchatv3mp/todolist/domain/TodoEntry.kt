package org.lerchenflo.schneaggchatv3mp.todolist.domain

import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.bugpriority_extreme
import schneaggchatv3mp.composeapp.generated.resources.bugpriority_high
import schneaggchatv3mp.composeapp.generated.resources.bugpriority_low
import schneaggchatv3mp.composeapp.generated.resources.bugpriority_normal
import schneaggchatv3mp.composeapp.generated.resources.bugstatus_finished
import schneaggchatv3mp.composeapp.generated.resources.bugstatus_in_progress
import schneaggchatv3mp.composeapp.generated.resources.bugstatus_unfinished
import schneaggchatv3mp.composeapp.generated.resources.bugtype

data class TodoEntry(
    var id: String,
    var senderId: String,
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
                ?: Multiplatform

        fun fromIntOrNull(value: Int): BugPlatform? =
            entries.firstOrNull { it.value == value }
    }
}

enum class BugType(val value: Int) {
    BugReport(0),
    FeatureRequest(1),

    Todo(2);

    override fun toString(): String = when (this) {
        BugReport -> "Bug Report"
        FeatureRequest -> "Feature Request"
        Todo -> "Todo"
    }

    companion object {
        fun fromInt(value: Int): BugType =
            entries.firstOrNull { it.value == value }
                ?: BugReport

        fun fromIntOrNull(value: Int): BugType? =
            entries.firstOrNull { it.value == value }
    }
}

enum class BugStatus(val value: Int) {
    Unfinished(0),
    InProgress(1),
    Finished(2);

    fun toUiText(): UiText = when (this) {
        Unfinished -> UiText.StringResourceText(Res.string.bugstatus_unfinished)
        InProgress -> UiText.StringResourceText(Res.string.bugstatus_in_progress)
        Finished   -> UiText.StringResourceText(Res.string.bugstatus_finished)
    }

    companion object {
        fun fromInt(value: Int): BugStatus =
            entries.firstOrNull { it.value == value } ?: Unfinished

        fun fromIntOrNull(value: Int): BugStatus? =
            entries.firstOrNull { it.value == value }
    }
}


enum class BugPriority(val value: Int) {
    Low(0),
    Normal(1),
    High(2),
    Extreme(3);

    fun toUiText(): UiText = when (this) {
        Low -> UiText.StringResourceText(Res.string.bugpriority_low)
        Normal -> UiText.StringResourceText(Res.string.bugpriority_normal)
        High -> UiText.StringResourceText(Res.string.bugpriority_high)
        Extreme -> UiText.StringResourceText(Res.string.bugpriority_extreme)
    }

    companion object {
        fun fromInt(value: Int): BugPriority =
            entries.firstOrNull { it.value == value }
                ?: BugPriority.Normal

        fun fromIntOrNull(value: Int): BugPriority? =
            entries.firstOrNull { it.value == value }
    }
}


enum class BugEditor(val value: Int) {
    Notassigned(0),
    Flo(1),
    Fabi(2);

    override fun toString(): String = when (this) {
        Notassigned -> "-"
        Flo -> "Flo"
        Fabi -> "Fabi"
    }

    companion object {
        fun fromInt(value: Int): BugEditor =
            entries.firstOrNull { it.value == value }
                ?: BugEditor.Notassigned

        fun fromIntOrNull(value: Int): BugEditor? =
            entries.firstOrNull { it.value == value }


    }
}

