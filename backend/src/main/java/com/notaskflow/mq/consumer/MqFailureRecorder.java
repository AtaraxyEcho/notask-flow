package com.notaskflow.mq.consumer;

import com.notaskflow.service.EventFailLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MQ 消费失败记录器。
 *
 * @author LIN
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqFailureRecorder {

    public static final String TASK_MEMBER_STATUS_CHANGED = "task.member.status.changed.consume";

    public static final String MAIL_SEND_REQUESTED = "mail.send.requested.consume";

    public static final String NOTIFICATION_CREATE = "notification.create.consume";

    public static final String SEARCH_INDEX_REQUESTED = "search.index.requested.consume";

    public static final String STATS_REFRESH_REQUESTED = "stats.refresh.requested.consume";

    public static final String FILE_PROCESS_REQUESTED = "file.process.requested.consume";

    private final EventFailLogService eventFailLogService;

    /**
     * 记录 MQ 消费失败事件。
     *
     * @param eventType 事件类型
     * @param eventData 事件数据
     * @param exception 消费异常
     */
    public void recordConsumerFailure(String eventType, Object eventData, RuntimeException exception) {
        try {
            eventFailLogService.recordFailure(eventType, eventData, resolveFailReason(exception));
        } catch (RuntimeException recordException) {
            log.warn("MQ 消费失败日志记录失败，eventType={}", eventType, recordException);
        }
    }

    private String resolveFailReason(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }
}
