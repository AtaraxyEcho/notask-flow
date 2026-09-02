package com.notaskflow.exception;

/**
 * 任务成员不存在异常。
 *
 * @author LIN
 */
public class TaskMemberNotFoundException extends BusinessException {

    /**
     * 构造任务成员不存在异常。
     *
     * @param message 异常描述
     */
    public TaskMemberNotFoundException(String message) {
        super(ErrorCode.TASK_MEMBER_NOT_FOUND, message);
    }
}
