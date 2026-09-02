package com.notaskflow.mq.consumer;

import com.notaskflow.common.constant.RedisKeyConstants;
import com.notaskflow.config.RabbitMqConfig;
import com.notaskflow.event.TaskMemberStatusChangedEvent;
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
 * 任务事件消息消费者。
 *
 * @author LIN
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskEventConsumer {

    private static final Duration EVENT_IDEMPOTENCY_TTL = Duration.ofDays(7);

    private final RedisUtil redisUtil;

    private final MqFailureRecorder mqFailureRecorder;

    /**
     * 消费任务成员状态变更事件并手动确认消息。
     *
     * @param event 任务成员状态变更事件
     * @param message 原始消息
     * @param channel RabbitMQ 信道
     * @throws IOException 确认消息失败时抛出
     */
    @RabbitListener(queues = RabbitMqConfig.TASK_EVENT_QUEUE)
    public void onTaskEvent(TaskMemberStatusChangedEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String eventKey = resolveEventKey(event.getEventId());
        boolean idempotencyMarked = false;
        try {
            if (eventKey != null) {
                idempotencyMarked = redisUtil.setIfAbsent(eventKey, "1", EVENT_IDEMPOTENCY_TTL);
            }
            if (eventKey != null && !idempotencyMarked) {
                log.info("任务事件已消费，跳过重复消息，eventId={}", event.getEventId());
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("收到任务成员状态事件。任务ID: {}, 成员ID: {}, 状态: {}", event.getTaskId(),
                    event.getTaskMemberId(), event.getTargetStatus());
            channel.basicAck(deliveryTag, false);
        } catch (RuntimeException exception) {
            if (idempotencyMarked) {
                releaseIdempotencyMarker(eventKey);
            }
            log.error("任务事件消费失败。任务ID: {}, 成员ID: {}", event.getTaskId(), event.getTaskMemberId(), exception);
            mqFailureRecorder.recordConsumerFailure(MqFailureRecorder.TASK_MEMBER_STATUS_CHANGED, event, exception);
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
            log.warn("任务事件幂等标记释放失败，eventKey={}", eventKey, exception);
        }
    }
}
