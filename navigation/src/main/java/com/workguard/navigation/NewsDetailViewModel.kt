package com.workguard.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workguard.core.network.ApiResult
import com.workguard.core.network.NewsDetail
import com.workguard.core.util.UrlUtil
import com.workguard.navigation.data.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NewsDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newsRepository: NewsRepository
) : ViewModel() {
    private val newsId: Int = savedStateHandle.get<String>(Routes.NewsIdArg)
        ?.toIntOrNull()
        ?: -1

    private val _state = MutableStateFlow(NewsDetailState())
    val state = _state.asStateFlow()

    init {
        loadDetail()
    }

    fun retry() {
        loadDetail()
    }

    private fun loadDetail() {
        if (newsId <= 0) {
            _state.update { it.copy(isLoading = false, errorMessage = "ID berita tidak valid") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = newsRepository.getNewsDetail(newsId)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            detail = mapDetail(result.data)
                        )
                    }
                }
                is ApiResult.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.throwable.message ?: "Gagal memuat detail berita"
                        )
                    }
                }
            }
        }
    }

    private fun mapDetail(detail: NewsDetail): NewsDetailUi {
        val title = detail.title?.takeIf { it.isNotBlank() } ?: "Tanpa judul"
        val summary = detail.summary?.takeIf { it.isNotBlank() } ?: ""
        val content = detail.content?.takeIf { it.isNotBlank() } ?: ""
        val category = detail.category?.name?.takeIf { it.isNotBlank() } ?: "Umum"
        val time = NewsTimeFormatter.formatRelativeTime(detail.publishedAt)
        val readTime = detail.readTimeMinutes?.takeIf { it > 0 }?.let { "$it menit" } ?: ""
        val attachments = detail.attachments.orEmpty().mapNotNull { attachment ->
            val name = attachment.name?.trim().takeIf { !it.isNullOrBlank() }
            val url = UrlUtil.resolveAssetUrl(attachment.url)
            when {
                name != null && url != null -> "$name - $url"
                name != null -> name
                url != null -> url
                else -> null
            }
        }
        val images = detail.images.orEmpty().mapNotNull { UrlUtil.resolveAssetUrl(it) }
        return NewsDetailUi(
            title = title,
            summary = summary,
            content = content,
            category = category,
            time = time,
            readTime = readTime,
            coverUrl = UrlUtil.resolveAssetUrl(detail.coverUrl),
            images = images,
            attachments = attachments
        )
    }

}

data class NewsDetailState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val detail: NewsDetailUi? = null
)

