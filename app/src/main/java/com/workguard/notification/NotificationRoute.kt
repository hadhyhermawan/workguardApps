package com.workguard.notification

import android.content.Intent
import com.workguard.navigation.Routes

object NotificationRoute {
    const val EXTRA_TARGET = "notification_target"
    const val EXTRA_NEWS_ID = "notification_news_id"
    const val EXTRA_THREAD_ID = "notification_thread_id"

    const val TARGET_NEWS = "news"
    const val TARGET_CHAT = "chat"

    fun routeFromIntent(intent: Intent?): String? {
        val target = intent?.getStringExtra(EXTRA_TARGET) ?: return null
        return when (target) {
            TARGET_NEWS -> {
                val newsId = intent.getIntExtra(EXTRA_NEWS_ID, -1)
                if (newsId > 0) Routes.newsDetail(newsId) else Routes.News
            }
            TARGET_CHAT -> {
                val threadId = intent.getStringExtra(EXTRA_THREAD_ID)
                if (!threadId.isNullOrBlank()) Routes.chatThread(threadId) else Routes.Chat
            }
            else -> null
        }
    }

    fun putNewsExtras(intent: Intent, newsId: Int?) {
        intent.putExtra(EXTRA_TARGET, TARGET_NEWS)
        if (newsId != null && newsId > 0) {
            intent.putExtra(EXTRA_NEWS_ID, newsId)
        }
    }

    fun putChatExtras(intent: Intent, threadId: String?) {
        intent.putExtra(EXTRA_TARGET, TARGET_CHAT)
        if (!threadId.isNullOrBlank()) {
            intent.putExtra(EXTRA_THREAD_ID, threadId)
        }
    }
}
