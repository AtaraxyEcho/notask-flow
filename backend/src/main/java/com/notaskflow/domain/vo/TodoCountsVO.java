package com.notaskflow.domain.vo;

/**
 * 待办数量统计视图对象。
 *
 * @author LIN
 */
public record TodoCountsVO(Long totalCount, Long pendingCount, Long completedCount) {
}
