package com.notaskflow.mq.consumer;

import com.notaskflow.common.constant.RedisKeyConstants;
import com.notaskflow.common.enums.FileProcessOperation;
import com.notaskflow.config.RabbitMqConfig;
import com.notaskflow.domain.entity.Attachment;
import com.notaskflow.domain.entity.ManagedFile;
import com.notaskflow.event.FileProcessRequestedEvent;
import com.notaskflow.mapper.AttachmentMapper;
import com.notaskflow.mapper.ManagedFileMapper;
import com.notaskflow.service.FileSearchService;
import com.notaskflow.service.FileTextExtractionService;
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
 * 文件处理事件消息消费者。
 *
 * @author LIN
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileProcessEventConsumer {

    private static final Duration EVENT_IDEMPOTENCY_TTL = Duration.ofDays(7);

    private final RedisUtil redisUtil;

    private final ManagedFileMapper managedFileMapper;

    private final AttachmentMapper attachmentMapper;

    private final FileSearchService fileSearchService;

    private final FileTextExtractionService fileTextExtractionService;

    private final MqFailureRecorder mqFailureRecorder;

    /**
     * 消费文件处理请求事件并手动确认消息。
     *
     * @param event 文件处理请求事件
     * @param message 原始消息
     * @param channel RabbitMQ 信道
     * @throws IOException 消息确认失败时抛出
     */
    @RabbitListener(queues = RabbitMqConfig.FILE_PROCESS_QUEUE)
    public void onFileProcessRequested(FileProcessRequestedEvent event, Message message, Channel channel)
            throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String eventKey = resolveEventKey(event.getEventId());
        boolean idempotencyMarked = false;
        try {
            if (eventKey != null) {
                idempotencyMarked = redisUtil.setIfAbsent(eventKey, "1", EVENT_IDEMPOTENCY_TTL);
            }
            if (eventKey != null && !idempotencyMarked) {
                log.info("文件处理事件已消费，跳过重复消息，eventId={}", event.getEventId());
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("文件处理事件已接收，spaceId={}, fileId={}, attachmentId={}, operation={}",
                    event.getSpaceId(), event.getFileId(), event.getAttachmentId(), event.getOperation());
            syncFileSearchIndex(event);
            channel.basicAck(deliveryTag, false);
        } catch (RuntimeException exception) {
            if (idempotencyMarked) {
                releaseIdempotencyMarker(eventKey);
            }
            log.error("文件处理事件消费失败，spaceId={}, fileId={}, operation={}",
                    event.getSpaceId(), event.getFileId(), event.getOperation(), exception);
            mqFailureRecorder.recordConsumerFailure(MqFailureRecorder.FILE_PROCESS_REQUESTED, event, exception);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void syncFileSearchIndex(FileProcessRequestedEvent event) {
        if (event.getFileId() == null) {
            return;
        }
        FileProcessOperation operation = event.getOperation();
        if (FileProcessOperation.PHYSICAL_DELETED.equals(operation)
                || FileProcessOperation.TRASHED.equals(operation)) {
            fileSearchService.delete(event.getFileId());
            return;
        }
        ManagedFile managedFile = managedFileMapper.selectById(event.getFileId());
        if (managedFile == null) {
            fileSearchService.delete(event.getFileId());
            return;
        }
        Attachment attachment = attachmentMapper.selectById(managedFile.getAttachmentId());
        if (attachment == null) {
            fileSearchService.delete(event.getFileId());
            return;
        }
        String extractedText = fileTextExtractionService.extract(attachment);
        fileSearchService.index(managedFile, attachment, extractedText);
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
            log.warn("文件处理事件幂等标记释放失败，eventKey={}", eventKey, exception);
        }
    }
}
