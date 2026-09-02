package com.notaskflow.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 管理端操作日志实体。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("nt_admin_operation_log")
public class AdminOperationLog extends BaseEntity {

    private String operator;

    private String method;

    private String path;

    private String operationName;

    private String ipAddress;

    private String userAgent;

    private Boolean success;

    private String errorMessage;
}
