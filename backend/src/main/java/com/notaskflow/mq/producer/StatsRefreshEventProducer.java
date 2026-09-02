package com.notaskflow.mq.producer;

import com.notaskflow.config.RabbitMqConfig;
import com.notaskflow.event.StatsRefreshRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 统计刷新事件消息生产者。
 *
 * @author LIN
 */
@Component
@RequiredArgsConstructor
public class StatsRefreshEventProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送统计刷新请求事件。
     *
     * @param event 统计刷新请求事件
     */
    public void sendRequestedEvent(StatsRefreshRequestedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.STATS_EXCHANGE,
                RabbitMqConfig.STATS_REFRESH_ROUTING_KEY,
                event
        );
    }
}
