package org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.domain.DowntimeReason
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.RecapPageTheme
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.RecapPageThemes
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.recap_downtime_count
import schneaggchatv3mp.composeapp.generated.resources.recap_downtime_subtitle
import schneaggchatv3mp.composeapp.generated.resources.recap_downtime_title

/** Hardcoded reasons the server was offline this year, most chaotic first, with how often each happened. */
@Composable
fun RecapDowntimePage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.Downtime
    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(
                text = stringResource(Res.string.recap_downtime_title, recap.year),
                color = theme.accent,
                maxFontSize = 32.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        RevealItem(visible, 1) {
            RecapBody(
                text = stringResource(Res.string.recap_downtime_subtitle),
                color = theme.onBackground.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
        }
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DowntimeReason.entries.forEachIndexed { index, reason ->
                RevealItem(visible, 2 + index) {
                    DowntimeRow(chaosRank = index + 1, reason = reason, theme = theme)
                }
            }
        }
    }
}

@Composable
private fun DowntimeRow(
    chaosRank: Int,
    reason: DowntimeReason,
    theme: RecapPageTheme,
) {
    // The most chaotic reason is accent-tinted, like the requester's own row in a ranking
    val isTop = chaosRank == 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isTop) theme.accent.copy(alpha = 0.26f)
                else theme.onBackground.copy(alpha = 0.09f)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isTop) theme.accent else theme.onBackground.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = chaosRank.toString(),
                color = if (isTop) theme.backgroundBottom else theme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(reason.title),
                color = if (isTop) theme.accent else theme.onBackground,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(reason.detail),
                color = theme.onBackground.copy(alpha = 0.65f),
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = reason.occurrences?.let { stringResource(Res.string.recap_downtime_count, it) } ?: "∞",
            color = theme.secondary,
            fontSize = if (reason.occurrences == null) 26.sp else 18.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}
