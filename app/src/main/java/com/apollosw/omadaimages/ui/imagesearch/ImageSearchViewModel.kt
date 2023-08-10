package com.apollosw.omadaimages.ui.imagesearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollosw.omadaimages.domain.model.FlickrPhotoInfo
import com.apollosw.omadaimages.domain.repo.api.ApiResponse
import com.apollosw.omadaimages.domain.repo.api.FlickrAPI
import com.apollosw.omadaimages.ui.imagesearch.state.PhotoGridViewState
import com.apollosw.omadaimages.ui.imagesearch.state.PhotoSearchViewState
import com.apollosw.omadaimages.ui.imagesearch.state.PhotoViewData
import com.apollosw.omadaimages.ui.imagesearch.state.PopupViewState
import com.apollosw.omadaimages.ui.util.SimplePager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageSearchViewModel @Inject constructor(
    private val flickrAPI: FlickrAPI
) : ViewModel() {

    private val _photoGridViewState: MutableSharedFlow<PhotoGridViewState> =
        MutableSharedFlow()
    val photoGridViewState: StateFlow<PhotoGridViewState> =
        _photoGridViewState.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            PhotoGridViewState.Empty
        )

    private val _popupViewState: MutableSharedFlow<PopupViewState> =
        MutableSharedFlow()
    val popupViewState: StateFlow<PopupViewState> =
        _popupViewState.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            PopupViewState.Closed
        )

    private val _photoSearchViewState: MutableSharedFlow<PhotoSearchViewState> =
        MutableSharedFlow()
    val photoSearchViewState: StateFlow<PhotoSearchViewState> =
        _photoSearchViewState.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            PhotoSearchViewState.NotSearching
        )

    private val _firstVisibleScrollItemIndex = MutableSharedFlow<Int>()

    private var pagerJob: Job? = null
    private val pager: SimplePager<FlickrPhotoInfo> =
        SimplePager(
            _firstVisibleScrollItemIndex,
            pageSize = 120
        )

    fun onSearchClick(searchString: String?) {
        viewModelScope.launch {
            _photoGridViewState.emit(PhotoGridViewState.Loading)
            _photoSearchViewState.emit(PhotoSearchViewState.Searching)
        }
        when (searchString.isNullOrBlank()) {
            true -> {
                getPhotos { page, pageSize ->
                    when (val apiResult = flickrAPI.getRecent(page, pageSize)) {
                        is ApiResponse.Error -> SimplePager.PagingOperation.Error()
                        is ApiResponse.Success -> {
                            SimplePager.PagingOperation.Success(apiResult.data)
                        }
                    }
                }
            }

            else -> {
                getPhotos { page, pageSize ->
                    when (
                        val apiResult =
                            flickrAPI.search(searchString = searchString, page, pageSize)
                    ) {
                        is ApiResponse.Error -> SimplePager.PagingOperation.Error()
                        is ApiResponse.Success -> {
                            SimplePager.PagingOperation.Success(apiResult.data)
                        }
                    }
                }
            }
        }
    }

    fun onScrollVisibleItemChanged(firstVisibleItemIndex: Int) {
        viewModelScope.launch {
            _firstVisibleScrollItemIndex.emit(firstVisibleItemIndex)
        }
    }

    fun onPhotoClick(photoViewData: PhotoViewData) {
        viewModelScope.launch {
            _popupViewState.emit(PopupViewState.Open(photoViewData))
        }
    }

    fun onPopupDismiss() {
        viewModelScope.launch {
            _popupViewState.emit(PopupViewState.Closed)
        }
    }

    private fun getPhotos(
        apiCall: suspend (Int, Int) -> SimplePager.PagingOperation<List<FlickrPhotoInfo>>
    ) {
        pagerJob?.cancel()
        pagerJob = viewModelScope.launch {
            pager.pageWith(apiCall).collect {
                _photoSearchViewState.emit(PhotoSearchViewState.NotSearching)
                _photoGridViewState.emit(
                        when(it) {
                            is SimplePager.Page.Error -> {
                                PhotoGridViewState.Error
                            }
                            is SimplePager.Page.Success -> {
                                PhotoGridViewState.Loaded(
                                    it.data.map { photoInfo ->
                                        PhotoViewData(
                                            title = photoInfo.title,
                                            description = photoInfo.description,
                                            owner = photoInfo.ownerName,
                                            dateTaken = photoInfo.dateTaken,
                                            originalFormat = photoInfo.originalFormat,
                                            thumbnailURL = photoInfo.thumbnailURL ?: photoInfo.defaultURL,
                                            popupURL = photoInfo.mediumURL ?: photoInfo.defaultURL
                                        )
                                    }
                                )
                            }
                        }
                )
            }
        }
    }
}