package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation

import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtension

data class GroupExtensionState(
    // Null once loaded means the extension was deleted (or never synced)
    val extension: GroupExtension? = null,
    val isLoading: Boolean = true,
)

sealed interface GroupExtensionAction {
    data object OnBackClick : GroupExtensionAction
}
