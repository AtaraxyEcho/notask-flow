package com.notaskflow.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO 对象存储配置属性。
 *
 * 
 */
@Data
@ConfigurationProperties(prefix = "notask-flow.minio")
public class MinioProperties {

    private String endpoint;

    private String accessKey;

    private String secretKey;

    private String bucketName = "notask-flow";

    private Long maxFileSize = 52428800L;

    private List<String> allowedMimeTypes = new ArrayList<>();
}
