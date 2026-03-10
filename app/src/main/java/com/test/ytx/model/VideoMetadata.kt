package com.test.ytx.model

import com.squareup.moshi.Json

data class VideoMetadata(
    val title: String,
    @Json(name = "thumbnail") val thumbnail: String? = null,
    @Json(name = "author_name") val author: String? = "Unknown",
    val duration: String = "Unknown"
)
