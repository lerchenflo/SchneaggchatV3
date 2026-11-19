package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.sharedtext

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.reportLastingErrorTo
import org.lerchenflo.schneaggchatv3mp.app.launchOnMain
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityRepository
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionItemData.SharedText
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtension
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionRepository
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.sharedtext.AuthoredText
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.sharedtext.MAX_SHARED_TEXT_LENGTH
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.sharedtext.mergeTextEdits
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.sharedtext.textChange
import org.lerchenflo.schneaggchatv3mp.sharedUi.text.userIdentityNameText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.you_with_brackets

/** Wait after the last keystroke before the text is saved. */
private const val SAVE_DELAY_MS = 1_000L

/**
 * Backs the shared text editor: the group writes one document together, each part highlighted in
 * its writer's name color. Edits are saved shortly after the last keystroke; the server keeps the
 * whole text (last save wins) and works out who wrote what. Changes of others that arrive while
 * the user types are merged into the editor ([mergeTextEdits]), so the own unsaved edits stay.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SharedTextViewModel(
    private val errorChannel: ErrorChannel,
    private val groupExtensionRepository: GroupExtensionRepository,
    private val groupRepository: GroupRepository,
    private val userIdentityRepository: UserIdentityRepository,
    private val applicationScope: ApplicationScope,
    private val navigator: Navigator,

    private val extensionId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(SharedTextState())
    val state = _state.asStateFlow()

    private val ownId = SessionCache.requireLoggedIn()?.userId.orEmpty()

    /** The server version the own edits are based on; set to the saved text as soon as a save starts. */
    private var base = AuthoredText.EMPTY

    /** What the editor shows: [base] plus the own unsaved edits. */
    private var local = AuthoredText.EMPTY

    /** The item holding the text, null until the first save created it. */
    private var itemId: String? = null

    private var pendingSave: Job? = null
    private val saveMutex = Mutex()

    /** Everyone who wrote a part of [local], for the author list. */
    private val writerIds = MutableStateFlow<Set<String>>(emptySet())

    init {
        observeExtension()
        observeAuthors()
    }

    fun onAction(action: SharedTextAction) {
        when (action) {
            SharedTextAction.OnBackClick -> viewModelScope.launch {
                navigator.navigateBack()
            }

            is SharedTextAction.OnTextChange -> onTextChange(action.value)

            SharedTextAction.OnToggleAuthorColors ->
                _state.update { it.copy(showAuthorColors = !it.showAuthorColors) }
        }
    }

    override fun onCleared() {
        // Leaving the screen must not drop the last keystrokes still waiting for the save delay
        if (local.text != base.text) save()
    }

    // ─── Editing ──────────────────────────────────────────────────────────────

    private fun onTextChange(value: TextFieldValue) {
        // Cursor or keyboard composition moved, the text is the same
        if (value.text == local.text) {
            _state.update { it.copy(text = value) }
            return
        }
        if (value.text.length > MAX_SHARED_TEXT_LENGTH) return

        local = local.edit(value.text, ownId)
        writerIds.value = local.writerIds
        _state.update { it.copy(text = value, writerRuns = local.runs) }
        scheduleSave()
    }

    private fun scheduleSave() {
        pendingSave?.cancel()
        pendingSave = viewModelScope.launch {
            delay(SAVE_DELAY_MS)
            save()
        }
    }

    /** Sends the editor text. Runs in the app scope, so a save also finishes after the screen is left. */
    private fun save(): Job = applicationScope.launchOnMain {
        saveMutex.withLock {
            if (local.text != base.text) sendLocalText()
        }
    }

    /** Sends [local] to the server; only called under [saveMutex], so saves never overlap. */
    private suspend fun sendLocalText() {
        val saving = local
        val previousBase = base
        // The server holds this text once the save is through, so its echo is no foreign change
        base = saving

        when (val result = groupExtensionRepository.upsertItem(extensionId, itemId, SharedText(saving.text))) {
            is DataResult.Success -> {
                // Picks up the new itemId before the next save starts
                groupExtensionRepository.getExtensionFlow(extensionId).first()?.let(::applyServerVersion)
            }
            is DataResult.Error -> {
                if (base === saving) base = previousBase
                // Someone else created the text item at the same time: load theirs, the merge saves again
                if (result.error is DataError.Conflict) {
                    groupExtensionRepository.syncGroupExtensions()
                } else {
                    result.reportLastingErrorTo(errorChannel)
                }
            }
        }
    }

    // ─── Server state ─────────────────────────────────────────────────────────

    private fun observeExtension() {
        viewModelScope.launch {
            groupExtensionRepository.getExtensionFlow(extensionId).collect { extension ->
                if (extension == null) {
                    _state.update { it.copy(isLoading = false, extensionMissing = true) }
                } else {
                    _state.update { it.copy(title = extension.title, isLoading = false, extensionMissing = false) }
                    applyServerVersion(extension)
                }
            }
        }
    }

    /** Takes over the server's text, keeping the own unsaved edits on top of it. */
    private fun applyServerVersion(extension: GroupExtension) {
        val item = extension.items.firstOrNull { it.data is SharedText }
        val sharedText = item?.data as? SharedText
        val remote = sharedText?.let { AuthoredText.fromServer(it.text, it.writers) } ?: AuthoredText.EMPTY
        if (item != null) itemId = item.id
        if (remote == base) return

        val merged = mergeTextEdits(base = base, local = local, remote = remote)
        base = remote
        showText(merged)
        if (merged.text != remote.text) scheduleSave()
    }

    /** Puts [newLocal] into the editor; the cursor stays at the same place of the text. */
    private fun showText(newLocal: AuthoredText) {
        val change = textChange(local.text, newLocal.text)
        local = newLocal
        writerIds.value = newLocal.writerIds
        _state.update { state ->
            val selection = state.text.selection
            val newSelection = if (change == null) selection else TextRange(
                change.mapOffset(selection.start),
                change.mapOffset(selection.end),
            )
            state.copy(
                text = TextFieldValue(text = newLocal.text, selection = newSelection),
                writerRuns = newLocal.runs,
            )
        }
    }

    // ─── Authors ──────────────────────────────────────────────────────────────

    private fun observeAuthors() {
        viewModelScope.launch {
            val nameText = userIdentityNameText()
            val ownSuffix = getString(Res.string.you_with_brackets)

            combine(
                memberColorsFlow(),
                writerIds.flatMapLatest { ids ->
                    userIdentityRepository.observeUsers(ids).map { users -> ids to users }
                },
            ) { colors, (ids, users) ->
                ids.map { userId ->
                    val isMe = userId == ownId
                    val name = nameText(users[userId]?.name)
                    SharedTextAuthorUi(
                        userId = userId,
                        name = if (isMe) name + ownSuffix else name,
                        color = colors[userId] ?: 0,
                        isMe = isMe,
                    )
                }.sortedWith(compareByDescending<SharedTextAuthorUi> { it.isMe }.thenBy { it.name.lowercase() })
            }.collect { authors ->
                _state.update { it.copy(authors = authors) }
            }
        }
    }

    /** Name color per userId of the group owning the extension; former members have none. */
    private fun memberColorsFlow(): Flow<Map<String, Int>> =
        groupExtensionRepository.getExtensionFlow(extensionId)
            .map { it?.ownerId }
            .distinctUntilChanged()
            .flatMapLatest { groupId ->
                if (groupId == null) {
                    flowOf(emptyMap())
                } else {
                    groupRepository.getGroupByIdFlow(groupId).map { group ->
                        group?.members.orEmpty().associate { it.userId to it.color }
                    }
                }
            }
}
