package com.workguard.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workguard.core.network.ApiResult
import com.workguard.core.network.NewsCategory
import com.workguard.core.network.NewsItem
import com.workguard.core.util.UrlUtil
import com.workguard.navigation.data.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {
    companion object {
        private const val QUERY_DEBOUNCE_MS = 400L
    }

    private val _state = MutableStateFlow(NewsState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        requestLoad(0L)
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        requestLoad(QUERY_DEBOUNCE_MS)
    }

    fun onCategorySelected(category: NewsCategoryUi) {
        _state.update { it.copy(selectedCategory = category) }
        requestLoad(0L)
    }

    fun refresh() {
        requestLoad(0L)
    }

    private fun requestLoad(delayMs: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (delayMs > 0) {
                delay(delayMs)
            }
            val snapshot = state.value
            val categoryId = snapshot.selectedCategory.id.takeUnless { snapshot.selectedCategory.isAll }
            val categorySlug = if (snapshot.selectedCategory.isAll || categoryId != null) {
                null
            } else {
                snapshot.selectedCategory.slug
            }
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (
                val result = newsRepository.loadNewsFeed(
                    query = snapshot.query.trim().takeIf { it.isNotBlank() },
                    categoryId = categoryId,
                    categorySlug = categorySlug,
                    page = 1,
                    limit = 20,
                    sort = "newest"
                )
            ) {
                is ApiResult.Success -> {
                    val categories = buildCategories(result.data.categories)
                    val selected = resolveSelectedCategory(snapshot.selectedCategory, categories)
                    val items = result.data.items.map { mapItem(it, limitReadTime = true) }
                    val featured = result.data.featured?.let { mapItem(it, limitReadTime = false) }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            categories = categories,
                            selectedCategory = selected,
                            featured = featured,
                            items = items
                        )
                    }
                }
                is ApiResult.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.throwable.message ?: "Gagal memuat berita"
                        )
                    }
                }
            }
        }
    }

    private fun buildCategories(categories: List<NewsCategory>): List<NewsCategoryUi> {
        val mapped = categories.mapNotNull { category ->
            val name = category.name?.trim().takeIf { !it.isNullOrBlank() } ?: return@mapNotNull null
            NewsCategoryUi(
                id = category.id,
                slug = category.slug,
                name = name,
                isAll = false
            )
        }.distinctBy { it.id ?: it.slug ?: it.name }
        return listOf(NewsCategoryUi.all()) + mapped
    }

    private fun resolveSelectedCategory(
        current: NewsCategoryUi,
        categories: List<NewsCategoryUi>
    ): NewsCategoryUi {
        if (current.isAll) {
            return categories.firstOrNull { it.isAll } ?: NewsCategoryUi.all()
        }
        return categories.firstOrNull { matchesCategory(it, current) }
            ?: categories.firstOrNull { it.isAll }
            ?: NewsCategoryUi.all()
    }

    private fun matchesCategory(a: NewsCategoryUi, b: NewsCategoryUi): Boolean {
        if (a.isAll || b.isAll) return false
        if (a.id != null && b.id != null && a.id == b.id) return true
        if (!a.slug.isNullOrBlank() && a.slug == b.slug) return true
        return a.name == b.name
    }

    private fun mapItem(item: NewsItem, limitReadTime: Boolean): NewsItemUi {
        val title = item.title?.takeIf { it.isNotBlank() } ?: "Tanpa judul"
        val summary = item.summary?.takeIf { it.isNotBlank() } ?: ""
        val category = item.category?.name?.takeIf { it.isNotBlank() } ?: "Umum"
        val time = NewsTimeFormatter.formatRelativeTime(item.publishedAt)
        val shouldHideReadTime = limitReadTime &&
            NewsTimeFormatter.isOlderThanDays(item.publishedAt, 6)
        val readTime = if (shouldHideReadTime) {
            ""
        } else {
            item.readTimeMinutes?.takeIf { it > 0 }?.let { "$it menit" } ?: ""
        }
        return NewsItemUi(
            id = item.id ?: -1,
            title = title,
            summary = summary,
            category = category,
            time = time,
            readTime = readTime,
            coverUrl = UrlUtil.resolveAssetUrl(item.coverUrl)
        )
    }
}

data class NewsState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val query: String = "",
    val categories: List<NewsCategoryUi> = listOf(NewsCategoryUi.all()),
    val selectedCategory: NewsCategoryUi = NewsCategoryUi.all(),
    val featured: NewsItemUi? = null,
    val items: List<NewsItemUi> = emptyList()
)

data class NewsCategoryUi(
    val id: Int? = null,
    val slug: String? = null,
    val name: String,
    val isAll: Boolean = false
) {
    companion object {
        fun all(): NewsCategoryUi = NewsCategoryUi(name = "Semua", isAll = true)
    }
}

data class NewsItemUi(
    val id: Int,
    val title: String,
    val summary: String,
    val category: String,
    val time: String,
    val readTime: String,
    val coverUrl: String? = null
)

