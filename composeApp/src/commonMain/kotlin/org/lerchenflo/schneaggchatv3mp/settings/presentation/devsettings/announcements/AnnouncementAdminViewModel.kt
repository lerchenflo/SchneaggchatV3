package org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings.announcements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.announcement.domain.Announcement
import org.lerchenflo.schneaggchatv3mp.announcement.domain.AnnouncementLimits
import org.lerchenflo.schneaggchatv3mp.announcement.domain.AnnouncementRepository
import org.lerchenflo.schneaggchatv3mp.announcement.domain.AnnouncementText
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.toUiText
import org.lerchenflo.schneaggchatv3mp.settings.domain.LanguageSetting
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.announcement_not_allowed
import kotlin.time.Clock

/**
 * Lists, creates, edits and deletes announcements. Shown to everyone with developer settings;
 * the server answers NotFound to non-admins, which shows as "only admins".
 */
class AnnouncementAdminViewModel(
    private val announcementRepository: AnnouncementRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AnnouncementAdminState())
    val state: StateFlow<AnnouncementAdminState> = _state.asStateFlow()

    private var loadJob: Job? = null

    fun onAction(action: AnnouncementAdminAction) {
        when (action) {
            AnnouncementAdminAction.Open -> {
                _state.value = AnnouncementAdminState(isOpen = true)
                loadAnnouncements()
            }
            AnnouncementAdminAction.Close -> {
                loadJob?.cancel()
                _state.value = AnnouncementAdminState()
            }
            AnnouncementAdminAction.NewClick ->
                _state.update { it.copy(form = AnnouncementForm.new(nowMillis()), error = null) }
            is AnnouncementAdminAction.EditClick ->
                _state.update { it.copy(form = AnnouncementForm.edit(action.announcement), error = null) }
            is AnnouncementAdminAction.TitleChange -> updateText(action.language) {
                it.copy(title = action.title.take(AnnouncementLimits.TITLE_MAX_LENGTH))
            }
            is AnnouncementAdminAction.BodyChange -> updateText(action.language) {
                it.copy(body = action.body.take(AnnouncementLimits.BODY_MAX_LENGTH))
            }
            is AnnouncementAdminAction.ExpiryChange -> updateForm { it.copy(expiresAt = action.expiresAt) }
            is AnnouncementAdminAction.PreviewLanguageChange -> updateForm { it.copy(previewLanguage = action.language) }
            AnnouncementAdminAction.Submit -> submit()
            AnnouncementAdminAction.CancelForm -> _state.update { it.copy(form = null, error = null) }
            is AnnouncementAdminAction.DeleteClick -> _state.update { it.copy(deleteCandidate = action.announcement) }
            AnnouncementAdminAction.DeleteConfirm -> delete()
            AnnouncementAdminAction.DeleteCancel -> _state.update { it.copy(deleteCandidate = null) }
        }
    }

    private fun loadAnnouncements() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = announcementRepository.getAll()) {
                is NetworkResult.Success -> _state.update { it.copy(announcements = result.data, isLoading = false) }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.error.toAdminUiText()) }
            }
        }
    }

    private fun submit() {
        val current = _state.value
        val form = current.form ?: return
        if (current.isSubmitting || !form.isValid(nowMillis())) return

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            val draft = form.toDraft()
            val result = form.editingId
                ?.let { announcementRepository.update(it, draft) }
                ?: announcementRepository.create(draft)

            when (result) {
                is NetworkResult.Success -> _state.update { state ->
                    state.copy(
                        isSubmitting = false,
                        form = null,
                        announcements = state.announcements.withSaved(result.data),
                    )
                }
                is NetworkResult.Error -> _state.update { it.copy(isSubmitting = false, error = result.error.toAdminUiText()) }
            }
        }
    }

    private fun delete() {
        val candidate = _state.value.deleteCandidate ?: return
        _state.update { it.copy(deleteCandidate = null, error = null) }

        viewModelScope.launch {
            when (val result = announcementRepository.delete(candidate.id)) {
                is NetworkResult.Success -> _state.update { state ->
                    state.copy(announcements = state.announcements.filterNot { it.id == candidate.id })
                }
                is NetworkResult.Error -> _state.update { it.copy(error = result.error.toAdminUiText()) }
            }
        }
    }

    private fun updateForm(transform: (AnnouncementForm) -> AnnouncementForm) {
        _state.update { state -> state.copy(form = state.form?.let(transform)) }
    }

    private fun updateText(language: LanguageSetting, transform: (AnnouncementText) -> AnnouncementText) {
        updateForm { form -> form.copy(texts = form.texts + (language to transform(form.textFor(language)))) }
    }

    private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
}

/** Replaces or adds [saved], newest number first like the server's list. */
private fun List<Announcement>.withSaved(saved: Announcement): List<Announcement> =
    (filterNot { it.id == saved.id } + saved).sortedByDescending { it.id }

// The server hides its admin routes behind NotFound for everyone who is not an admin
private fun NetworkingError.toAdminUiText(): UiText =
    if (this is NetworkingError.NotFound) UiText.StringResourceText(Res.string.announcement_not_allowed)
    else toUiText()
