package com.notaskflow.listener;

import com.notaskflow.event.StatsRefreshRequestedEvent;
import com.notaskflow.mq.producer.StatsRefreshEventProducer;
import com.notaskflow.service.EventFailLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 统计刷新领域事件监听器，负责在业务事务提交后投递 MQ。
 *
 * @author LIN
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatsRefreshEventListener {

    private final StatsRefreshEventProducer statsRefreshEventProducer;

    private final EventFailLogService eventFailLogService;

    /**
     * 事务提交后投递统计刷新请求事件。
     *
     * @param event 统计刷新请求事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatsRefreshRequested(StatsRefreshRequestedEvent event) {
        try {
            statsRefreshEventProducer.sendRequestedEvent(event);
        } catch (AmqpException exception) {
            log.error("统计刷新事件投递失败，spaceId={}, scope={}",
                    event.getSpaceId(), event.getScope(), exception);
            eventFailLogService.recordFailure("STATS_REFRESH_REQUESTED", event, exception.getMessage());
        }
    }
}
