package org.lerchenflo.schneaggchatv3mp.utilities.wake

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.NotificationConfig

/**
 * Plays the system alarm ringtone over the ALARM stream when somebody wakes this user, and stops
 * on its own after [ALARM_TIMEOUT_MS]. Runs as a foreground service so it survives the app being
 * backgrounded or killed - the FCM message that triggers it arrives with high priority, which
 * grants the short background start window this relies on.
 */
class WakeAlarmService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val stopRunnable = Runnable { stopSelf() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val senderName = intent?.getStringExtra(EXTRA_SENDER_NAME).orEmpty()
        val reason = intent?.getStringExtra(EXTRA_REASON).orEmpty()
        val groupName = intent?.getStringExtra(EXTRA_GROUP_NAME).orEmpty()
        val wokenUserCount = intent?.getIntExtra(EXTRA_WOKEN_USER_COUNT, 1) ?: 1
        val wokenDeviceCount = intent?.getIntExtra(EXTRA_WOKEN_DEVICE_COUNT, 1) ?: 1

        createChannelIfNeeded()

        // Two phase on purpose: startForeground has to happen within a few seconds of the service
        // starting or the system kills us, but the localized strings come from Compose Resources
        // and are only reachable from a coroutine. So show an unlocalized notification built
        // purely from the payload first, then swap in the proper one a moment later.
        startForegroundCompat(
            buildNotification(senderName, reason, groupName, wokenUserCount, wokenDeviceCount, null)
        )

        serviceScope.launch {
            runCatching {
                val strings = WakeNotificationStrings.resolve(
                    senderName = senderName,
                    groupName = groupName,
                    reason = reason,
                    wokenUserCount = wokenUserCount,
                    wokenDeviceCount = wokenDeviceCount,
                )
                val manager = ContextCompat.getSystemService(
                    this@WakeAlarmService,
                    NotificationManager::class.java
                )
                manager?.notify(
                    NOTIFICATION_ID,
                    buildNotification(senderName, reason, groupName, wokenUserCount, wokenDeviceCount, strings)
                )
            }
        }

        // A second wake arriving mid-alarm restarts the countdown instead of stacking a second
        // player on top of the first.
        if (mediaPlayer == null) {
            acquireWakeLock()
            startAlarm()
        }
        timeoutHandler.removeCallbacks(stopRunnable)
        timeoutHandler.postDelayed(stopRunnable, ALARM_TIMEOUT_MS)

        launchAlarmScreenIfForeground(senderName, reason, groupName, wokenUserCount, wokenDeviceCount)

        return START_NOT_STICKY
    }

    /**
     * The notification's full screen intent only takes over the screen when the device is locked
     * or the screen is off - Android deliberately downgrades it to a heads-up notification
     * otherwise. So when the app is already in the foreground (the only situation where we are
     * allowed to start an activity from the background) launch the alarm screen directly, so an
     * unlocked, in-app user gets the same experience.
     */
    private fun launchAlarmScreenIfForeground(
        senderName: String,
        reason: String,
        groupName: String,
        wokenUserCount: Int,
        wokenDeviceCount: Int,
    ) {
        if (!AppLifecycleManager.isAppInForeground) return

        runCatching {
            startActivity(
                Intent(this, WakeAlarmActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(EXTRA_SENDER_NAME, senderName)
                    .putExtra(EXTRA_REASON, reason)
                    .putExtra(EXTRA_GROUP_NAME, groupName)
                    .putExtra(EXTRA_WOKEN_USER_COUNT, wokenUserCount)
                    .putExtra(EXTRA_WOKEN_DEVICE_COUNT, wokenDeviceCount)
            )
        }
    }

    override fun onDestroy() {
        timeoutHandler.removeCallbacks(stopRunnable)
        serviceScope.cancel()
        releaseAlarm()
        releaseWakeLock()
        //Explicit so the ongoing notification is gone the moment the alarm stops, whichever of
        //the three exit paths (stop action, timeout, system kill) got us here.
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        super.onDestroy()
    }

    private fun startAlarm() {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: return

        runCatching {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@WakeAlarmService, alarmUri)
                // USAGE_ALARM is what makes this audible through silent mode and Do Not Disturb,
                // and ties the volume to the alarm slider rather than the ringer.
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        }.onFailure {
            releaseAlarm()
        }
    }

    private fun releaseAlarm() {
        runCatching {
            mediaPlayer?.let { player ->
                if (player.isPlaying) player.stop()
                player.release()
            }
        }
        mediaPlayer = null
    }

    private fun acquireWakeLock() {
        runCatching {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "schneaggchat:wake_alarm"
            ).apply {
                setReferenceCounted(false)
                //Timeout as a safety net - the alarm should always beat it to stopSelf().
                acquire(ALARM_TIMEOUT_MS + 5_000L)
            }
        }
    }

    private fun releaseWakeLock() {
        runCatching {
            wakeLock?.let { if (it.isHeld) it.release() }
        }
        wakeLock = null
    }

    private fun startForegroundCompat(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /**
     * @param strings the localized texts, or null for the immediate pre-localization notification
     * that only exists to satisfy startForeground's deadline.
     */
    private fun buildNotification(
        senderName: String,
        reason: String,
        groupName: String,
        wokenUserCount: Int,
        wokenDeviceCount: Int,
        strings: WakeNotificationStrings?,
    ): android.app.Notification {
        val title = strings?.title
            ?: if (groupName.isNotEmpty()) "$senderName · $groupName" else senderName
        val body = strings?.body ?: reason

        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, WakeAlarmService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = PendingIntent.getActivity(
            this,
            0,
            //No CLEAR_TASK: dismissing the alarm should return the user to whatever they had
            //open, not wipe the app's task stack.
            Intent(this, WakeAlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_SENDER_NAME, senderName)
                .putExtra(EXTRA_REASON, reason)
                .putExtra(EXTRA_GROUP_NAME, groupName)
                .putExtra(EXTRA_WOKEN_USER_COUNT, wokenUserCount)
                .putExtra(EXTRA_WOKEN_DEVICE_COUNT, wokenDeviceCount),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(NotificationConfig.iconResId)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Only honoured when the user granted USE_FULL_SCREEN_INTENT (auto-granted below
            // API 34). Without it this silently degrades to a heads-up notification and the
            // alarm still sounds - we just lose the lock screen takeover.
            .setFullScreenIntent(fullScreenIntent, true)
            .apply {
                //Only add the action once we have a localized label for it.
                strings?.let { addAction(NotificationConfig.markAsReadIconResId, it.stopLabel, stopIntent) }
            }
            .build()
    }

    private fun createChannelIfNeeded() {
        val manager = ContextCompat.getSystemService(this, NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
            // The service plays the alarm itself, so the channel must stay silent - otherwise
            // the notification sound layers on top of the ringtone.
            setSound(null, null)
            enableVibration(true)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "schneaggchat_wake"
        private const val CHANNEL_NAME = "Wake-ups"
        private const val CHANNEL_DESCRIPTION = "Alarms triggered when a contact wakes you"
        private const val NOTIFICATION_ID = 4711

        const val ACTION_STOP = "org.lerchenflo.schneaggchatv3mp.WAKE_STOP"

        const val EXTRA_SENDER_NAME = "sender_name"
        const val EXTRA_REASON = "reason"
        const val EXTRA_GROUP_NAME = "group_name"
        const val EXTRA_WOKEN_USER_COUNT = "woken_user_count"
        const val EXTRA_WOKEN_DEVICE_COUNT = "woken_device_count"

        /** How long the alarm rings before giving up on its own. */
        const val ALARM_TIMEOUT_MS = 30_000L

        fun start(
            context: Context,
            senderName: String,
            reason: String,
            groupName: String,
            wokenUserCount: Int,
            wokenDeviceCount: Int,
        ) {
            val intent = Intent(context, WakeAlarmService::class.java)
                .putExtra(EXTRA_SENDER_NAME, senderName)
                .putExtra(EXTRA_REASON, reason)
                .putExtra(EXTRA_GROUP_NAME, groupName)
                .putExtra(EXTRA_WOKEN_USER_COUNT, wokenUserCount)
                .putExtra(EXTRA_WOKEN_DEVICE_COUNT, wokenDeviceCount)

            ContextCompat.startForegroundService(context, intent)
        }
    }
}
