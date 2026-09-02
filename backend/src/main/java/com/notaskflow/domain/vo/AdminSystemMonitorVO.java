package com.notaskflow.domain.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端系统监控快照视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminSystemMonitorVO {

    private String osName;

    private String osVersion;

    private String osArch;

    private String javaVersion;

    private Double cpuUsage;

    private Double processCpuUsage;

    private Double systemLoadAverage;

    private Integer cpuCoreCount;

    private Long physicalMemoryUsedBytes;

    private Long physicalMemoryTotalBytes;

    private Long physicalMemoryFreeBytes;

    private Double physicalMemoryUsage;

    private Long jvmHeapUsedBytes;

    private Long jvmHeapMaxBytes;

    private Double jvmHeapUsage;

    private Long diskUsedBytes;

    private Long diskTotalBytes;

    private Long diskFreeBytes;

    private Double diskUsage;

    private Integer threadCount;

    private Integer daemonThreadCount;

    private Long gcCount;

    private Long gcTimeMillis;

    private Long uptimeMillis;

    private Long redisKeyCount;

    private Long redisUsedMemoryBytes;

    private Long redisMaxMemoryBytes;

    private Double redisMemoryUsage;

    private Double redisHitRate;

    private Long redisKeyspaceHits;

    private Long redisKeyspaceMisses;

    private Long redisConnectedClients;

    private Long redisOpsPerSecond;

    private Long networkReceivedBytes;

    private Long networkTransmittedBytes;

    private Integer networkInterfaceCount;

    private Integer networkActiveInterfaceCount;

    private Boolean networkTrafficSupported;

    private Double mysqlQueriesPerSecond;

    private Long mysqlQuestionCount;

    private Long mysqlSlowQueryCount;

    private Long mysqlUptimeSeconds;

    private Long mysqlThreadsConnected;

    private Long mysqlThreadsRunning;

    private LocalDateTime timestamp;
}
