package com.notaskflow.mq.producer;

import com.notaskflow.config.RabbitMqConfig;
import com.notaskflow.event.FileProcessRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 文件处理事件消息生产者。
 *
 * @author LIN
 */
@Component
@RequiredArgsConstructor
public class FileProcessEventProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送文件处理请求事件。
     *
     * @param event 文件处理请求事件
     */
    public void sendRequestedEvent(FileProcessRequestedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.FILE_PROCESS_EXCHANGE,
                RabbitMqConfig.FILE_PROCESS_ROUTING_KEY,
                event
        );
    }
}
