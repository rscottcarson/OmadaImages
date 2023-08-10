package com.apollosw.omadaimages.domain.repo.api.json

import kotlinx.serialization.Serializable

@Serializable
data class PhotoInfoFlickerJSON (
    val id: String,
    val secret: String,
    val server: String,
    val farm: Int,
    val title: String,
    val description: DescriptionJSON? = null,
    val ownername: String? = null,
    val datetaken: String? = null,
    val originalformat: String? = null,
    val url_q: String? = null,
    val url_z: String? = null
)