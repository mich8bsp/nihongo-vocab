package io.github.mich8bsp.nihongovocab.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import io.github.mich8bsp.nihongovocab.MainActivity
import io.github.mich8bsp.nihongovocab.data.AppDatabase
import io.github.mich8bsp.nihongovocab.data.Entry
import io.github.mich8bsp.nihongovocab.data.QuizPreferences
import io.github.mich8bsp.nihongovocab.data.displayText
import io.github.mich8bsp.nihongovocab.data.meaningsWithRomaji
import io.github.mich8bsp.nihongovocab.data.pickRandomActiveEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val ACTION_FIRE = "io.github.mich8bsp.nihongovocab.ACTION_FIRE_QUIZ_NOTIFICATION"
private const val CHANNEL_ID = "quiz_reminders"
private const val PREFS_NAME = "quiz_alarm"
private const val KEY_NEXT_TRIGGER_AT = "next_trigger_at"

/**
 * Fires quiz notifications via AlarmManager instead of WorkManager - WorkManager's
 * JobScheduler backend gets deferred indefinitely by Doze/App Standby once the app
 * hasn't been opened in a while (observed: no notifications until the app is
 * opened, at which point the overdue one fires immediately). AlarmManager's
 * *AndAllowWhileIdle variants are the platform's own Doze-resistant mechanism for
 * this - see DESIGN.md "Notifications".
 */
class QuizAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> ensureScheduled(context)
            ACTION_FIRE -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getInstance(context)
                        val entry = pickRandomActiveEntry(db.entryDao(), db.poolStateDao())
                        if (entry != null) {
                            if (Random.nextInt(5) == 0) {
                                showQuizNotification(context, entry)
                            } else {
                                showRevealNotification(context, entry)
                            }
                        }
                        scheduleNext(context)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    /** Tap opens the Quiz screen for [entry] - the original notification type. */
    private fun showQuizNotification(context: Context, entry: Entry) {
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ENTRY_ID, entry.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            entry.id.toInt(),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Quiz time")
            .setContentText(entry.displayText())
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        postNotification(context, entry.id.toInt(), notification)
    }

    /**
     * Shows the word, romaji, and meaning directly - not clickable (no
     * `contentIntent`, no `setAutoCancel`), dismiss-only, for passive
     * review without a quiz prompt.
     */
    private fun showRevealNotification(context: Context, entry: Entry) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(entry.displayText())
            .setContentText(entry.meaningsWithRomaji())
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        postNotification(context, entry.id.toInt(), notification)
    }

    private fun postNotification(context: Context, id: Int, notification: android.app.Notification) {
        val channel = NotificationChannel(CHANNEL_ID, "Quiz reminders", NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }

    companion object {
        /** Called after a notification fires (or was skipped) - always picks a fresh delay. */
        fun scheduleNext(context: Context) = schedule(context, computeNextDelayMillis())

        /**
         * Called on app start / boot - re-arms without disturbing an already-pending
         * countdown. No-ops if notifications are disabled in Settings.
         */
        fun ensureScheduled(context: Context) {
            if (!QuizPreferences.isNotificationsEnabled(context)) return
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val storedTriggerAt = prefs.getLong(KEY_NEXT_TRIGGER_AT, -1L)
            if (storedTriggerAt > System.currentTimeMillis()) {
                arm(context, storedTriggerAt)
            } else {
                schedule(context, computeNextDelayMillis())
            }
        }

        /** Called from Settings when the notifications toggle changes. */
        fun setEnabled(context: Context, enabled: Boolean) {
            QuizPreferences.setNotificationsEnabled(context, enabled)
            if (enabled) {
                ensureScheduled(context)
            } else {
                context.getSystemService<AlarmManager>()?.cancel(firePendingIntent(context))
            }
        }

        private fun schedule(context: Context, delayMillis: Long) {
            val triggerAt = System.currentTimeMillis() + delayMillis
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putLong(KEY_NEXT_TRIGGER_AT, triggerAt)
                .apply()
            arm(context, triggerAt)
        }

        private fun firePendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, QuizAlarmReceiver::class.java).setAction(ACTION_FIRE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private fun arm(context: Context, triggerAtMillis: Long) {
            val alarmManager = context.getSystemService<AlarmManager>() ?: return
            val pendingIntent = firePendingIntent(context)
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                // ponytail: exact-alarm permission not granted - falls back to an inexact
                // Doze-aware alarm (still far better than plain JobScheduler deferral).
                // MainActivity prompts for the exact permission on launch.
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }
    }
}
