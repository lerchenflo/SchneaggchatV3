@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.sharedUi.core

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun SnackbarPopup(
    snackbarEvent: SnackbarManager.SnackbarEvent,
    onDismiss: () -> Unit,
) {
    var visible by remember { mutableStateOf(true) }
    var dismissedCalled by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun dismiss() {
        if (!dismissedCalled) {
            dismissedCalled = true
            visible = false
            scope.launch {
                delay(300)
                onDismiss()
            }
        }
    }

    // Auto-hide after showTime
    LaunchedEffect(Unit) {
        delay(snackbarEvent.showTime)
        dismiss()
    }

    Popup(
        alignment = Alignment.BottomCenter,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                shadowElevation = 6.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = snackbarEvent.message,   // fix 1: was `message`
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 14.dp)
                        )

                        IconButton(
                            onClick = { dismiss() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.inverseOnSurface
                            )
                        }
                    }

                    var currentProgress by remember { mutableStateOf(1f) }

                    LaunchedEffect(Unit) {
                        val startTime = Clock.System.now().toEpochMilliseconds()
                        val endTime = startTime + snackbarEvent.showTime  // fix 2: was `showDuration`

                        while (isActive && Clock.System.now().toEpochMilliseconds() < endTime) {
                            val elapsed = Clock.System.now().toEpochMilliseconds() - startTime
                            currentProgress = (1f - elapsed.toFloat() / snackbarEvent.showTime.toFloat())  // fix 2
                                .coerceIn(0f, 1f)
                            delay(16)
                        }
                        currentProgress = 0f
                    }

                    LinearProgressIndicator(
                        progress = { currentProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.inversePrimary,
                        trackColor = MaterialTheme.colorScheme.inverseSurface,
                        drawStopIndicator = {}
                    )
                }
            }
        }
    }
}