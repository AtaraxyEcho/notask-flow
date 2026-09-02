package com.notaskflow.mq.consumer;

import com.notaskflow.common.constant.RedisKeyConstants;
import com.notaskflow.common.enums.SearchIndexOperation;
import com.notaskflow.config.RabbitMqConfig;
import com.notaskflow.domain.entity.Note;
import com.notaskflow.event.SearchIndexRequestedEvent;
import com.notaskflow.mapper.NoteMapper;
import com.notaskflow.service.NoteSearchService;
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
 * 搜索索引事件消息消费者。
 *
 * @author LIN
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchIndexEventConsumer {

    private static final Duration EVENT_IDEMPOTENCY_TTL = Duration.ofDays(7);

    private final NoteMapper noteMapper;

    private final NoteSearchService noteSearchService;

    private final RedisUtil redisUtil;

    private final MqFailureRecorder mqFailureRecorder;

    /**
     * 消费搜索索引同步请求事件并手动确认消息。
     *
     * @param event 搜索索引同步请求事件
     * @param message 原始消息
     * @param channel RabbitMQ 信道
     * @throws IOException 消息确认失败时抛出
     */
    @RabbitListener(queues = RabbitMqConfig.SEARCH_INDEX_QUEUE)
    public void onSearchIndexRequested(SearchIndexRequestedEvent event, Message message, Channel channel)
            throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String eventKey = resolveEventKey(event.getEventId());
        boolean idempotencyMarked = false;
        try {
            if (eventKey != null) {
                idempotencyMarked = redisUtil.setIfAbsent(eventKey, "1", EVENT_IDEMPOTENCY_TTL);
            }
            if (eventKey != null && !idempotencyMarked) {
                log.info("搜索索引事件已消费，跳过重复消息，eventId={}", event.getEventId());
                channel.basicAck(deliveryTag, false);
                return;
            }
            syncIndex(event);
            channel.basicAck(deliveryTag, false);
        } catch (RuntimeException exception) {
            if (idempotencyMarked) {
                releaseIdempotencyMarker(eventKey);
            }
            log.error("搜索索引事件消费失败，noteId={}, operation={}",
                    event.getNoteId(), event.getOperation(), exception);
            mqFailureRecorder.recordConsumerFailure(MqFailureRecorder.SEARCH_INDEX_REQUESTED, event, exception);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void syncIndex(SearchIndexRequestedEvent event) {
        if (event.getNoteId() == null) {
            return;
        }
        SearchIndexOperation operation = event.getOperation();
        if (SearchIndexOperation.DELETE.equals(operation)) {
            noteSearchService.delete(event.getNoteId());
            return;
        }
        Note note = noteMapper.selectById(event.getNoteId());
        if (note == null) {
            noteSearchService.delete(event.getNoteId());
            return;
        }
        noteSearchService.index(note);
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
            log.warn("搜索索引事件幂等标记释放失败，eventKey={}", eventKey, exception);
        }
    }
}
