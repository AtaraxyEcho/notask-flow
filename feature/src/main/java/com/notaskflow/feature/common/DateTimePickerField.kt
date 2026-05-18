package com.notaskflow.feature.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.notaskflow.core.common.formatLocalDateTime
import com.notaskflow.core.common.parseLocalDateTime
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "选择日期和时间",
    supportingText: String? = "格式 yyyy-MM-dd:HH:mm:ss",
    enabled: Boolean = true
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val parsedDateTime = remember(value) { parseLocalDateTime(value) }

    TextField(
        value = value,
        onValueChange = {},
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                showDatePicker = true
            },
        enabled = enabled,
        readOnly = true,
        singleLine = true,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        supportingText = supportingText?.let { text ->
            {
                Text(text)
            }
        },
        trailingIcon = {
            Row {
                if (value.isNotBlank()) {
                    IconButton(
                        onClick = { onValueChange("") },
                        enabled = enabled
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "清除时间",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = { showDatePicker = true },
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = "选择时间"
                    )
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = parsedDateTime?.toLocalDate()?.toEpochMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis
                            ?.toLocalDate()
                            ?.let { date ->
                                selectedDate = date
                                showDatePicker = false
                                showTimePicker = true
                            }
                    }
                ) {
                    Text("下一步")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val initialTime = parsedDateTime?.toLocalTime() ?: DEFAULT_TIME
        val timePickerState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("选择时间") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val date = selectedDate
                        if (date != null) {
                            val time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                            onValueChange(formatLocalDateTime(LocalDateTime.of(date, time)))
                        }
                        showTimePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("取消")
                }
            }
        )
    }
}

private fun LocalDate.toEpochMillis(): Long {
    return atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}

private val DEFAULT_TIME = LocalTime.of(18, 0)
