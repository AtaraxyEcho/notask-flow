package com.notaskflow.exception;

/**
 * 业务访问拒绝异常。
 *
 * @author LIN
 */
public class AccessDeniedException extends BusinessException {

    /**
     * 构造访问拒绝异常。
     *
     * @param message 异常描述
     */
    public AccessDeniedException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }
}
