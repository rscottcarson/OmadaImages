package com.apollosw.omadaimages.domain.repo.api.json

import kotlinx.serialization.Serializable

@Serializable
data class GetRecentPhotosResponseDataFlickrJSON(
    val page: Int,
    val pages: Int,
    val perpage: Int,
    val total: Int,
    val photo: List<PhotoInfoFlickerJSON>,
    val stat: String? = null
)