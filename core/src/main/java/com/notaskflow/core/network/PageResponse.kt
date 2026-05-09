package com.notaskflow.core.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PageResponse<T>(
    @Json(name = "total") val total: Long,
    @Json(name = "pageNum") val pageNum: Long,
    @Json(name = "pageSize") val pageSize: Long,
    @Json(name = "list") val list: List<T>
)
