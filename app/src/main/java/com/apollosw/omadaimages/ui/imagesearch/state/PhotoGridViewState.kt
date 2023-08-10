package com.apollosw.omadaimages.ui.imagesearch.state

sealed class PhotoGridViewState {
    data object Empty : PhotoGridViewState()
    data object Loading : PhotoGridViewState()
    data class Loaded(val photos: List<PhotoViewData>) : PhotoGridViewState()
    data object Error : PhotoGridViewState()
}