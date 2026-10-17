package org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings.announcements

import org.lerchenflo.schneaggchatv3mp.announcement.domain.Announcement
import org.lerchenflo.schneaggchatv3mp.announcement.domain.AnnouncementDraft
import org.lerchenflo.schneaggchatv3mp.announcement.domain.AnnouncementLimits
import org.lerchenflo.schneaggchatv3mp.announcement.domain.AnnouncementText
import org.lerchenflo.schneaggchatv3mp.settings.domain.LanguageSetting
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import kotlin.time.Duration.Companion.days

/** Languages of the form, in the order they are shown. German is the only required one. */
val ANNOUNCEMENT_LANGUAGES = listOf(
    LanguageSetting.GERMAN,
    LanguageSetting.ENGLISH,
    LanguageSetting.VORI,
    LanguageSetting.ITALIAN,
)

/** Expiry presets of the form, in days. */
val ANNOUNCEMENT_EXPIRY_PRESET_DAYS = listOf(1, 7, 30)

private val EMPTY_TEXT = AnnouncementText(title = "", body = "")

data class AnnouncementForm(
    /** Number of the announcement being edited, null for a new one. */
    val editingId: Long? = null,
    val texts: Map<LanguageSetting, AnnouncementText> = emptyMap(),
    /** Epoch millis */
    val expiresAt: Long,
    val previewLanguage: LanguageSetting = LanguageSetting.GERMAN,
) {
    fun textFor(language: LanguageSetting): AnnouncementText = texts[language] ?: EMPTY_TEXT

    fun isValid(nowMillis: Long): Boolean =
        textFor(LanguageSetting.GERMAN).title.isNotBlank() &&
            expiresAt > nowMillis &&
            expiresAt <= nowMillis + AnnouncementLimits.MAX_EXPIRY_DAYS.days.inWholeMilliseconds

    /** Translations without a title count as missing (the repository drops them). */
    fun toDraft(): AnnouncementDraft = AnnouncementDraft(
        german = textFor(LanguageSetting.GERMAN),
        vori = translation(LanguageSetting.VORI),
        english = translation(LanguageSetting.ENGLISH),
        italian = translation(LanguageSetting.ITALIAN),
        expiresAt = expiresAt,
    )

    /** The form as an [Announcement], so the preview applies the same fallbacks as the banner. */
    fun toPreview(): Announcement = Announcement(
        id = editingId ?: 0,
        german = textFor(LanguageSetting.GERMAN),
        vori = translation(LanguageSetting.VORI),
        english = translation(LanguageSetting.ENGLISH),
        italian = translation(LanguageSetting.ITALIAN),
        expiresAt = expiresAt,
        createdAt = 0,
        updatedAt = 0,
    )

    private fun translation(language: LanguageSetting): AnnouncementText? =
        texts[language]?.takeIf { it.title.isNotBlank() }

    companion object {
        private val DEFAULT_EXPIRY = 7.days

        fun new(nowMillis: Long): AnnouncementForm =
            AnnouncementForm(expiresAt = nowMillis + DEFAULT_EXPIRY.inWholeMilliseconds)

        fun edit(announcement: Announcement): AnnouncementForm = AnnouncementForm(
            editingId = announcement.id,
            texts = buildMap {
                put(LanguageSetting.GERMAN, announcement.german)
                announcement.english?.let { put(LanguageSetting.ENGLISH, it) }
                announcement.vori?.let { put(LanguageSetting.VORI, it) }
                announcement.italian?.let { put(LanguageSetting.ITALIAN, it) }
            },
            expiresAt = announcement.expiresAt,
        )
    }
}

/** The announcements popup in the developer settings. */
data class AnnouncementAdminState(
    val isOpen: Boolean = false,
    val announcements: List<Announcement> = emptyList(),
    val isLoading: Boolean = false,
    /** Shown instead of the list; null while the list is shown. */
    val form: AnnouncementForm? = null,
    val isSubmitting: Boolean = false,
    /** Waiting for the user to confirm its deletion. */
    val deleteCandidate: Announcement? = null,
    /** Shown inside the popup - a snackbar would be hidden behind it. */
    val error: UiText? = null,
)

sealed interface AnnouncementAdminAction {
    data object Open : AnnouncementAdminAction
    data object Close : AnnouncementAdminAction
    data object NewClick : AnnouncementAdminAction
    data class EditClick(val announcement: Announcement) : AnnouncementAdminAction
    data class TitleChange(val language: LanguageSetting, val title: String) : AnnouncementAdminAction
    data class BodyChange(val language: LanguageSetting, val body: String) : AnnouncementAdminAction
    data class ExpiryChange(val expiresAt: Long) : AnnouncementAdminAction
    data class PreviewLanguageChange(val language: LanguageSetting) : AnnouncementAdminAction
    data object Submit : AnnouncementAdminAction
    data object CancelForm : AnnouncementAdminAction
    data class DeleteClick(val announcement: Announcement) : AnnouncementAdminAction
    data object DeleteConfirm : AnnouncementAdminAction
    data object DeleteCancel : AnnouncementAdminAction
}
