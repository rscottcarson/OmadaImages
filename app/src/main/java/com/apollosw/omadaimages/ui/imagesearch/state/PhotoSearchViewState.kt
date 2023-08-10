package com.apollosw.omadaimages.ui.imagesearch.state

sealed class PhotoSearchViewState {
    data object Searching : PhotoSearchViewState()
    data object NotSearching : PhotoSearchViewState()
}