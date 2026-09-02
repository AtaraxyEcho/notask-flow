package com.notaskflow.core.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PageResponse<T>(
    @param:Json(name = "total") val total: Long,
    @param:Json(name = "pageNum") val pageNum: Long,
    @param:Json(name = "pageSize") val pageSize: Long,
    @param:Json(name = "list") val list: List<T>
)
