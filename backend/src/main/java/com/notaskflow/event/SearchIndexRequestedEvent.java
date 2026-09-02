package com.notaskflow.event;

import com.notaskflow.common.enums.SearchIndexOperation;
import java.io.Serializable;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 搜索索引同步请求事件。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
public class SearchIndexRequestedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;

    private Long noteId;

    private SearchIndexOperation operation;

    /**
     * 创建搜索索引同步请求事件。
     *
     * @param noteId 笔记标识
     * @param operation 索引操作类型
     */
    public SearchIndexRequestedEvent(Long noteId, SearchIndexOperation operation) {
        this(UUID.randomUUID().toString(), noteId, operation);
    }

    /**
     * 创建搜索索引同步请求事件。
     *
     * @param eventId 事件标识
     * @param noteId 笔记标识
     * @param operation 索引操作类型
     */
    public SearchIndexRequestedEvent(String eventId, Long noteId, SearchIndexOperation operation) {
        this.eventId = eventId == null || eventId.isBlank() ? UUID.randomUUID().toString() : eventId;
        this.noteId = noteId;
        this.operation = operation;
    }
}
