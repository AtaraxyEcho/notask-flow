package com.notaskflow.domain.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端操作日志视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOperationLogVO {

    private Long id;

    private String operator;

    private String method;

    private String path;

    private String operationName;

    private String ipAddress;

    private String userAgent;

    private Boolean success;

    private String errorMessage;

    private LocalDateTime gmtCreate;
}
