package com.apollosw.omadaimages.ui.util

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
/**
 * A basic, generic pager that caches pages of data
 *
 * @param T the type of item being paged through
 * @property scrollVisibleItemIndex a flow that emits the most recent visible item index in a
 * listview or gridview to help determine when to fetch new data
 * @property pageSize the number of elements to fetch per page
 * @property cachedPageLimit the number of pages to keep cached in memory
 */
class SimplePager<T>(
    private val scrollVisibleItemIndex: Flow<Int>,
    private val pageSize: Int = DEFAULT_PAGE_SIZE,
    private val cachedPageLimit: Int = DEFAULT_CACHED_PAGE_LIMIT
) {
    private var currentDataPage: DataPage<T>? = null
    private var firstCachedDataPage: DataPage<T>? = null
    private var lastCachedDataPage: DataPage<T>? = null
    private var previousVisibleItemIndex = 0

    private lateinit var page: suspend (pageIndex: Int, elementsPerPage: Int) -> PagingOperation<List<T>>
    private val coroutineScope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO)

    private val _itemFlow: MutableSharedFlow<Page<List<T>>> = MutableSharedFlow(replay = 1)

    init {
        coroutineScope.launch {
            scrollVisibleItemIndex.debounce(500.milliseconds).collect {
                pageIfNeeded(it)
            }
        }
    }

    /**
     * Start paging from the first page
     *
     * @param loadPage a function that defines how the pager should fetch new data
     *
     *@return A flow that emits a Page on new data being fetched from memory or using [loadPage]
     */
    suspend fun pageWith(
        loadPage: suspend (pageIndex: Int, elementsPerPage: Int) -> PagingOperation<List<T>>
    ): Flow<Page<List<T>>> {
        currentDataPage = null
        firstCachedDataPage = null
        lastCachedDataPage = null
        page = loadPage
        nextPage()
        return _itemFlow
    }

    private suspend fun nextPage() {
        val currentIndex = currentDataPage?.pageNumber ?: 0
        val lastIndex = lastCachedDataPage?.pageNumber ?: 0

        when {
            currentIndex == 0 -> {
                // No pages, get first page of data
                when (val pagingResult = page(1, pageSize)) {
                    is PagingOperation.Success -> {
                        DataPage(
                            1,
                            pagingResult.data,
                            nextDataPage = null,
                            previousDataPage = null
                        ).also {
                            currentDataPage = it
                            firstCachedDataPage = it
                            lastCachedDataPage = it
                            _itemFlow.emit(Page.Success(it.getPagedData()))
                        }
                    }
                    else -> _itemFlow.emit(Page.Error())
                }
            }

            currentIndex < lastIndex -> { // Next page of data is cached, return it
                _itemFlow.emit(
                    currentDataPage?.nextDataPage?.let {
                        currentDataPage = it
                        Page.Success(it.getPagedData())
                    } ?: Page.Error()
                )
            }

            currentIndex == lastIndex && lastIndex < MAX_NUM_PAGES -> {
                // Need to fetch next page of data
                Log.d("*** Pager", "need next page")
                currentDataPage?.let {
                    val nextPageIndex = it.pageNumber + 1
                    when (val pageResult = page(nextPageIndex, pageSize)) {
                        is PagingOperation.Success -> {
                            it.nextDataPage = DataPage(
                                nextPageIndex,
                                pageResult.data,
                                nextDataPage = null,
                                previousDataPage = it
                            )
                            _itemFlow.emit(
                                Page.Success(it.getPagedData())
                            )
                            currentDataPage = currentDataPage?.nextDataPage
                            lastCachedDataPage = currentDataPage

                        }
                        else -> _itemFlow.emit(Page.Error())
                    }
                }
            }
        }
        validateCache()
    }

    private suspend fun previousPage() {
        Log.d("*** Pager", "Page previous from ${currentDataPage?.pageNumber}")
        val currentIndex = currentDataPage?.pageNumber ?: 0
        val firstIndex = firstCachedDataPage?.pageNumber ?: 0

        // No pages, get first page of data
        when {
            currentIndex == 0 -> {
                when (val pagingResult = page(1, pageSize)) {
                    is PagingOperation.Success -> {
                        DataPage(
                            1,
                            pagingResult.data,
                            nextDataPage = null,
                            previousDataPage = null
                        ).also {
                            currentDataPage = it
                            firstCachedDataPage = it
                            lastCachedDataPage = it
                            _itemFlow.emit(Page.Success(it.getPagedData()))
                        }
                    }

                    else -> _itemFlow.emit(Page.Error())
                }
            }

            currentIndex > firstIndex -> {
                // Previous page of data is cached, return it
                _itemFlow.emit(
                    currentDataPage?.previousDataPage?.let {
                        currentDataPage = it
                        Page.Success(it.getPagedData())
                    } ?: Page.Error()
                )
            }

            currentIndex == firstIndex && firstIndex > 1 -> {
                // Need to fetch previous page of data
                currentDataPage?.let {
                    val previousPageIndex = it.pageNumber - 1
                    when (val pageResult = page(previousPageIndex, pageSize)) {
                        is PagingOperation.Success -> {
                            it.nextDataPage = DataPage(
                                previousPageIndex,
                                pageResult.data,
                                nextDataPage = it,
                                previousDataPage = null
                            )
                            _itemFlow.emit(
                                Page.Success(it.getPagedData())
                            )
                            currentDataPage = currentDataPage?.previousDataPage
                            firstCachedDataPage = currentDataPage
                        }
                        else -> _itemFlow.emit(Page.Error())
                    }
                }
            }
        }
        validateCache()
    }

    private suspend fun pageIfNeeded(visibleElementIndex: Int) {
        val currentIndex = currentDataPage?.pageNumber ?: 0
        val firstIndex = firstCachedDataPage?.pageNumber ?: 0
        val lastIndex = lastCachedDataPage?.pageNumber ?: 0
        val maxElements = when (currentIndex) {
            1 -> pageSize.toDouble()
            2 -> pageSize * 2.toDouble()
            lastIndex -> pageSize * 2.toDouble()
            firstIndex -> pageSize * 2.toDouble()
            else -> pageSize * 3.toDouble()
        }

        val isScrollingNext = (visibleElementIndex - previousVisibleItemIndex) >= 0

        val percentageScrolled = visibleElementIndex.toDouble() / maxElements

        Log.d(
            "*** Pager",
            "percentage: $percentageScrolled, current - $currentIndex, first - $firstIndex"
        )
        if (percentageScrolled > 0.8f && isScrollingNext) nextPage()
        else if (percentageScrolled < 0.2f && !isScrollingNext) previousPage()

        previousVisibleItemIndex = visibleElementIndex
    }

    private fun validateCache() {
        val currentIndex = currentDataPage?.pageNumber ?: 0
        val lastIndex = lastCachedDataPage?.pageNumber ?: 0
        val firstIndex = firstCachedDataPage?.pageNumber ?: 0

        // Check range
        if (lastIndex - firstIndex > cachedPageLimit) {
            // we need to drop a page
            if ((lastIndex - currentIndex) > (currentIndex - firstIndex)) {
                // Drop last page
                lastCachedDataPage = lastCachedDataPage?.previousDataPage
                lastCachedDataPage?.nextDataPage = null
            } else {
                // Drop first page
                firstCachedDataPage = firstCachedDataPage?.nextDataPage
                firstCachedDataPage?.previousDataPage = null
            }
        }
    }

    private data class DataPage<T>(
        var pageNumber: Int,
        val data: List<T>,
        var nextDataPage: DataPage<T>?,
        var previousDataPage: DataPage<T>?
    ) {
        fun getPagedData(): List<T> {
            val dataList: MutableList<T> = mutableListOf()
            previousDataPage?.let { dataList.addAll(it.data) }
            dataList.addAll(data)
            nextDataPage?.let { dataList.addAll(it.data) }
            return dataList
        }
    }

    /**
     * Encapsulates the result of [page] when fetching new data
     */
    sealed class PagingOperation<T> {
        class Error<T> : PagingOperation<T>()
        class Success<T>(val data: T) : PagingOperation<T>()
    }

    /**
     * Basic wrapper around newly paged data
     */
    sealed class Page<T> {
        class Error<T> : Page<T>()
        class Success<T>(val data: T) : Page<T>()
    }

    companion object {
        const val MAX_NUM_PAGES = 999
        const val DEFAULT_CACHED_PAGE_LIMIT = 10
        const val DEFAULT_PAGE_SIZE = 100
    }
}