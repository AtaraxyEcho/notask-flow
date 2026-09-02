package com.notaskflow.listener;

import com.notaskflow.event.SearchIndexRequestedEvent;
import com.notaskflow.mq.producer.SearchIndexEventProducer;
import com.notaskflow.service.EventFailLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 搜索索引领域事件监听器，负责在业务事务提交后投递 MQ。
 *
 * @author LIN
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchIndexEventListener {

    private final SearchIndexEventProducer searchIndexEventProducer;

    private final EventFailLogService eventFailLogService;

    /**
     * 事务提交后投递搜索索引同步请求事件。
     *
     * @param event 搜索索引同步请求事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSearchIndexRequested(SearchIndexRequestedEvent event) {
        try {
            searchIndexEventProducer.sendRequestedEvent(event);
        } catch (AmqpException exception) {
            log.error("搜索索引事件投递失败，noteId={}, operation={}",
                    event.getNoteId(), event.getOperation(), exception);
            eventFailLogService.recordFailure("SEARCH_INDEX_REQUESTED", event, exception.getMessage());
        }
    }
}
