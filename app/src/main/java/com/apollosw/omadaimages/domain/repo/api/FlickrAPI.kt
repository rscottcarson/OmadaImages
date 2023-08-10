package com.apollosw.omadaimages.domain.repo.api

import com.apollosw.omadaimages.domain.model.FlickrPhotoInfo

interface FlickrAPI {

    suspend fun getRecent(
        pageIndex: Int,
        photosPerPage: Int
    ) : ApiResponse<List<FlickrPhotoInfo>>

    suspend fun search(
        searchString: String,
        pageIndex: Int,
        photosPerPage: Int
    ) : ApiResponse<List<FlickrPhotoInfo>>

}