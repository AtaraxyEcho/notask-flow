package com.notaskflow.exception;

import lombok.Getter;

/**
 * 基础业务异常。
 *
 * @author LIN
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    private final String message;

    /**
     * 根据错误码构造异常。
     *
     * @param errorCode 错误码
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    /**
     * 根据错误码和自定义描述构造异常。
     *
     * @param errorCode 错误码
     * @param customMessage 自定义描述
     */
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.code = errorCode.getCode();
        this.message = customMessage;
    }
}
