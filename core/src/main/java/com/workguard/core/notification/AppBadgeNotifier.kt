package com.workguard.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object AppBadgeNotifier {
    private const val CHANNEL_ID = "workguard_badge"
    private const val CHANNEL_NAME = "WorkGuard Badge"
    private const val NOTIF_ID = 4002

    fun updateViolationsBadge(context: Context, count: Int) {
        val appContext = context.applicationContext
        createChannelIfNeeded(appContext)

        val manager = NotificationManagerCompat.from(appContext)
        if (count <= 0) {
            runCatching { manager.cancel(NOTIF_ID) }
            return
        }

        val launchIntent = appContext.packageManager
            .getLaunchIntentForPackage(appContext.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        val pendingIntent = launchIntent?.let { intent ->
            PendingIntent.getActivity(
                appContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentFlags()
            )
        }

        // Badges on Android are driven by active notifications. This notification is silent and
        // only exists to carry the badge count.
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(appContext.applicationInfo.icon)
            .setContentTitle("Pelanggaran")
            .setContentText("Ada $count pelanggaran hari ini")
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setNumber(count)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .build()

        runCatching { manager.notify(NOTIF_ID, notification) }
    }

    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            setShowBadge(true)
            description = "Notifikasi silent untuk menampilkan badge aplikasi."
        }
        manager.createNotificationChannel(channel)
    }

    private fun pendingIntentFlags(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }
}

