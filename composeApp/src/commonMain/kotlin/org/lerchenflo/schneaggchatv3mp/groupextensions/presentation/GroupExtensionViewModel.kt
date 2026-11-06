package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.isSuccess
import org.lerchenflo.schneaggchatv3mp.app.domain.reportErrorTo
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupRepository
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionRepository
import org.lerchenflo.schneaggchatv3mp.sharedUi.popups.ErrorMessage

/**
 * Backs the screen of one extension: loads the container plus whether the user may delete it, and
 * handles the rename / delete menu. The type specific screens hang below it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GroupExtensionViewModel(
    private val errorChannel: ErrorChannel,
    private val groupExtensionRepository: GroupExtensionRepository,
    private val groupRepository: GroupRepository,
    private val navigator: Navigator,

    private val extensionId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(GroupExtensionState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            groupExtensionRepository.getExtensionFlow(extensionId)
                .flatMapLatest { extension ->
                    if (extension == null) return@flatMapLatest flowOf(null to false)
                    groupRepository.getGroupByIdFlow(extension.ownerId).map { group ->
                        val ownId = SessionCache.requireLoggedIn()?.userId
                        val canDelete = ownId != null && (
                            extension.createdBy == ownId ||
                                group?.members.orEmpty().any { member -> member.userId == ownId && member.admin }
                            )
                        extension to canDelete
                    }
                }
                .collect { (extension, canDelete) ->
                    _state.update { it.copy(extension = extension, canDelete = canDelete, isLoading = false) }
                }
        }
    }

    fun onAction(action: GroupExtensionAction) {
        when (action) {
            GroupExtensionAction.OnBackClick -> viewModelScope.launch {
                navigator.navigateBack()
            }

            GroupExtensionAction.OnRenameClick ->
                _state.update { it.copy(dialog = GroupExtensionDialog.RENAME, renameError = null) }

            is GroupExtensionAction.OnRenameConfirm -> {
                val error = validateExtensionTitle(action.title)
                if (error != null) {
                    _state.update { it.copy(renameError = error) }
                    return
                }
                _state.update { it.copy(dialog = null, renameError = null) }
                if (action.title == _state.value.extension?.title) return
                viewModelScope.launch {
                    groupExtensionRepository.renameExtension(extensionId, action.title).reportErrorTo(errorChannel)
                }
            }

            GroupExtensionAction.OnDeleteClick ->
                _state.update { it.copy(dialog = GroupExtensionDialog.DELETE) }

            GroupExtensionAction.OnDeleteConfirm -> {
                _state.update { it.copy(dialog = null) }
                viewModelScope.launch {
                    if (groupExtensionRepository.deleteExtension(extensionId).reportErrorTo(errorChannel).isSuccess) {
                        navigator.navigateBack()
                    }
                }
            }

            GroupExtensionAction.OnDialogDismiss ->
                _state.update { it.copy(dialog = null, renameError = null) }
        }
    }
}

/** Highest title length, mirrors the server limit. */
const val MAX_EXTENSION_TITLE_LENGTH = 100

/** Null when [title] (already trimmed) may be stored as an extension title. */
fun validateExtensionTitle(title: String): ErrorMessage? = when {
    title.isEmpty() -> ErrorMessage.NAME_TO_SHORT
    title.length > MAX_EXTENSION_TITLE_LENGTH -> ErrorMessage.NAME_TO_LONG
    else -> null
}
