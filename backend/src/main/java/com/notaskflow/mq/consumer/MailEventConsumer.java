package com.notaskflow.mq.consumer;

import com.notaskflow.common.constant.RedisKeyConstants;
import com.notaskflow.config.RabbitMqConfig;
import com.notaskflow.event.MailSendRequestedEvent;
import com.notaskflow.service.MailNotificationService;
import com.notaskflow.utils.RedisUtil;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 邮件发送事件消息消费者。
 *
 * @author LIN
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailEventConsumer {

    private static final Duration EVENT_IDEMPOTENCY_TTL = Duration.ofDays(7);

    private final MailNotificationService mailNotificationService;

    private final RedisUtil redisUtil;

    private final MqFailureRecorder mqFailureRecorder;

    /**
     * 消费邮件发送请求事件并手动确认消息。
     *
     * @param event 邮件发送请求事件
     * @param message 原始消息
     * @param channel RabbitMQ 信道
     * @throws IOException 消息确认失败时抛出
     */
    @RabbitListener(queues = RabbitMqConfig.MAIL_SEND_QUEUE)
    public void onMailSendRequested(MailSendRequestedEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String eventKey = resolveEventKey(event.getEventId());
        boolean idempotencyMarked = false;
        try {
            if (eventKey != null) {
                idempotencyMarked = redisUtil.setIfAbsent(eventKey, "1", EVENT_IDEMPOTENCY_TTL);
            }
            if (eventKey != null && !idempotencyMarked) {
                log.info("邮件发送事件已消费，跳过重复消息，eventId={}", event.getEventId());
                channel.basicAck(deliveryTag, false);
                return;
            }
            boolean sentOrSkipped = mailNotificationService.sendIfNecessary(
                    event.getUserId(),
                    event.getType(),
                    event.getBusinessType(),
                    event.getTitle(),
                    event.getContent()
            );
            if (!sentOrSkipped) {
                throw new IllegalStateException("邮件通知发送失败");
            }
            channel.basicAck(deliveryTag, false);
        } catch (RuntimeException exception) {
            if (idempotencyMarked) {
                releaseIdempotencyMarker(eventKey);
            }
            log.error("邮件发送事件消费失败，userId={}, type={}, businessType={}",
                    event.getUserId(), event.getType(), event.getBusinessType(), exception);
            mqFailureRecorder.recordConsumerFailure(MqFailureRecorder.MAIL_SEND_REQUESTED, event, exception);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private String resolveEventKey(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return null;
        }
        return RedisKeyConstants.mqConsumedEvent(eventId);
    }

    private void releaseIdempotencyMarker(String eventKey) {
        try {
            redisUtil.delete(eventKey);
        } catch (RuntimeException exception) {
            log.warn("邮件发送事件幂等标记释放失败，eventKey={}", eventKey, exception);
        }
    }
}
