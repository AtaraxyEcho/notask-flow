package com.notaskflow.mq.producer;

import com.notaskflow.config.RabbitMqConfig;
import com.notaskflow.event.NotificationCreateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 通知事件消息生产者。
 *
 * @author LIN
 */
@Component
@RequiredArgsConstructor
public class NotificationEventProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送通知创建事件。
     *
     * @param event 通知创建事件
     */
    public void sendCreateEvent(NotificationCreateEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.NOTIFICATION_EXCHANGE,
                RabbitMqConfig.NOTIFICATION_CREATE_ROUTING_KEY,
                event
        );
    }
}
