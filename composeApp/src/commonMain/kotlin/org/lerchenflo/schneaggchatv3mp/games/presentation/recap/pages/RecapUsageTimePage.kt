package org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.RecapPageThemes
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.recap_usage_avg_session
import schneaggchatv3mp.composeapp.generated.resources.recap_usage_busiest_day
import schneaggchatv3mp.composeapp.generated.resources.recap_usage_busiest_hour
import schneaggchatv3mp.composeapp.generated.resources.recap_usage_duration_hm
import schneaggchatv3mp.composeapp.generated.resources.recap_usage_duration_m
import schneaggchatv3mp.composeapp.generated.resources.recap_usage_hours_label
import schneaggchatv3mp.composeapp.generated.resources.recap_usage_longest_session
import schneaggchatv3mp.composeapp.generated.resources.recap_usage_minutes_label
import schneaggchatv3mp.composeapp.generated.resources.recap_usage_peak_month
import schneaggchatv3mp.composeapp.generated.resources.recap_usage_sessions
import schneaggchatv3mp.composeapp.generated.resources.recap_usage_title
import schneaggchatv3mp.composeapp.generated.resources.recap_usage_tracking_note

private const val MILLIS_PER_MINUTE = 60_000L
private const val MILLIS_PER_HOUR = 3_600_000L

/** Online time (WebSocket connection time) of the recap year. */
@Composable
fun RecapUsageTimePage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.UsageTime
    // Below one hour the hero number switches to minutes, so a short year never reads "0".
    val showHours = recap.usageMillisThisYear >= MILLIS_PER_HOUR
    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_usage_title), color = theme.accent)
        }
        RecapBigNumber(
            target = if (showHours) recap.usageMillisThisYear / MILLIS_PER_HOUR else recap.usageMillisThisYear / MILLIS_PER_MINUTE,
            running = visible,
            color = theme.onBackground
        )
        RevealItem(visible, 1) {
            RecapBody(
                text = stringResource(if (showHours) Res.string.recap_usage_hours_label else Res.string.recap_usage_minutes_label),
                color = theme.onBackground.copy(alpha = 0.8f),
                fontSize = 20.sp
            )
        }
        Spacer(Modifier.height(24.dp))
        MonthBarChart(
            months = recap.usagePerMonth,
            peakMonth = recap.usagePeakMonth?.month,
            visible = visible,
            barColor = theme.onBackground.copy(alpha = 0.35f),
            peakColor = theme.accent,
            labelColor = theme.onBackground.copy(alpha = 0.6f),
            chartHeight = 110.dp
        )
        recap.usagePeakMonth?.let { peak ->
            Spacer(Modifier.height(12.dp))
            RevealItem(visible, 2) {
                RecapBody(
                    text = stringResource(Res.string.recap_usage_peak_month, peak.monthName.asString()),
                    color = theme.secondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        RevealItem(visible, 3) {
            PillFlow(
                pills = listOf(
                    stringResource(Res.string.recap_usage_sessions, formatCount(recap.usageSessionCount)),
                    stringResource(Res.string.recap_usage_avg_session, formatDuration(recap.usageAverageSessionMillis)),
                    stringResource(Res.string.recap_usage_longest_session, formatDuration(recap.usageLongestSessionMillis)),
                ),
                theme = theme
            )
        }
        Spacer(Modifier.height(16.dp))
        RevealItem(visible, 4) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                recap.usageBusiestDayFormatted?.let { day ->
                    RecapBody(
                        text = stringResource(Res.string.recap_usage_busiest_day, day, formatDuration(recap.usageBusiestDayMillis)),
                        color = theme.onBackground.copy(alpha = 0.75f),
                        fontSize = 15.sp
                    )
                }
                recap.usageBusiestHourOfDay?.let { hour ->
                    RecapBody(
                        text = stringResource(Res.string.recap_usage_busiest_hour, hour),
                        color = theme.onBackground.copy(alpha = 0.75f),
                        fontSize = 15.sp
                    )
                }
            }
        }
        val sinceMonth = recap.usageTrackingSinceMonth
        val sinceYear = recap.usageTrackingSinceYear
        if (sinceMonth != null && sinceYear != null) {
            Spacer(Modifier.height(16.dp))
            RevealItem(visible, 5) {
                RecapBody(
                    text = stringResource(Res.string.recap_usage_tracking_note, sinceMonth.asString(), sinceYear),
                    color = theme.onBackground.copy(alpha = 0.55f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun formatDuration(millis: Long): String {
    val totalMinutes = millis / MILLIS_PER_MINUTE
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        stringResource(Res.string.recap_usage_duration_hm, hours.toInt(), minutes.toInt())
    } else {
        stringResource(Res.string.recap_usage_duration_m, minutes.toInt())
    }
}
