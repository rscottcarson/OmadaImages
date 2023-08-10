package com.apollosw.omadaimages.ui.imagesearch.state

sealed class PopupViewState {
    data class Open(val photoViewData: PhotoViewData) : PopupViewState()
    data object Closed : PopupViewState()
}