package com.notaskflow.data.common

import com.notaskflow.core.network.PageResponse
import com.notaskflow.domain.model.Page

fun <T, R> PageResponse<T>.toDomain(mapper: (T) -> R): Page<R> {
    return Page(
        total = total,
        pageNum = pageNum,
        pageSize = pageSize,
        list = list.map(mapper)
    )
}
