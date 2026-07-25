package io.github.mich8bsp.nihongovocab.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.mich8bsp.nihongovocab.MainActivity
import io.github.mich8bsp.nihongovocab.data.AppDatabase
import io.github.mich8bsp.nihongovocab.data.Entry
import java.util.concurrent.TimeUnit

private const val WORK_NAME = "quiz_notification"
private const val CHANNEL_ID = "quiz_reminders"

class QuizNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val enabledLevels = db.poolStateDao().getEnabledLevels()
        val entry = db.entryDao().getRandomActiveEntry(enabledLevels)
        if (entry != null) {
            showNotification(applicationContext, entry)
        }
        scheduleNext(applicationContext)
        return Result.success()
    }

    private fun showNotification(context: Context, entry: Entry) {
        val channel = NotificationChannel(CHANNEL_ID, "Quiz reminders", NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ENTRY_ID, entry.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            entry.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Quiz time")
            .setContentText(entry.text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(entry.id.toInt(), notification)
        }
    }

    companion object {
        /** Called from within doWork() - always replaces, since we just ran. */
        private fun scheduleNext(context: Context) {
            enqueue(context, ExistingWorkPolicy.REPLACE)
        }

        /** Called on app start - never disturbs an already-pending countdown. */
        fun ensureScheduled(context: Context) {
            enqueue(context, ExistingWorkPolicy.KEEP)
        }

        private fun enqueue(context: Context, policy: ExistingWorkPolicy) {
            val request = OneTimeWorkRequestBuilder<QuizNotificationWorker>()
                .setInitialDelay(computeNextDelayMillis(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, policy, request)
        }
    }
}
