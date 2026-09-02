package com.notaskflow.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端存储排行视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminStorageRankVO {

    private Long targetId;

    private String targetName;

    private String targetDescription;

    private Long fileCount;

    private Long storageBytes;
}
