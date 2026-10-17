package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import org.lerchenflo.schneaggchatv3mp.announcement.domain.Announcement
import org.lerchenflo.schneaggchatv3mp.announcement.domain.AnnouncementText
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.settings.domain.LanguageSetting
import org.lerchenflo.schneaggchatv3mp.sharedUi.ChatSelectorDismissableInfo

/** An admin announcement at the top of the chat list, in the language the app is shown in. */
@Composable
fun AnnouncementBanner(
    announcement: Announcement,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = announcement.textFor(currentAppLanguage())

    ChatSelectorDismissableInfo(
        title = text.title,
        description = text.body.ifBlank { null },
        onDismiss = onDismiss,
        icon = Icons.Default.Campaign,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * The language the app's own texts use right now - the same locale the string resources are
 * picked by. A system language the app has no translation for shows the default English texts,
 * so it gets English here too.
 */
@Composable
private fun currentAppLanguage(): LanguageSetting {
    val language = LanguageSetting.fromIsoCode(Locale.current.toLanguageTag())
    return if (language == LanguageSetting.SYSTEM) LanguageSetting.ENGLISH else language
}

@Preview(showBackground = true)
@Composable
private fun AnnouncementBannerPreview() {
    SchneaggchatTheme {
        AnnouncementBanner(
            announcement = Announcement(
                id = 1,
                german = AnnouncementText(
                    title = "Wartungsarbeiten am Sonntag",
                    body = "Der Server ist am Sonntag zwischen 20 und 22 Uhr nicht erreichbar.",
                ),
                vori = null,
                english = AnnouncementText(
                    title = "Maintenance on Sunday",
                    body = "The server is offline on Sunday between 8 and 10 pm.",
                ),
                italian = null,
                expiresAt = Long.MAX_VALUE,
                createdAt = 0,
                updatedAt = 0,
            ),
            onDismiss = {},
        )
    }
}
