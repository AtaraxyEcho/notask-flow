package com.notaskflow.domain.vo;

import com.notaskflow.common.enums.BusinessType;
import com.notaskflow.common.enums.NotificationType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationVO {

    private Long id;

    private Long userId;

    private Long spaceId;

    private NotificationType type;

    private BusinessType businessType;

    private Long businessId;

    private String title;

    private String content;

    private Boolean isRead;

    private LocalDateTime gmtCreate;
}
