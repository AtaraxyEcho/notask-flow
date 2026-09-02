package com.notaskflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 协作文档配置属性。
 *
 * @author LIN
 */
@Data
@Component
@ConfigurationProperties(prefix = "notask-flow.collab")
public class CollaborationProperties {

    private String internalToken = "notask-flow-collab-internal-change-me";

    private String realtimeBroadcastUrl = "";
}
