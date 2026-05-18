package com.notaskflow.core.network

import com.notaskflow.core.common.formatLocalDateTime
import com.notaskflow.core.common.parseLocalDateTime
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.LocalDateTime

class LocalDateTimeJsonAdapter {
    @FromJson
    fun fromJson(value: String): LocalDateTime {
        return parseLocalDateTime(value) ?: throw JsonDataException("时间格式不合法：$value")
    }

    @ToJson
    fun toJson(value: LocalDateTime): String = formatLocalDateTime(value)
}
