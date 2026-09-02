package com.notaskflow.exception;

/**
 * 文件存储异常。
 *
 * @author LIN
 */
public class FileStorageException extends BusinessException {

    /**
     * 构造文件存储异常。
     *
     * @param message 异常描述
     */
    public FileStorageException(String message) {
        super(ErrorCode.FILE_UPLOAD_FAILED, message);
    }

    /**
     * 构造文件存储异常。
     *
     * @param message 异常描述
     * @param cause 原始异常
     */
    public FileStorageException(String message, Throwable cause) {
        super(ErrorCode.FILE_UPLOAD_FAILED, message);
        initCause(cause);
    }
}
