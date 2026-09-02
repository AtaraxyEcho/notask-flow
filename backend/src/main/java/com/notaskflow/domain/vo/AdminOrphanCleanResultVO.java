package com.notaskflow.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端孤立文件清理结果视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrphanCleanResultVO {

    private Long cleanedCount;

    private Long cleanedBytes;

    private Long failedCount;
}
