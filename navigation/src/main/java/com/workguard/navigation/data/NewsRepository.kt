package com.workguard.navigation.data

import com.workguard.core.network.ApiResult
import com.workguard.core.network.ApiService
import com.workguard.core.network.NewsCategory
import com.workguard.core.network.NewsDetail
import com.workguard.core.network.NewsItem
import com.workguard.core.network.NewsPagination
import java.io.IOException
import javax.inject.Inject
import retrofit2.HttpException

data class NewsFeed(
    val featured: NewsItem? = null,
    val categories: List<NewsCategory> = emptyList(),
    val items: List<NewsItem> = emptyList(),
    val pagination: NewsPagination? = null
)

interface NewsRepository {
    suspend fun loadNewsFeed(
        query: String? = null,
        categoryId: Int? = null,
        categorySlug: String? = null,
        page: Int? = null,
        limit: Int? = null,
        sort: String? = null
    ): ApiResult<NewsFeed>

    suspend fun getNewsDetail(id: Int): ApiResult<NewsDetail>
}

class NewsRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : NewsRepository {
    override suspend fun loadNewsFeed(
        query: String?,
        categoryId: Int?,
        categorySlug: String?,
        page: Int?,
        limit: Int?,
        sort: String?
    ): ApiResult<NewsFeed> {
        return try {
            val listResponse = apiService.getNewsList(
                q = query,
                categoryId = categoryId,
                categorySlug = categorySlug,
                page = page,
                limit = limit,
                sort = sort
            )
            if (listResponse.success == false) {
                return ApiResult.Error(
                    IllegalStateException(listResponse.message ?: "Gagal memuat berita")
                )
            }

            val categoriesResponse = runCatching { apiService.getNewsCategories() }.getOrNull()
            val featuredResponse = runCatching { apiService.getNewsFeatured() }.getOrNull()

            val categories = if (categoriesResponse?.success == false) {
                emptyList()
            } else {
                categoriesResponse?.data ?: emptyList()
            }
            val featured = if (featuredResponse?.success == false) {
                null
            } else {
                featuredResponse?.data
            }
            val items = listResponse.data?.items ?: emptyList()
            val pagination = listResponse.data?.pagination

            ApiResult.Success(
                NewsFeed(
                    featured = featured,
                    categories = categories,
                    items = items,
                    pagination = pagination
                )
            )
        } catch (e: HttpException) {
            ApiResult.Error(IllegalStateException("Gagal memuat berita (${e.code()})"))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    override suspend fun getNewsDetail(id: Int): ApiResult<NewsDetail> {
        return try {
            val response = apiService.getNewsDetail(id)
            if (response.success == false) {
                ApiResult.Error(
                    IllegalStateException(response.message ?: "Gagal memuat detail berita")
                )
            } else {
                ApiResult.Success(response.data ?: NewsDetail())
            }
        } catch (e: HttpException) {
            ApiResult.Error(IllegalStateException("Gagal memuat detail berita (${e.code()})"))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }
}

