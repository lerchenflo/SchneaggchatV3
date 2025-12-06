@file:OptIn( ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.sharedUi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.trace
import kotlinx.coroutines.isActive
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun AutoFadePopup(
    message: String,
    onDismiss: () -> Unit,
    showDuration: Long = 3000L,
    exitAnimationDelay: Long = 300L,
) {
    var visible by remember { mutableStateOf(true) }
    var dismissedCalled by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Auto-hide after showDuration (if not dismissed manually)
    LaunchedEffect(Unit) {
        delay(showDuration)
        if (!dismissedCalled) {
            visible = false
            delay(exitAnimationDelay)
            if (!dismissedCalled) {
                dismissedCalled = true
                onDismiss()
            }
        }
    }


    AnimatedVisibility(
        visible = visible,
        exit = fadeOut()
    ) {
        Popup(
            alignment = Alignment.TopCenter,
            properties = PopupProperties(
                focusable = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(15.dp)
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(15.dp)
                    )
                    .padding(12.dp)
            ) {
                Column{
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )

                        IconButton(
                            onClick = {
                                if (!dismissedCalled) {
                                    dismissedCalled = true
                                    visible = false
                                    scope.launch {
                                        delay(exitAnimationDelay)
                                        onDismiss()
                                    }
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    var currentProgress by remember { mutableStateOf(1f) }

                    LaunchedEffect(key1 = Unit) {
                        val startTime = Clock.System.now().toEpochMilliseconds()
                        val endTime = startTime + showDuration

                        while (isActive && Clock.System.now().toEpochMilliseconds() < endTime) {
                            val elapsed = Clock.System.now().toEpochMilliseconds() - startTime
                            val progress = 1f - (elapsed.toFloat() / showDuration.toFloat())
                            currentProgress = progress.coerceIn(0f, 1f)
                            delay(16)
                        }
                        currentProgress = 0f
                    }

                    LinearProgressIndicator(
                        progress = {currentProgress},
                        modifier = Modifier.fillMaxWidth(),
                        trackColor = MaterialTheme.colorScheme.background,
                        drawStopIndicator = {}
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAutoFadePopup_TopCentered() {
    // In preview, show the popup persistently (no auto-dismiss) at top center
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            var show by remember { mutableStateOf(true) }
            if (show) {
                AutoFadePopup(
                    message = "This is a top-centered popup — you can still use the app.",
                    onDismiss = { show = false },
                    showDuration = Long.MAX_VALUE, // disable auto fade in preview
                )
            }

            // Sample underlying content to demonstrate interactions remain possible
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Underlying app content is interactive")
            }
        }
    }
}