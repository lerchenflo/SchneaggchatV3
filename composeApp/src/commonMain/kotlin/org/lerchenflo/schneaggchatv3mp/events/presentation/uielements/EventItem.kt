package org.lerchenflo.schneaggchatv3mp.events.presentation.uielements

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.icon
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.event_ended
import schneaggchatv3mp.composeapp.generated.resources.event_private
import schneaggchatv3mp.composeapp.generated.resources.event_public
import schneaggchatv3mp.composeapp.generated.resources.event_started
import schneaggchatv3mp.composeapp.generated.resources.event_starts_in
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun EventItem(
    event: Event,
    creatorProfilePictureUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = event.type.icon(),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = if (event.public) Icons.Default.Public else Icons.Default.PublicOff,
                        contentDescription = stringResource(if (event.public) Res.string.event_public else Res.string.event_private),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )

                    if (!creatorProfilePictureUrl.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        ProfilePictureView(
                            filepath = creatorProfilePictureUrl,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                        )
                    }
                }
                if (event.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = millisToString(event.startDate, "dd.MM.yyyy HH:mm"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    EventStartCountdownTimer(
                        startDate = event.startDate,
                        closeDate = event.closeDate
                    )
                }
            }
        }
    }
}

@Composable
private fun EventStartCountdownTimer(
    startDate: Long,
    closeDate: Long?,
    modifier: Modifier = Modifier
) {
    var nowMillis by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }

    LaunchedEffect(startDate, closeDate) {
        while (true) {
            nowMillis = Clock.System.now().toEpochMilliseconds()
            delay(1000L.milliseconds)
        }
    }

    val timeRemaining = (startDate - nowMillis).coerceAtLeast(0L)

    val text = when {
        timeRemaining > 0 -> {
            val d = timeRemaining / (24 * 60 * 60 * 1000)
            val h = ((timeRemaining / (60 * 60 * 1000)) % 24).toString().padStart(2, '0')
            val m = ((timeRemaining / (60 * 1000)) % 60).toString().padStart(2, '0')
            val s = ((timeRemaining / 1000) % 60).toString().padStart(2, '0')

            val formattedTime = if (d > 0) {
                "${d}d $h:$m:$s"
            } else {
                "$h:$m:$s"
            }
            stringResource(Res.string.event_starts_in, formattedTime)
        }
        closeDate != null && nowMillis >= closeDate -> {
            stringResource(Res.string.event_ended)
        }
        else -> {
            stringResource(Res.string.event_started)
        }
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

