package com.apollosw.omadaimages.ui.state

sealed class PhotoSearchViewState {
    data object Searching : PhotoSearchViewState()
    data object NotSearching : PhotoSearchViewState()
}