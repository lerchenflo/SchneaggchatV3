package org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.RecapPageThemes
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.recap_count_all_time
import schneaggchatv3mp.composeapp.generated.resources.recap_quote_count
import schneaggchatv3mp.composeapp.generated.resources.recap_text_count
import schneaggchatv3mp.composeapp.generated.resources.recap_wakes_group
import schneaggchatv3mp.composeapp.generated.resources.recap_wakes_most_woken_label
import schneaggchatv3mp.composeapp.generated.resources.recap_wakes_received
import schneaggchatv3mp.composeapp.generated.resources.recap_wakes_received_label
import schneaggchatv3mp.composeapp.generated.resources.recap_wakes_sent_label
import schneaggchatv3mp.composeapp.generated.resources.recap_wakes_title
import schneaggchatv3mp.composeapp.generated.resources.recap_wakes_top_reason_label
import schneaggchatv3mp.composeapp.generated.resources.recap_wakes_top_waker_label

/**
 * Wakes sent and received this year: whom you woke most and why, who woke you most, and the wakes
 * others sent to your groups (worded as group wakes: whether your phone rang depends on your settings).
 */
@Composable
fun RecapWakesPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.Wakes
    val wakes = recap.wakes
    // Sent wakes lead; someone who only got woken sees the received ones as the big number
    val receivedIsHero = wakes.sentThisYear == 0L && wakes.receivedThisYear > 0

    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_wakes_title), color = theme.accent)
        }
        if (wakes.sentThisYear > 0 || receivedIsHero) {
            Spacer(Modifier.height(24.dp))
            RecapBigNumber(
                target = if (receivedIsHero) wakes.receivedThisYear else wakes.sentThisYear,
                running = visible,
                color = theme.onBackground
            )
            RevealItem(visible, 1) {
                RecapBody(
                    text = stringResource(if (receivedIsHero) Res.string.recap_wakes_received_label else Res.string.recap_wakes_sent_label),
                    color = theme.onBackground.copy(alpha = 0.8f),
                    fontSize = 20.sp
                )
            }
            val allTime = if (receivedIsHero) wakes.receivedAllTime else wakes.sentAllTime
            Spacer(Modifier.height(12.dp))
            RevealItem(visible, 2) {
                PillFlow(pills = listOf(stringResource(Res.string.recap_count_all_time, formatCount(allTime))), theme = theme)
            }
        }

        Column(
            modifier = Modifier.padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            wakes.mostWoken?.let { woken ->
                RevealItem(visible, 3) {
                    RecapLabeledCard(
                        label = stringResource(Res.string.recap_wakes_most_woken_label),
                        text = stringResource(Res.string.recap_text_count, woken.name, formatCount(woken.count)),
                        theme = theme
                    )
                }
            }
            wakes.topReason?.let { reason ->
                RevealItem(visible, 4) {
                    RecapLabeledCard(
                        label = stringResource(Res.string.recap_wakes_top_reason_label),
                        text = stringResource(Res.string.recap_quote_count, reason.text, formatCount(reason.count)),
                        theme = theme
                    )
                }
            }
            wakes.topWaker?.let { waker ->
                RevealItem(visible, 5) {
                    RecapLabeledCard(
                        label = stringResource(Res.string.recap_wakes_top_waker_label),
                        text = stringResource(Res.string.recap_text_count, waker.name, formatCount(waker.count)),
                        theme = theme
                    )
                }
            }
        }

        val notes = buildList {
            if (!receivedIsHero && wakes.receivedThisYear > 0) {
                add(stringResource(Res.string.recap_wakes_received, formatCount(wakes.receivedThisYear), formatCount(wakes.receivedAllTime)))
            }
            if (wakes.groupWakesThisYear > 0) {
                add(stringResource(Res.string.recap_wakes_group, formatCount(wakes.groupWakesThisYear)))
            }
        }
        if (notes.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            RevealItem(visible, 6) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    notes.forEach { note ->
                        RecapBody(text = note, color = theme.onBackground.copy(alpha = 0.8f), fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
