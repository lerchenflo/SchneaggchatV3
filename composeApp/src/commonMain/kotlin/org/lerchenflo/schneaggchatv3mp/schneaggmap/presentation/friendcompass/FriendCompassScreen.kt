package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.friendcompass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.utilities.bearing
import org.lerchenflo.schneaggchatv3mp.utilities.distanceMeters
import org.lerchenflo.schneaggchatv3mp.utilities.formatDistance
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.direction_e
import schneaggchatv3mp.composeapp.generated.resources.direction_n
import schneaggchatv3mp.composeapp.generated.resources.direction_ne
import schneaggchatv3mp.composeapp.generated.resources.direction_nw
import schneaggchatv3mp.composeapp.generated.resources.direction_s
import schneaggchatv3mp.composeapp.generated.resources.direction_se
import schneaggchatv3mp.composeapp.generated.resources.direction_sw
import schneaggchatv3mp.composeapp.generated.resources.direction_w
import schneaggchatv3mp.composeapp.generated.resources.friend_compass_no_friends
import schneaggchatv3mp.composeapp.generated.resources.friend_compass_no_sensor
import schneaggchatv3mp.composeapp.generated.resources.friend_compass_title
import schneaggchatv3mp.composeapp.generated.resources.friend_compass_waiting_gps
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin

/** One friend prepared for compass rendering - bearing/distance relative to the own position. */
private data class FriendCompassEntry(
    val userId: String,
    val name: String,
    val bearingDegrees: Float,
    val distanceMeters: Double,
)

@Composable
fun FriendCompassRoot(
    targetUserId: String? = null,
) {
    val viewModel = koinViewModel<FriendCompassViewModel> { parametersOf(targetUserId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    FriendCompassScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun FriendCompassScreen(
    state: FriendCompassState,
    onAction: (FriendCompassAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        ActivityTitle(
            title = stringResource(Res.string.friend_compass_title),
            onBackClick = { onAction(FriendCompassAction.OnBackClick) }
        )

        val entries = remember(state.friends, state.ownLocation) {
            val own = state.ownLocation ?: return@remember emptyList<FriendCompassEntry>()
            state.friends.mapNotNull { user ->
                val location = user.location ?: return@mapNotNull null
                val friendPosition = LatLong(lat = location.lat, long = location.long)
                FriendCompassEntry(
                    userId = user.id,
                    name = user.displayName,
                    bearingDegrees = bearing(own, friendPosition).toFloat(),
                    distanceMeters = distanceMeters(own, friendPosition),
                )
            }.sortedBy { it.distanceMeters }
        }

        // Smoothly animated device azimuth, unwrapped across the 359->0 boundary so the
        // needle never spins the long way around.
        val animatedAzimuth = remember { Animatable(0f) }
        LaunchedEffect(state.azimuthDegrees) {
            val target = state.azimuthDegrees ?: 0f
            val current = animatedAzimuth.value
            val delta = ((target - current) % 360f + 540f) % 360f - 180f
            animatedAzimuth.animateTo(
                targetValue = current + delta,
                animationSpec = tween(durationMillis = 250, easing = LinearOutSlowInEasing)
            )
        }

        when {
            state.ownLocation == null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.friend_compass_waiting_gps),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                CompassDial(
                    entries = entries,
                    azimuthDegrees = animatedAzimuth.value,
                    targetUserId = state.targetUserId,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(8.dp)
                )

                if (state.azimuthDegrees == null) {
                    Text(
                        text = stringResource(Res.string.friend_compass_no_sensor),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (entries.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.friend_compass_no_friends),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                    )
                } else {
                    FriendList(
                        entries = entries,
                        azimuthDegrees = animatedAzimuth.value,
                        targetUserId = state.targetUserId,
                        onFriendClick = { onAction(FriendCompassAction.OnFriendClick(it)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompassDial(
    entries: List<FriendCompassEntry>,
    azimuthDegrees: Float,
    targetUserId: String?,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()

    val cardinalLabels = listOf(
        stringResource(Res.string.direction_n),
        stringResource(Res.string.direction_e),
        stringResource(Res.string.direction_s),
        stringResource(Res.string.direction_w),
    )

    val outlineColor = MaterialTheme.colorScheme.outline
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    val cardinalStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
    val labelStyle = TextStyle(fontSize = 11.sp)

    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Outer ring
        drawCircle(
            color = outlineColor,
            radius = radius * 0.98f,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // Tick marks every 30 degrees, rotating against the device azimuth so they stay
        // fixed relative to the real world.
        for (tickDegrees in 0 until 360 step 30) {
            val screenAngleRad = (tickDegrees - azimuthDegrees - 90f) * PI.toFloat() / 180f
            val isCardinal = tickDegrees % 90 == 0
            val outer = radius * 0.98f
            val inner = if (isCardinal) radius * 0.88f else radius * 0.93f
            val direction = Offset(cos(screenAngleRad), sin(screenAngleRad))
            drawLine(
                color = if (isCardinal) onSurfaceColor else outlineColor,
                start = center + direction * inner,
                end = center + direction * outer,
                strokeWidth = if (isCardinal) 3.dp.toPx() else 1.5f.dp.toPx()
            )
        }

        // Cardinal letters (N highlighted)
        cardinalLabels.forEachIndexed { index, label ->
            val bearingDegrees = index * 90f
            val screenAngleRad = (bearingDegrees - azimuthDegrees - 90f) * PI.toFloat() / 180f
            val position = center + Offset(cos(screenAngleRad), sin(screenAngleRad)) * (radius * 0.78f)
            val layout = textMeasurer.measure(
                text = label,
                style = cardinalStyle.copy(color = if (index == 0) primaryColor else onSurfaceVariantColor)
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = position.x - layout.size.width / 2f,
                    y = position.y - layout.size.height / 2f
                )
            )
        }

        // Own position in the middle
        drawCircle(color = primaryColor, radius = 5.dp.toPx(), center = center)

        // Friends around the own position. The radius is log-scaled so a friend 50 m away
        // and one 5 km away both stay readable on the same dial.
        val maxDistance = entries.maxOfOrNull { it.distanceMeters } ?: 0.0
        entries.forEach { entry ->
            val isTarget = entry.userId == targetUserId
            val normalized = if (maxDistance <= 0.0) {
                1.0
            } else {
                log10(1.0 + entry.distanceMeters) / log10(1.0 + maxDistance)
            }
            val markerRadius = radius * (0.25f + 0.45f * normalized.toFloat())
            val screenAngleRad = (entry.bearingDegrees - azimuthDegrees - 90f) * PI.toFloat() / 180f
            val position = center + Offset(cos(screenAngleRad), sin(screenAngleRad)) * markerRadius

            if (isTarget) {
                // Pointer line from the own position towards the target friend
                drawLine(
                    color = primaryColor,
                    start = center,
                    end = position,
                    strokeWidth = 3.dp.toPx()
                )
            }

            drawCircle(
                color = if (isTarget) primaryColor else tertiaryColor,
                radius = if (isTarget) 8.dp.toPx() else 6.dp.toPx(),
                center = position
            )

            val nameLayout = textMeasurer.measure(
                text = entry.name,
                style = labelStyle.copy(
                    color = if (isTarget) primaryColor else onSurfaceColor,
                    fontWeight = if (isTarget) FontWeight.Bold else FontWeight.Normal
                )
            )
            val distanceLayout = textMeasurer.measure(
                text = formatDistance(entry.distanceMeters),
                style = labelStyle.copy(color = onSurfaceVariantColor)
            )
            drawText(
                textLayoutResult = nameLayout,
                topLeft = Offset(
                    x = position.x - nameLayout.size.width / 2f,
                    y = position.y + 8.dp.toPx()
                )
            )
            drawText(
                textLayoutResult = distanceLayout,
                topLeft = Offset(
                    x = position.x - distanceLayout.size.width / 2f,
                    y = position.y + 8.dp.toPx() + nameLayout.size.height
                )
            )
        }
    }
}

@Composable
private fun FriendList(
    entries: List<FriendCompassEntry>,
    azimuthDegrees: Float,
    targetUserId: String?,
    onFriendClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val directionLabels = listOf(
        stringResource(Res.string.direction_n),
        stringResource(Res.string.direction_ne),
        stringResource(Res.string.direction_e),
        stringResource(Res.string.direction_se),
        stringResource(Res.string.direction_s),
        stringResource(Res.string.direction_sw),
        stringResource(Res.string.direction_w),
        stringResource(Res.string.direction_nw),
    )

    LazyColumn(modifier = modifier) {
        items(entries, key = { it.userId }) { entry ->
            val isTarget = entry.userId == targetUserId
            Surface(
                color = if (isTarget) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onFriendClick(entry.userId) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = if (isTarget) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(entry.bearingDegrees - azimuthDegrees)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isTarget) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${formatDistance(entry.distanceMeters)} ${directionLabels[cardinalIndex(entry.bearingDegrees)]}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isTarget) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

/** Maps a bearing in degrees to one of 8 cardinal direction indexes (0 = N, 1 = NE, ...). */
private fun cardinalIndex(bearingDegrees: Float): Int {
    val normalized = (bearingDegrees % 360f + 360f) % 360f
    return (((normalized + 22.5f) % 360f) / 45f).toInt()
}
