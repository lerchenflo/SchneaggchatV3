package org.lerchenflo.schneaggchatv3mp.utilities.wake

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.ConfirmSlider
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.wake_notification_title
import schneaggchatv3mp.composeapp.generated.resources.wake_notification_title_group
import schneaggchatv3mp.composeapp.generated.resources.wake_swipe_to_stop
import schneaggchatv3mp.composeapp.generated.resources.wake_woken_alongside

/**
 * Lock screen takeover shown while the wake alarm is ringing. Only reached when the user granted
 * USE_FULL_SCREEN_INTENT (auto-granted below API 34), or when the app happens to be in the
 * foreground - otherwise the notification alone has to do.
 */
class WakeAlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showOverLockScreen()

        val senderName = intent?.getStringExtra(WakeAlarmService.EXTRA_SENDER_NAME).orEmpty()
        val reason = intent?.getStringExtra(WakeAlarmService.EXTRA_REASON).orEmpty()
        val groupName = intent?.getStringExtra(WakeAlarmService.EXTRA_GROUP_NAME).orEmpty()
        val wokenUserCount = intent?.getIntExtra(WakeAlarmService.EXTRA_WOKEN_USER_COUNT, 1) ?: 1
        val wokenDeviceCount = intent?.getIntExtra(WakeAlarmService.EXTRA_WOKEN_DEVICE_COUNT, 1) ?: 1

        setContent {
            WakeAlarmScreen(
                senderName = senderName,
                reason = reason,
                groupName = groupName,
                wokenUserCount = wokenUserCount,
                wokenDeviceCount = wokenDeviceCount,
                onStop = ::stopAlarmAndFinish
            )
        }
    }

    private fun showOverLockScreen() {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        keyguardManager?.requestDismissKeyguard(this, null)
    }

    private fun stopAlarmAndFinish() {
        startService(
            Intent(this, WakeAlarmService::class.java).setAction(WakeAlarmService.ACTION_STOP)
        )
        finish()
    }
}

@Composable
private fun WakeAlarmScreen(
    senderName: String,
    reason: String,
    groupName: String,
    wokenUserCount: Int,
    wokenDeviceCount: Int,
    onStop: () -> Unit,
) {
    SchneaggchatTheme {
        // Normal surface background, with the urgency carried by the pulsing accent icon instead
        // of a full bleed red screen - that stayed readable in every theme variant and did not
        // fight the rest of the app.
        val accent = MaterialTheme.colorScheme.error

        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.height(1.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SpinningAlarmIcon(accent = accent)

                    Spacer(Modifier.height(32.dp))

                    Text(
                        text = if (groupName.isNotEmpty()) {
                            stringResource(Res.string.wake_notification_title_group, senderName, groupName)
                        } else {
                            stringResource(Res.string.wake_notification_title, senderName)
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    if (reason.isNotEmpty()) {
                        // Raised block so a long reason stays readable instead of floating as
                        // loose text in the middle of the screen.
                        Surface(
                            modifier = Modifier
                                .padding(top = 24.dp)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                            )
                        }
                    }

                    if (wokenUserCount > 1) {
                        Text(
                            text = stringResource(
                                Res.string.wake_woken_alongside,
                                wokenUserCount,
                                wokenDeviceCount
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }

                // Deliberately a swipe rather than a button: a tap is far too easy to hit by
                // accident while half asleep, which would silence the alarm unintentionally.
                ConfirmSlider(
                    text = stringResource(Res.string.wake_swipe_to_stop),
                    thumbIcon = Icons.Default.AlarmOff,
                    width = 280.dp,
                    progressColor = accent,
                    thumbColor = MaterialTheme.colorScheme.onError,
                    //Stopping is terminal - the screen closes, so resetting the track is pointless.
                    autoResetMillis = null,
                    trailingIcon = {
                        Icon(imageVector = Icons.Default.AlarmOff, contentDescription = null)
                    },
                    onConfirm = onStop
                )
            }
        }
    }
}

/** Continuously turning alarm clock, so the screen reads as actively ringing rather than static. */
@Composable
private fun SpinningAlarmIcon(accent: Color) {
    val transition = rememberInfiniteTransition(label = "wakeSpin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            //Linear + Restart, otherwise the rotation visibly stutters once per lap.
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wakeSpinAngle"
    )

    Box(
        modifier = Modifier
            .size(140.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Alarm,
            contentDescription = null,
            tint = accent,
            modifier = Modifier
                .size(64.dp)
                .rotate(angle)
        )
    }
}

@Preview(
    name = "Wake alarm - 1:1",
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun WakeAlarmScreenPreview() {
    WakeAlarmScreen(
        senderName = "John Doe",
        reason = "Pack dei zeug, mir fahrat in 10 minuta",
        groupName = "",
        wokenUserCount = 1,
        wokenDeviceCount = 1,
        onStop = {}
    )
}

@Preview(
    name = "Wake alarm - group",
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun WakeAlarmScreenGroupPreview() {
    WakeAlarmScreen(
        senderName = "John Doe",
        reason = "Wer kunnt no zum fussball?",
        groupName = "Schneagg Buaba",
        wokenUserCount = 4,
        wokenDeviceCount = 7,
        onStop = {}
    )
}

/** No reason given - the reason block should disappear instead of leaving an empty gap. */
@Preview(
    name = "Wake alarm - no reason",
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun WakeAlarmScreenNoReasonPreview() {
    WakeAlarmScreen(
        senderName = "John Doe",
        reason = "",
        groupName = "",
        wokenUserCount = 1,
        wokenDeviceCount = 1,
        onStop = {}
    )
}
