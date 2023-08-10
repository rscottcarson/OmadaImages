package com.apollosw.omadaimages.domain.repo.api.json

import kotlinx.serialization.Serializable

@Serializable
data class GetPhotosFlickerJSON (
    val photos: GetRecentPhotosResponseDataFlickrJSON
)