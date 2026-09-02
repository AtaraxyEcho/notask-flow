package com.notaskflow.service.impl;

import com.notaskflow.common.enums.SpaceRealtimeEventType;
import com.notaskflow.event.SpaceRealtimeEvent;
import com.notaskflow.service.SpaceRealtimeEventService;
import com.notaskflow.utils.LoginUserUtil;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * 空间实时事件服务实现。
 *
 * @author LIN
 */
@Service
@RequiredArgsConstructor
public class SpaceRealtimeEventServiceImpl implements SpaceRealtimeEventService {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 发布空间实时事件。
     *
     * @param spaceId 空间标识
     * @param type 事件类型
     * @param payload 事件载荷
     */
    @Override
    public void publish(Long spaceId, SpaceRealtimeEventType type, Map<String, Object> payload) {
        Long actorUserId = LoginUserUtil.currentUserId();
        publishWithActor(spaceId, actorUserId, type, payload);
    }

    /**
     * 以系统身份发布空间实时事件。
     *
     * @param spaceId 空间标识
     * @param type 事件类型
     * @param payload 事件载荷
     */
    @Override
    public void publishSystem(Long spaceId, SpaceRealtimeEventType type, Map<String, Object> payload) {
        publishWithActor(spaceId, null, type, payload);
    }

    /**
     * 发布指定操作者的空间实时事件。
     *
     * @param spaceId 空间标识
     * @param actorUserId 操作者标识
     * @param type 事件类型
     * @param payload 事件载荷
     */
    private void publishWithActor(Long spaceId, Long actorUserId, SpaceRealtimeEventType type,
                                  Map<String, Object> payload) {
        applicationEventPublisher.publishEvent(new SpaceRealtimeEvent(spaceId, actorUserId, type, payload));
    }
}
