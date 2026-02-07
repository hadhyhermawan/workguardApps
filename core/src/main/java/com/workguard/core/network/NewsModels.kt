package com.workguard.core.network

import com.squareup.moshi.Json

data class NewsCategory(
    val id: Int? = null,
    val name: String? = null,
    val slug: String? = null,
    val count: Int? = null
)

data class NewsItem(
    val id: Int? = null,
    val title: String? = null,
    val summary: String? = null,
    @Json(name = "cover_url")
    val coverUrl: String? = null,
    @Json(name = "read_time_minutes")
    val readTimeMinutes: Int? = null,
    @Json(name = "published_at")
    val publishedAt: String? = null,
    val category: NewsCategory? = null
)

data class NewsAttachment(
    val name: String? = null,
    val url: String? = null
)

data class NewsDetail(
    val id: Int? = null,
    val title: String? = null,
    val summary: String? = null,
    val content: String? = null,
    @Json(name = "cover_url")
    val coverUrl: String? = null,
    val images: List<String>? = null,
    val attachments: List<NewsAttachment>? = null,
    @Json(name = "read_time_minutes")
    val readTimeMinutes: Int? = null,
    @Json(name = "published_at")
    val publishedAt: String? = null,
    val category: NewsCategory? = null
)

data class NewsPagination(
    val page: Int? = null,
    val limit: Int? = null,
    val total: Int? = null,
    @Json(name = "total_pages")
    val totalPages: Int? = null
)

data class NewsListResponse(
    val items: List<NewsItem>? = null,
    val pagination: NewsPagination? = null
)

