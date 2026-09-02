package com.notaskflow.exception;

/**
 * 资源不存在异常。
 *
 * @author LIN
 */
public class ResourceNotFoundException extends BusinessException {

    /**
     * 构造资源不存在异常。
     *
     * @param message 异常描述
     */
    public ResourceNotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
