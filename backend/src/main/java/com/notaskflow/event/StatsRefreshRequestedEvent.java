package com.notaskflow.event;

import com.notaskflow.common.enums.StatsRefreshScope;
import java.io.Serializable;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统计刷新请求事件。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
public class StatsRefreshRequestedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;

    private Long spaceId;

    private StatsRefreshScope scope;

    /**
     * 创建统计刷新请求事件。
     *
     * @param spaceId 空间标识
     * @param scope 刷新范围
     */
    public StatsRefreshRequestedEvent(Long spaceId, StatsRefreshScope scope) {
        this(UUID.randomUUID().toString(), spaceId, scope);
    }

    /**
     * 创建统计刷新请求事件。
     *
     * @param eventId 事件标识
     * @param spaceId 空间标识
     * @param scope 刷新范围
     */
    public StatsRefreshRequestedEvent(String eventId, Long spaceId, StatsRefreshScope scope) {
        this.eventId = eventId == null || eventId.isBlank() ? UUID.randomUUID().toString() : eventId;
        this.spaceId = spaceId;
        this.scope = scope;
    }
}
