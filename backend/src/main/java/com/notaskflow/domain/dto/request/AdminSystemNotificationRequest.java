package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端系统通知发送请求。
 *
 * @author LIN
 */
@Data
public class AdminSystemNotificationRequest {

    @NotBlank(message = "通知标题不能为空")
    @Size(max = 100, message = "通知标题不能超过100个字符")
    private String title;

    @NotBlank(message = "通知内容不能为空")
    @Size(max = 1000, message = "通知内容不能超过1000个字符")
    private String content;
}
