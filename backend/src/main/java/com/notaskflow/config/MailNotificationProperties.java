package com.notaskflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮件通知配置属性。
 *
 * @author LIN
 */
@Data
@Component
@ConfigurationProperties(prefix = "notask-flow.mail")
public class MailNotificationProperties {

    private String from;

    private String appBaseUrl = "http://localhost:3000";
}
