package com.notaskflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件管理配置属性。
 *
 * @author LIN
 */
@Data
@Component
@ConfigurationProperties(prefix = "notask-flow.file")
public class FileManagementProperties {

    private Boolean cleanupEnabled = true;

    private Integer trashRetentionDays = 30;

    private Integer cleanupBatchSize = 100;

    private Long multipartThresholdSize = 52428800L;

    private Long chunkSize = 5242880L;

    private Integer chunkUploadSessionMinutes = 30;

    private String chunkTempDir = "";
}
