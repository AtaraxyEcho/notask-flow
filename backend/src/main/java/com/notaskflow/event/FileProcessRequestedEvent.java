package com.notaskflow.event;

import com.notaskflow.common.enums.FileProcessOperation;
import java.io.Serializable;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件异步处理请求事件。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
public class FileProcessRequestedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;

    private Long spaceId;

    private Long fileId;

    private Long attachmentId;

    private FileProcessOperation operation;

    /**
     * 创建文件异步处理请求事件。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @param attachmentId 附件标识
     * @param operation 文件处理操作
     */
    public FileProcessRequestedEvent(Long spaceId, Long fileId, Long attachmentId, FileProcessOperation operation) {
        this(UUID.randomUUID().toString(), spaceId, fileId, attachmentId, operation);
    }

    /**
     * 创建文件异步处理请求事件。
     *
     * @param eventId 事件标识
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @param attachmentId 附件标识
     * @param operation 文件处理操作
     */
    public FileProcessRequestedEvent(String eventId, Long spaceId, Long fileId, Long attachmentId,
                                     FileProcessOperation operation) {
        this.eventId = eventId == null || eventId.isBlank() ? UUID.randomUUID().toString() : eventId;
        this.spaceId = spaceId;
        this.fileId = fileId;
        this.attachmentId = attachmentId;
        this.operation = operation;
    }
}
