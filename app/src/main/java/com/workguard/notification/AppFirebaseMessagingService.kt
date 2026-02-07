package com.workguard.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.workguard.core.util.DeviceIdProvider
import com.workguard.notification.PushTokenRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.workguard.MainActivity
import com.workguard.R

@AndroidEntryPoint
class AppFirebaseMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var pushTokenRepository: PushTokenRepository

    @Inject
    lateinit var deviceIdProvider: DeviceIdProvider

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token: $token")
        CoroutineScope(Dispatchers.IO).launch {
            pushTokenRepository.registerToken(token, deviceIdProvider.getDeviceId())
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val type = data["type"]?.lowercase()
        val rawNewsId = data["news_id"] ?: data["newsId"]
        val hasNewsId = rawNewsId?.toIntOrNull()?.let { it > 0 } == true
        val threadId = (data["thread_id"] ?: data["threadId"])?.takeIf { it.isNotBlank() }
        val hasThreadId = !threadId.isNullOrBlank()
        val title = message.notification?.title
            ?: data["title"]
            ?: "WorkGuard"
        val body = message.notification?.body
            ?: data["body"]
            ?: data["summary"]
            ?: ""
        when {
            type == NotificationRoute.TARGET_NEWS || hasNewsId -> {
                val newsId = rawNewsId?.toIntOrNull()
                showNotification(
                    title = title,
                    body = body,
                    channelId = CHANNEL_NEWS,
                    channelName = "WorkGuard News",
                    requestCode = newsId ?: 0
                ) { intent ->
                    NotificationRoute.putNewsExtras(intent, newsId)
                }
            }
            type == NotificationRoute.TARGET_CHAT || hasThreadId -> {
                showNotification(
                    title = title,
                    body = body,
                    channelId = CHANNEL_CHAT,
                    channelName = "WorkGuard Chat",
                    requestCode = threadId?.hashCode() ?: 0
                ) { intent ->
                    NotificationRoute.putChatExtras(intent, threadId)
                }
            }
            else -> {
                showNotification(
                    title = title,
                    body = body,
                    channelId = CHANNEL_ID,
                    channelName = "WorkGuard Notification",
                    requestCode = 0,
                    intentExtras = null
                )
            }
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        channelId: String,
        channelName: String,
        requestCode: Int,
        intentExtras: ((Intent) -> Unit)?
    ) {
        createChannel(channelId, channelName)
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intentExtras?.invoke(intent)
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentFlag()
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        NotificationManagerCompat.from(this)
            .notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createChannel(channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)
    }

    private fun pendingIntentFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "workguard_default"
        private const val CHANNEL_NEWS = "workguard_news"
        private const val CHANNEL_CHAT = "workguard_chat"
    }
}
