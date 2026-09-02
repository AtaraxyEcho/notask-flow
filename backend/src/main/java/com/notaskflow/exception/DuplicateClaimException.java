package com.notaskflow.exception;

/**
 * 重复认领异常。
 *
 * @author LIN
 */
public class DuplicateClaimException extends BusinessException {

    /**
     * 构造重复认领异常。
     *
     * @param message 异常描述
     */
    public DuplicateClaimException(String message) {
        super(ErrorCode.DUPLICATE_CLAIM, message);
    }
}
