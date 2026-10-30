package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation

import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtension
import org.lerchenflo.schneaggchatv3mp.sharedUi.popups.ErrorMessage

data class GroupExtensionState(
    // Null once loaded means the extension was deleted (or never synced)
    val extension: GroupExtension? = null,
    val isLoading: Boolean = true,
    // Creator of the extension or a group admin; renaming is open to every member
    val canDelete: Boolean = false,
    val dialog: GroupExtensionDialog? = null,
    val renameError: ErrorMessage? = null,
)

enum class GroupExtensionDialog { RENAME, DELETE }

sealed interface GroupExtensionAction {
    data object OnBackClick : GroupExtensionAction
    data object OnRenameClick : GroupExtensionAction
    data class OnRenameConfirm(val title: String) : GroupExtensionAction
    data object OnDeleteClick : GroupExtensionAction
    data object OnDeleteConfirm : GroupExtensionAction
    data object OnDialogDismiss : GroupExtensionAction
}
