package com.test.ytx.network

import com.test.ytx.model.VideoMetadata
import retrofit2.http.GET
import retrofit2.http.Query

interface YtxApiService {
    @GET("metadata")
    suspend fun getMetadata(@Query("url") url: String): VideoMetadata

    @GET("ping")
    suspend fun ping(): Map<String, String>
}
