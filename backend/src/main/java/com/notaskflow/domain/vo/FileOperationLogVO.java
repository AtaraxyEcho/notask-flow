package com.notaskflow.domain.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件操作日志视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileOperationLogVO {

    private Long id;

    private Long fileId;

    private Long spaceId;

    private Long operatorId;

    private String operationType;

    private String detail;

    private LocalDateTime gmtCreate;
}
