package com.apollosw.omadaimages.domain.repo.api

import com.apollosw.omadaimages.domain.model.FlickrPhotoInfo
import com.apollosw.omadaimages.domain.repo.api.json.GetPhotosFlickerJSON
import com.apollosw.omadaimages.domain.repo.api.json.PhotoInfoFlickerJSON
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext

class HTTPFlickrAPI(
    private val client: HttpClient,
    private val coroutineScope: CoroutineScope
) : FlickrAPI {

    override suspend fun getRecent(
        pageIndex: Int,
        photosPerPage: Int
    ): ApiResponse<List<FlickrPhotoInfo>> = withContext(coroutineScope.coroutineContext) {
        val result = client.get("https://www.flickr.com/services/rest/") {
            url {
                parameters.append("method", "flickr.photos.getRecent")
                parameters.append("api_key", "a0222db495999c951dc33702500fdc4d")
                parameters.append("page", "$pageIndex")
                parameters.append("per_page", "$photosPerPage")
                parameters.append("format", "json")
                parameters.append("nojsoncallback", "1")
                parameters.append(
                    "extras",
                    "description,date_taken,owner_name,original_format,url_q,url_z"
                )
            }
        }

        when (result.status) {
            HttpStatusCode.OK -> {
                val photos: GetPhotosFlickerJSON = result.body()
                ApiResponse.Success(photos.photos.photo.map { it.toDomainPhotoInfo() })
            }

            else -> {
                ApiResponse.Error()
            }
        }
    }

    override suspend fun search(
        searchString: String,
        pageIndex: Int,
        photosPerPage: Int
    ): ApiResponse<List<FlickrPhotoInfo>> = withContext(coroutineScope.coroutineContext) {
        val result = client.get("https://www.flickr.com/services/rest/") {
            url {
                parameters.append("method", "flickr.photos.search")
                parameters.append("api_key", "a0222db495999c951dc33702500fdc4d")
                parameters.append("text", searchString)
                parameters.append("page", "$pageIndex")
                parameters.append("per_page", "$photosPerPage")
                parameters.append("format", "json")
                parameters.append("nojsoncallback", "1")
                parameters.append(
                    "extras",
                    "description,date_taken,owner_name,original_format,url_q,url_z"
                )
            }
        }

        when (result.status) {
            HttpStatusCode.OK -> {
                val photos: GetPhotosFlickerJSON = result.body()
                ApiResponse.Success(photos.photos.photo.map { it.toDomainPhotoInfo() })
            }

            else -> {
                ApiResponse.Error()
            }
        }
    }

    private fun PhotoInfoFlickerJSON.toDomainPhotoInfo(): FlickrPhotoInfo {
        return FlickrPhotoInfo(
            id = id,
            secret = secret,
            server = server,
            title = title,
            description = description?._content ?: "",
            ownerName = ownername ?: "",
            dateTaken = datetaken ?: "",
            originalFormat = originalformat ?: "",
            thumbnailURL = url_q,
            mediumURL = url_z
        )
    }
}