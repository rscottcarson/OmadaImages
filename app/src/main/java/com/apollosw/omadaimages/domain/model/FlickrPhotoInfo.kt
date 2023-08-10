package com.apollosw.omadaimages.domain.model

data class FlickrPhotoInfo(
    val id: String,
    val secret: String,
    val server: String,
    val title: String,
    val ownerName: String,
    val description: String,
    val dateTaken: String,
    val originalFormat: String,
    val thumbnailURL: String? = null,
    val mediumURL: String? = null
) {
    val defaultURL = "https://live.staticflickr.com/$server/${id}_${secret}.jpg"
}