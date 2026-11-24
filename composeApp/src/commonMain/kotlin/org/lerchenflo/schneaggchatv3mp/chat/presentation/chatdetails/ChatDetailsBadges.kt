package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.formatCountdown
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.rememberCountdownMillis
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.isBirthdayToday
import org.lerchenflo.schneaggchatv3mp.utilities.iso8601DateFormatter
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.birthday_label
import schneaggchatv3mp.composeapp.generated.resources.group_expired
import schneaggchatv3mp.composeapp.generated.resources.group_expires_at
import schneaggchatv3mp.composeapp.generated.resources.group_timer_label
import kotlin.time.Clock

/**
 * Fixed badge colors (user, 2026-11-24: GitHub-like colored badges with the hard-coded colors of the
 * games recap, `RecapPalette`). Each with the text color that reads best on it; every pair has at
 * least 4.5:1 text contrast (WCAG AA), and every color stands out from the app background in light
 * and dark mode by at least 1.6:1. Lemon, Gold, Lilac and Aqua of the recap failed that on the
 * light background and are left out.
 */
enum class BadgeColor(val container: Color, val content: Color) {
    GREEN(Color(0xFF1DB954), BadgeInk),
    PINK(Color(0xFFFF007F), BadgeInk),
    AMBER(Color(0xFFFFB300), BadgeInk),
    VIOLET(Color(0xFFD63CFF), BadgeInk),
    BLUE(Color(0xFF0A1ED3), Color(0xFFFFFFFF)),
    ORANGE(Color(0xFFFF9E00), BadgeInk),
    CORAL(Color(0xFFFF5C5C), BadgeInk),
    SKY(Color(0xFF35C4FF), BadgeInk),
}

private val BadgeInk = Color(0xFF121212)

/**
 * The facts of a chat as small two-part badges (label | value, like the badges of a GitHub readme)
 * in one centered row under the profile picture, wrapping onto more lines: the group's delete
 * timer and the friend's birthday. Later hobby tags of a user fit in here too. Nothing is drawn
 * while there is nothing to show.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatDetailsBadgeRow(
    groupExpiresAt: Long?,
    birthDate: String?,
    onBirthdayClick: () -> Unit,
    modifier: Modifier = Modifier,
    timerModifier: Modifier = Modifier,
) {
    if (groupExpiresAt == null && birthDate == null) return

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupExpiresAt?.let { GroupTimerBadge(expiresAt = it, modifier = timerModifier) }

        birthDate?.let { BirthdayBadge(birthDate = it, onClick = onBirthdayClick) }

    }
}

/**
 * Countdown of a group's delete timer, shown to every member; a tap tells the exact time. Admins
 * change the timer through the ⋮ of the chat details.
 */
@Composable
private fun GroupTimerBadge(
    expiresAt: Long,
    modifier: Modifier = Modifier,
) {
    val timeRemaining = rememberCountdownMillis(key = expiresAt) {
        expiresAt - Clock.System.now().toEpochMilliseconds()
    }
    val exactTime = stringResource(
        Res.string.group_expires_at,
        millisToString(expiresAt, format = "dd.MM.yyyy HH:mm")
    )

    InfoBadge(
        icon = Icons.Default.Schedule,
        label = stringResource(Res.string.group_timer_label),
        value = if (timeRemaining > 0) formatCountdown(timeRemaining) else stringResource(Res.string.group_expired),
        valueColor = BadgeColor.CORAL,
        onClick = { SnackbarManager.showMessage(exactTime) },
        modifier = modifier
    )
}

/** A friend's birthday ([birthDate] in ISO 8601), highlighted on the day itself. */
@Composable
private fun BirthdayBadge(
    birthDate: String,
    onClick: () -> Unit,
) {
    val isToday = remember(birthDate) { isBirthdayToday(birthDate) }
    val date = iso8601DateFormatter(iso8601Format = birthDate, format = "dd.MM.")

    InfoBadge(
        icon = Icons.Default.Cake,
        label = stringResource(Res.string.birthday_label),
        value = if (isToday) "$date 🎂" else date,
        valueColor = if (isToday) BadgeColor.PINK else BadgeColor.SKY,
        onClick = onClick
    )
}

/**
 * Two-part pill: icon and [label] on the neutral left part, [value] on the colored right part.
 * The pill is drawn small; Compose still gives a clickable one the minimum touch target (48 dp).
 */
@Composable
private fun InfoBadge(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: BadgeColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(valueColor.container)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = valueColor.content
            )
        }
    }
}
