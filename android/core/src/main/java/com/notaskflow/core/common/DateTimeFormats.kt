package com.notaskflow.core.common

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

const val DATE_TIME_PATTERN = "yyyy-MM-dd:HH:mm:ss"
const val DATE_PATTERN = "yyyy-MM-dd"

val notaskDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)

fun parseLocalDateTime(value: String): LocalDateTime? {
    val normalizedValue = value.trim()
    if (normalizedValue.isBlank()) {
        return null
    }
    INPUT_DATE_TIME_FORMATTERS.forEach { formatter ->
        parseDateTimeWithFormatter(normalizedValue, formatter)?.let { return it }
    }
    return parseDate(normalizedValue)?.atStartOfDay()
}

fun normalizeDateTimeInput(
    value: String,
    defaultTime: LocalTime = LocalTime.of(23, 59, 59)
): String? {
    val normalizedValue = value.trim()
    if (normalizedValue.isBlank()) {
        return null
    }
    INPUT_DATE_TIME_FORMATTERS.forEach { formatter ->
        parseDateTimeWithFormatter(normalizedValue, formatter)?.let { dateTime ->
            return formatLocalDateTime(dateTime)
        }
    }
    return parseDate(normalizedValue)?.atTime(defaultTime)?.let(::formatLocalDateTime)
}

fun formatLocalDateTime(value: LocalDateTime): String {
    return value.format(notaskDateTimeFormatter)
}

fun formatDateTimeText(value: String?): String {
    val normalizedValue = value?.trim().orEmpty()
    if (normalizedValue.isBlank()) {
        return ""
    }
    return parseLocalDateTime(normalizedValue)?.let(::formatLocalDateTime) ?: normalizedValue
}

private fun parseDateTimeWithFormatter(
    value: String,
    formatter: DateTimeFormatter
): LocalDateTime? {
    return try {
        LocalDateTime.parse(value, formatter)
    } catch (ignored: DateTimeParseException) {
        null
    }
}

private fun parseDate(value: String): LocalDate? {
    return try {
        LocalDate.parse(value, DateTimeFormatter.ofPattern(DATE_PATTERN))
    } catch (ignored: DateTimeParseException) {
        null
    }
}

private val INPUT_DATE_TIME_FORMATTERS = listOf(
    notaskDateTimeFormatter,
    DateTimeFormatter.ofPattern("yyyy-MM-dd:HH:mm"),
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
    DateTimeFormatter.ISO_LOCAL_DATE_TIME
)
