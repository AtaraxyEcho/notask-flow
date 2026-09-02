package com.notaskflow.listener;

import com.notaskflow.config.CollaborationProperties;
import com.notaskflow.event.SpaceRealtimeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 空间实时事件监听器，负责事务提交后推送到协作 WebSocket 服务。
 *
 * @author LIN
 */
@Slf4j
@Component
public class SpaceRealtimeEventListener {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final CollaborationProperties collaborationProperties;

    private final RestTemplate restTemplate;

    /**
     * 创建空间实时事件监听器。
     *
     * @param collaborationProperties 协作配置
     * @param restTemplateBuilder HTTP 客户端构造器
     */
    public SpaceRealtimeEventListener(CollaborationProperties collaborationProperties,
                                      RestTemplateBuilder restTemplateBuilder) {
        this.collaborationProperties = collaborationProperties;
        this.restTemplate = restTemplateBuilder.build();
    }

    /**
     * 事务提交后推送空间实时事件。
     *
     * @param event 空间实时事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSpaceRealtimeEvent(SpaceRealtimeEvent event) {
        String broadcastUrl = collaborationProperties.getRealtimeBroadcastUrl();
        if (!StringUtils.hasText(broadcastUrl)) {
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(INTERNAL_TOKEN_HEADER, collaborationProperties.getInternalToken());
            restTemplate.postForEntity(broadcastUrl, new HttpEntity<>(event, headers), Void.class);
        } catch (RestClientException exception) {
            log.warn("空间实时事件推送失败，spaceId={}，eventType={}",
                    event.getSpaceId(), event.getType(), exception);
        }
    }
}
