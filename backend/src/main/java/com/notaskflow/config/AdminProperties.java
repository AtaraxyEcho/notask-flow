package com.notaskflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 管理平台 Administrator 账号配置。
 *
 * @author LIN
 */
@Data
@Component
@ConfigurationProperties(prefix = "notask-flow.admin")
public class AdminProperties {

    private String username = "Administrator";

    private String password = "change-me";
}
