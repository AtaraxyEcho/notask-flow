package com.notaskflow.job;

import com.notaskflow.service.EventFailLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 失败事件补偿重试定时任务。
 *
 * @author LIN
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventFailRetryJob {

    private final EventFailLogService eventFailLogService;

    @Value("${notask-flow.event-fail.retry-batch-size:20}")
    private int retryBatchSize;

    @Value("${notask-flow.event-fail.max-retry-count:5}")
    private int maxRetryCount;

    /**
     * 定时重试待补偿的失败事件。
     */
    @Scheduled(fixedDelayString = "${notask-flow.event-fail.retry-delay-ms:120000}")
    public void retryPendingFailures() {
        int retried = eventFailLogService.retryPendingFailures(retryBatchSize, maxRetryCount);
        if (retried > 0) {
            log.info("失败事件补偿重试完成，retried={}", retried);
        }
    }
}
