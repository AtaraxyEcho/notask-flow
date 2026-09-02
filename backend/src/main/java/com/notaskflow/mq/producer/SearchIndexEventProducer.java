package com.notaskflow.mq.producer;

import com.notaskflow.config.RabbitMqConfig;
import com.notaskflow.event.SearchIndexRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 搜索索引事件消息生产者。
 *
 * @author LIN
 */
@Component
@RequiredArgsConstructor
public class SearchIndexEventProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送搜索索引同步请求事件。
     *
     * @param event 搜索索引同步请求事件
     */
    public void sendRequestedEvent(SearchIndexRequestedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.SEARCH_INDEX_EXCHANGE,
                RabbitMqConfig.SEARCH_INDEX_ROUTING_KEY,
                event
        );
    }
}
