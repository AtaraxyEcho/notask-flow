package com.notaskflow.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 通用分页查询参数。
 *
 * @author LIN
 */
@Data
public class PageQuery {

    @Min(value = 1, message = "页码必须从1开始")
    private Long pageNum = 1L;

    @Min(value = 1, message = "每页条数必须大于0")
    @Max(value = 100, message = "每页条数不能超过100")
    private Long pageSize = 10L;

    /**
     * 返回安全页码。
     *
     * @return 页码
     */
    public Long safePageNum() {
        if (pageNum == null || pageNum < 1) {
            return 1L;
        }
        return pageNum;
    }

    /**
     * 返回安全分页大小。
     *
     * @return 分页大小
     */
    public Long safePageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        if (pageSize > 100) {
            return 100L;
        }
        return pageSize;
    }
}
