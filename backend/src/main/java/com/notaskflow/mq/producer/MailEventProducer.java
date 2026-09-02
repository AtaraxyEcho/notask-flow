package com.notaskflow.mq.producer;

import com.notaskflow.config.RabbitMqConfig;
import com.notaskflow.event.MailSendRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 邮件事件消息生产者。
 *
 * @author LIN
 */
@Component
@RequiredArgsConstructor
public class MailEventProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送邮件发送请求事件。
     *
     * @param event 邮件发送请求事件
     */
    public void sendRequestedEvent(MailSendRequestedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.MAIL_EXCHANGE,
                RabbitMqConfig.MAIL_SEND_ROUTING_KEY,
                event
        );
    }
}
