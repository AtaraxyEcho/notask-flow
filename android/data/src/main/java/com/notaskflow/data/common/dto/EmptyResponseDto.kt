package com.notaskflow.data.common.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EmptyResponseDto(
    val ignored: String? = null
)
