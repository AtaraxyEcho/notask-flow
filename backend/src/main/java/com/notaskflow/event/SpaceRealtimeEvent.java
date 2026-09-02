package com.notaskflow.event;

import com.notaskflow.common.enums.SpaceRealtimeEventType;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 空间实时广播事件。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
public class SpaceRealtimeEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;

    private Long spaceId;

    private Long actorUserId;

    private SpaceRealtimeEventType type;

    private Map<String, Object> payload = new LinkedHashMap<>();

    private LocalDateTime occurredAt;

    /**
     * 创建空间实时广播事件。
     *
     * @param spaceId 空间标识
     * @param actorUserId 操作用户标识
     * @param type 事件类型
     * @param payload 事件载荷
     */
    public SpaceRealtimeEvent(Long spaceId,
                              Long actorUserId,
                              SpaceRealtimeEventType type,
                              Map<String, Object> payload) {
        this.eventId = UUID.randomUUID().toString();
        this.spaceId = spaceId;
        this.actorUserId = actorUserId;
        this.type = type;
        this.payload = payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
        this.occurredAt = LocalDateTime.now();
    }
}
