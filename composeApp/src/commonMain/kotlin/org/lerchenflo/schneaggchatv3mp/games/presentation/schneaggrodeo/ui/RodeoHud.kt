package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.StringResource
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.RodeoMap
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_map_cave
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_map_fossil
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_map_rainbow
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_map_sea
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_map_welcome
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.SPEEDOMETER_FILL
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.SPEEDOMETER_RING
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.SPEEDOMETER_TEXT
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind

/** Pops up over the track when the rider boards a vehicle. */
@Composable
internal fun RodeoAnnouncementBanner(announcement: RodeoVehicleKind?, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = announcement,
        transitionSpec = {
            (fadeIn() + scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))) togetherWith
                    (fadeOut() + scaleOut(targetScale = 1.3f))
        },
        contentAlignment = Alignment.Center,
        label = "rodeoAnnouncement",
        modifier = modifier
    ) { shown ->
        if (shown == null) return@AnimatedContent
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 4.dp
        ) {
            Text(
                text = stringResource(shown.boardingCall ?: shown.title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * Shown big over almost the whole track for a moment after arriving on another map: its name. Draws
 * no input handling of its own, so riding goes on underneath.
 */
@Composable
internal fun RodeoMapTitle(map: RodeoMap?, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = map,
        transitionSpec = {
            (fadeIn() + scaleIn(initialScale = 0.6f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))) togetherWith
                    (fadeOut() + scaleOut(targetScale = 1.15f))
        },
        contentAlignment = Alignment.Center,
        label = "rodeoMapTitle",
        modifier = modifier
    ) { shown ->
        val name = shown?.let(::mapName) ?: return@AnimatedContent
        val colors = MaterialTheme.colorScheme
        Box(
            modifier = Modifier
                .fillMaxSize(MAP_TITLE_SIZE)
                .background(colors.primaryContainer.copy(alpha = 0.92f), RoundedCornerShape(28.dp))
                .border(4.dp, colors.primary, RoundedCornerShape(28.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(Res.string.games_schneaggrodeo_map_welcome),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimaryContainer,
                )
                Text(
                    text = stringResource(name),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = colors.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

/** Share of the track the map title covers. */
private const val MAP_TITLE_SIZE = 0.85f

private fun mapName(map: RodeoMap): StringResource? = when (map) {
    RodeoMap.SURFACE -> null
    RodeoMap.CAVE -> Res.string.games_schneaggrodeo_map_cave
    RodeoMap.SEA -> Res.string.games_schneaggrodeo_map_sea
    RodeoMap.RAINBOW -> Res.string.games_schneaggrodeo_map_rainbow
    RodeoMap.FOSSIL -> Res.string.games_schneaggrodeo_map_fossil
}

/** Round speed sign like the one on the Schneaggmap. */
@Composable
internal fun RodeoSpeedometer(speedKmh: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .background(SPEEDOMETER_FILL, CircleShape)
            .border(width = 3.dp, color = SPEEDOMETER_RING, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$speedKmh",
            color = SPEEDOMETER_TEXT,
            fontWeight = FontWeight.Bold,
            // The tractor's five digits need a smaller font to fit the sign
            style = if (speedKmh >= 10_000) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleMedium,
            maxLines = 1
        )
    }
}
