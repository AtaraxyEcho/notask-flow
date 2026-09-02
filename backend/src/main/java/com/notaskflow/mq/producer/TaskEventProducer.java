package com.notaskflow.mq.producer;

import com.notaskflow.config.RabbitMqConfig;
import com.notaskflow.event.TaskMemberStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 任务事件消息生产者。
 *
 * @author LIN
 */
@Component
@RequiredArgsConstructor
public class TaskEventProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送任务成员状态变更事件。
     *
     * @param event 任务成员状态变更事件
     */
    public void sendMemberStatusChanged(TaskMemberStatusChangedEvent event) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.TASK_EXCHANGE, RabbitMqConfig.TASK_EVENT_ROUTING_KEY, event);
    }
}
