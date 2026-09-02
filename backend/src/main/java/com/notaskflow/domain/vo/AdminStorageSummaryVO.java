package com.notaskflow.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端存储汇总视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminStorageSummaryVO {

    private Long totalFileCount;

    private Long totalStorageBytes;

    private Long orphanFileCount;

    private Long orphanStorageBytes;

    private Long deletedFileCount;

    private Long deletedStorageBytes;
}
