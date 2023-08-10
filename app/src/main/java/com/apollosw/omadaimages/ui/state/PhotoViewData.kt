package com.apollosw.omadaimages.ui.state

data class PhotoViewData (
    val title: String,
    val description: String,
    val owner: String,
    val dateTaken: String,
    val originalFormat: String,
    val thumbnailURL: String,
    val popupURL: String
)
