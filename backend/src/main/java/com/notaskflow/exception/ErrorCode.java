package com.notaskflow.exception;

import lombok.Getter;

/**
 * 业务错误码定义。
 *
 * @author LIN
 */
@Getter
public enum ErrorCode {

    SUCCESS(200, "成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据冲突"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    TASK_STATUS_ILLEGAL(2001, "任务状态流转非法"),
    TASK_MEMBER_NOT_FOUND(2002, "任务成员不存在"),
    DUPLICATE_CLAIM(2003, "该职责已被认领"),
    TASK_ALREADY_COMPLETED(2004, "任务已完成，不可操作"),
    NOTEBOOK_NOT_EMPTY(3001, "笔记本非空，无法删除"),
    FILE_UPLOAD_FAILED(4001, "文件上传失败"),
    FILE_SIZE_EXCEEDED(4002, "文件大小超过限制"),
    SYSTEM_ERROR(500, "系统错误");

    private final Integer code;

    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
