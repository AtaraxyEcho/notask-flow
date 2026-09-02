package com.notaskflow.exception;

/**
 * 任务状态流转非法异常。
 *
 * @author LIN
 */
public class IllegalTaskStateException extends BusinessException {

    /**
     * 构造任务状态流转异常。
     *
     * @param message 异常描述
     */
    public IllegalTaskStateException(String message) {
        super(ErrorCode.TASK_STATUS_ILLEGAL, message);
    }
}
