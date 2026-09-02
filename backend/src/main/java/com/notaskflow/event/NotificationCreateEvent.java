package com.notaskflow.event;

import com.notaskflow.common.enums.BusinessType;
import com.notaskflow.common.enums.NotificationType;
import java.io.Serializable;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知创建事件。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
public class NotificationCreateEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;

    private Long userId;

    private NotificationType type;

    private BusinessType businessType;

    private Long businessId;

    private String title;

    private String content;

    /**
     * 创建通知事件。
     *
     * @param userId 用户标识
     * @param type 通知类型
     * @param businessType 业务类型
     * @param businessId 业务标识
     * @param title 通知标题
     * @param content 通知内容
     */
    public NotificationCreateEvent(Long userId, NotificationType type, BusinessType businessType, Long businessId,
                                   String title, String content) {
        this(UUID.randomUUID().toString(), userId, type, businessType, businessId, title, content);
    }

    /**
     * 创建通知事件。
     *
     * @param eventId 事件标识
     * @param userId 用户标识
     * @param type 通知类型
     * @param businessType 业务类型
     * @param businessId 业务标识
     * @param title 通知标题
     * @param content 通知内容
     */
    public NotificationCreateEvent(String eventId, Long userId, NotificationType type, BusinessType businessType,
                                   Long businessId, String title, String content) {
        this.eventId = eventId == null || eventId.isBlank() ? UUID.randomUUID().toString() : eventId;
        this.userId = userId;
        this.type = type;
        this.businessType = businessType;
        this.businessId = businessId;
        this.title = title;
        this.content = content;
    }
}
