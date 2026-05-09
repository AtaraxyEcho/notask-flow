package com.notaskflow.core.network

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.LocalDateTime

class LocalDateTimeJsonAdapter {
    @FromJson
    fun fromJson(value: String): LocalDateTime = LocalDateTime.parse(value)

    @ToJson
    fun toJson(value: LocalDateTime): String = value.toString()
}
