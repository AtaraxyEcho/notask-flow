package com.notaskflow.service;

import com.notaskflow.common.enums.SpaceRealtimeEventType;
import java.util.Map;

/**
 * 空间实时事件服务。
 *
 * @author LIN
 */
public interface SpaceRealtimeEventService {

    /**
     * 发布空间实时事件。
     *
     * @param spaceId 空间标识
     * @param type 事件类型
     * @param payload 事件载荷
     */
    void publish(Long spaceId, SpaceRealtimeEventType type, Map<String, Object> payload);

    /**
     * 以系统身份发布空间实时事件。
     *
     * @param spaceId 空间标识
     * @param type 事件类型
     * @param payload 事件载荷
     */
    void publishSystem(Long spaceId, SpaceRealtimeEventType type, Map<String, Object> payload);
}
