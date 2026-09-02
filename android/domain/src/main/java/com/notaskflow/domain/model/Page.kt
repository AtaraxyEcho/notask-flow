package com.notaskflow.domain.model

data class Page<T>(
    val total: Long,
    val pageNum: Long,
    val pageSize: Long,
    val list: List<T>
)
