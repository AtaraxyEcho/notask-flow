package com.notaskflow.event;

import com.notaskflow.common.enums.BusinessType;
import com.notaskflow.common.enums.NotificationType;
import java.io.Serializable;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮件发送请求事件。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
public class MailSendRequestedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;

    private Long userId;

    private NotificationType type;

    private BusinessType businessType;

    private String title;

    private String content;

    /**
     * 创建邮件发送请求事件。
     *
     * @param userId 用户标识
     * @param type 通知类型
     * @param businessType 业务类型
     * @param title 邮件标题
     * @param content 邮件内容
     */
    public MailSendRequestedEvent(Long userId, NotificationType type, BusinessType businessType, String title,
                                  String content) {
        this(UUID.randomUUID().toString(), userId, type, businessType, title, content);
    }

    /**
     * 创建邮件发送请求事件。
     *
     * @param eventId 事件标识
     * @param userId 用户标识
     * @param type 通知类型
     * @param businessType 业务类型
     * @param title 邮件标题
     * @param content 邮件内容
     */
    public MailSendRequestedEvent(String eventId, Long userId, NotificationType type, BusinessType businessType,
                                  String title, String content) {
        this.eventId = eventId == null || eventId.isBlank() ? UUID.randomUUID().toString() : eventId;
        this.userId = userId;
        this.type = type;
        this.businessType = businessType;
        this.title = title;
        this.content = content;
    }
}
