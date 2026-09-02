package com.notaskflow.listener;

import com.notaskflow.event.FileProcessRequestedEvent;
import com.notaskflow.mq.producer.FileProcessEventProducer;
import com.notaskflow.service.EventFailLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 文件处理领域事件监听器，负责在业务事务提交后投递 MQ。
 *
 * @author LIN
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileProcessEventListener {

    private final FileProcessEventProducer fileProcessEventProducer;

    private final EventFailLogService eventFailLogService;

    /**
     * 事务提交后投递文件处理请求事件。
     *
     * @param event 文件处理请求事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFileProcessRequested(FileProcessRequestedEvent event) {
        try {
            fileProcessEventProducer.sendRequestedEvent(event);
        } catch (AmqpException exception) {
            log.error("文件处理事件投递失败，spaceId={}, fileId={}, operation={}",
                    event.getSpaceId(), event.getFileId(), event.getOperation(), exception);
            eventFailLogService.recordFailure("FILE_PROCESS_REQUESTED", event, exception.getMessage());
        }
    }
}
