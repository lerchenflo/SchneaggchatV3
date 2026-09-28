package org.lerchenflo.schneaggchatv3mp.feedback.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.jetbrains.compose.resources.StringResource
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackStatus
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackTag
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackType
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.feedback_sort_new
import schneaggchatv3mp.composeapp.generated.resources.feedback_sort_top
import schneaggchatv3mp.composeapp.generated.resources.feedback_status_confirmed
import schneaggchatv3mp.composeapp.generated.resources.feedback_status_fixed
import schneaggchatv3mp.composeapp.generated.resources.feedback_status_implemented
import schneaggchatv3mp.composeapp.generated.resources.feedback_status_open
import schneaggchatv3mp.composeapp.generated.resources.feedback_status_planned
import schneaggchatv3mp.composeapp.generated.resources.feedback_status_requested
import schneaggchatv3mp.composeapp.generated.resources.feedback_tab_bugs
import schneaggchatv3mp.composeapp.generated.resources.feedback_tab_features
import schneaggchatv3mp.composeapp.generated.resources.feedback_tag_chat
import schneaggchatv3mp.composeapp.generated.resources.feedback_tag_events
import schneaggchatv3mp.composeapp.generated.resources.feedback_tag_games
import schneaggchatv3mp.composeapp.generated.resources.feedback_tag_groups
import schneaggchatv3mp.composeapp.generated.resources.feedback_tag_map
import schneaggchatv3mp.composeapp.generated.resources.feedback_tag_notifications
import schneaggchatv3mp.composeapp.generated.resources.feedback_tag_other
import schneaggchatv3mp.composeapp.generated.resources.feedback_tag_performance
import schneaggchatv3mp.composeapp.generated.resources.feedback_tag_settings
import schneaggchatv3mp.composeapp.generated.resources.feedback_tag_ui

fun FeedbackType.labelRes(): StringResource = when (this) {
    FeedbackType.FEATURE -> Res.string.feedback_tab_features
    FeedbackType.BUG -> Res.string.feedback_tab_bugs
}

fun FeedbackSort.labelRes(): StringResource = when (this) {
    FeedbackSort.TOP -> Res.string.feedback_sort_top
    FeedbackSort.NEW -> Res.string.feedback_sort_new
}

fun FeedbackStatus.labelRes(): StringResource = when (this) {
    FeedbackStatus.REQUESTED -> Res.string.feedback_status_requested
    FeedbackStatus.PLANNED -> Res.string.feedback_status_planned
    FeedbackStatus.IMPLEMENTED -> Res.string.feedback_status_implemented
    FeedbackStatus.OPEN -> Res.string.feedback_status_open
    FeedbackStatus.CONFIRMED -> Res.string.feedback_status_confirmed
    FeedbackStatus.FIXED -> Res.string.feedback_status_fixed
}

fun FeedbackTag.labelRes(): StringResource = when (this) {
    FeedbackTag.CHAT -> Res.string.feedback_tag_chat
    FeedbackTag.GROUPS -> Res.string.feedback_tag_groups
    FeedbackTag.EVENTS -> Res.string.feedback_tag_events
    FeedbackTag.MAP -> Res.string.feedback_tag_map
    FeedbackTag.GAMES -> Res.string.feedback_tag_games
    FeedbackTag.SETTINGS -> Res.string.feedback_tag_settings
    FeedbackTag.UI -> Res.string.feedback_tag_ui
    FeedbackTag.NOTIFICATIONS -> Res.string.feedback_tag_notifications
    FeedbackTag.PERFORMANCE -> Res.string.feedback_tag_performance
    FeedbackTag.OTHER -> Res.string.feedback_tag_other
}

/** Container + content color pair, both taken from the theme. */
data class FeedbackColors(val container: Color, val content: Color)

// Same rainbow palette as the TowerStack and Schneagg Rodeo games (explicitly requested for tags)
private val TAG_PALETTE = listOf(
    Color(0xFFFF0000), // Red
    Color(0xFFFF7F00), // Orange
    Color(0xFF00FF00), // Green
    Color(0xFF0000FF)  // Blue
)

/**
 * Tags cycle through the game rainbow palette - enough to tell neighbouring tags apart, not a
 * per-tag legend. Text is black or white, whichever reads better on the color.
 */
fun FeedbackTag.colors(): FeedbackColors {
    val container = TAG_PALETTE[ordinal % TAG_PALETTE.size]
    val content = if (container.luminance() > 0.3f) Color.Black else Color.White
    return FeedbackColors(container, content)
}

/** Done (implemented / fixed) and in-progress entries are colored, requested / open stay neutral. */
@Composable
fun FeedbackStatus.colors(): FeedbackColors = with(MaterialTheme.colorScheme) {
    when (this@colors) {
        FeedbackStatus.IMPLEMENTED, FeedbackStatus.FIXED -> FeedbackColors(tertiary, onTertiary)
        FeedbackStatus.PLANNED, FeedbackStatus.CONFIRMED -> FeedbackColors(secondary, onSecondary)
        FeedbackStatus.REQUESTED, FeedbackStatus.OPEN -> FeedbackColors(surfaceVariant, onSurfaceVariant)
    }
}
