package com.notaskflow.mq.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notaskflow.domain.entity.EventFailLog;
import com.notaskflow.event.FileProcessRequestedEvent;
import com.notaskflow.event.MailSendRequestedEvent;
import com.notaskflow.event.NotificationCreateEvent;
import com.notaskflow.event.SearchIndexRequestedEvent;
import com.notaskflow.event.StatsRefreshRequestedEvent;
import com.notaskflow.event.TaskMemberStatusChangedEvent;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 失败事件重试投递器。
 *
 * @author LIN
 */
@Component
@RequiredArgsConstructor
public class EventFailRetryPublisher {

    private static final String TASK_MEMBER_STATUS_CHANGED = "TASK_MEMBER_STATUS_CHANGED";

    private static final String MAIL_SEND_REQUESTED = "MAIL_SEND_REQUESTED";

    private static final String NOTIFICATION_CREATE = "NOTIFICATION_CREATE";

    private static final String SEARCH_INDEX_REQUESTED = "SEARCH_INDEX_REQUESTED";

    private static final String STATS_REFRESH_REQUESTED = "STATS_REFRESH_REQUESTED";

    private static final String FILE_PROCESS_REQUESTED = "FILE_PROCESS_REQUESTED";

    private static final String CONSUME_SUFFIX = "_CONSUME";

    private final TaskEventProducer taskEventProducer;

    private final MailEventProducer mailEventProducer;

    private final NotificationEventProducer notificationEventProducer;

    private final SearchIndexEventProducer searchIndexEventProducer;

    private final StatsRefreshEventProducer statsRefreshEventProducer;

    private final FileProcessEventProducer fileProcessEventProducer;

    private final ObjectMapper objectMapper;

    /**
     * 重试投递失败事件。
     *
     * @param failLog 失败事件记录
     */
    public void publish(EventFailLog failLog) {
        String eventType = normalizeEventType(failLog.getEventType());
        switch (eventType) {
            case TASK_MEMBER_STATUS_CHANGED:
                taskEventProducer.sendMemberStatusChanged(readEvent(failLog, TaskMemberStatusChangedEvent.class));
                break;
            case MAIL_SEND_REQUESTED:
                mailEventProducer.sendRequestedEvent(readEvent(failLog, MailSendRequestedEvent.class));
                break;
            case NOTIFICATION_CREATE:
                notificationEventProducer.sendCreateEvent(readEvent(failLog, NotificationCreateEvent.class));
                break;
            case SEARCH_INDEX_REQUESTED:
                searchIndexEventProducer.sendRequestedEvent(readEvent(failLog, SearchIndexRequestedEvent.class));
                break;
            case STATS_REFRESH_REQUESTED:
                statsRefreshEventProducer.sendRequestedEvent(readEvent(failLog, StatsRefreshRequestedEvent.class));
                break;
            case FILE_PROCESS_REQUESTED:
                fileProcessEventProducer.sendRequestedEvent(readEvent(failLog, FileProcessRequestedEvent.class));
                break;
            default:
                throw new IllegalArgumentException("Unsupported event type: " + failLog.getEventType());
        }
    }

    private String normalizeEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return "";
        }
        String normalized = eventType.trim()
                .replace('.', '_')
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
        if (normalized.endsWith(CONSUME_SUFFIX)) {
            return normalized.substring(0, normalized.length() - CONSUME_SUFFIX.length());
        }
        return normalized;
    }

    private <T> T readEvent(EventFailLog failLog, Class<T> eventClass) {
        try {
            return objectMapper.readValue(failLog.getEventData(), eventClass);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to deserialize event data: " + failLog.getId(), exception);
        }
    }
}
