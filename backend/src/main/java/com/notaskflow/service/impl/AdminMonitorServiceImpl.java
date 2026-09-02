package com.notaskflow.service.impl;

import com.notaskflow.domain.vo.AdminSystemMonitorVO;
import com.notaskflow.mapper.AdminMonitorMapper;
import com.notaskflow.service.AdminMonitorService;
import com.sun.management.OperatingSystemMXBean;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 管理端系统监控服务实现。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMonitorServiceImpl implements AdminMonitorService {

    private static final String REDIS_KEYSPACE_HITS = "keyspace_hits";

    private static final String REDIS_KEYSPACE_MISSES = "keyspace_misses";

    private static final String REDIS_USED_MEMORY = "used_memory";

    private static final String REDIS_MAX_MEMORY = "maxmemory";

    private static final String REDIS_CONNECTED_CLIENTS = "connected_clients";

    private static final String REDIS_OPS_PER_SECOND = "instantaneous_ops_per_sec";

    private static final Path LINUX_NETWORK_STATS_PATH = Path.of("/proc/net/dev");

    private static final String MYSQL_QUESTIONS = "Questions";

    private static final String MYSQL_QUERIES = "Queries";

    private static final String MYSQL_UPTIME = "Uptime";

    private static final String MYSQL_SLOW_QUERIES = "Slow_queries";

    private static final String MYSQL_THREADS_CONNECTED = "Threads_connected";

    private static final String MYSQL_THREADS_RUNNING = "Threads_running";

    private final RedisConnectionFactory redisConnectionFactory;

    private final AdminMonitorMapper adminMonitorMapper;

    /**
     * 查询系统运行快照。
     *
     * @return 系统运行快照
     */
    @Override
    public AdminSystemMonitorVO snapshot() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        MemoryUsage heapUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        List<GarbageCollectorMXBean> garbageCollectorBeans = ManagementFactory.getGarbageCollectorMXBeans();
        DiskUsage diskUsage = calculateDiskUsage();
        CpuUsage cpuUsage = calculateCpuUsage();
        PhysicalMemory physicalMemory = calculatePhysicalMemory();
        RedisMonitor redisMonitor = readRedisMonitor();
        NetworkMonitor networkMonitor = readNetworkMonitor();
        MySqlMonitor mySqlMonitor = readMySqlMonitor();
        long gcCount = garbageCollectorBeans.stream()
                .mapToLong(bean -> positiveOrZero(bean.getCollectionCount()))
                .sum();
        long gcTimeMillis = garbageCollectorBeans.stream()
                .mapToLong(bean -> positiveOrZero(bean.getCollectionTime()))
                .sum();
        long heapMax = heapUsage.getMax() > 0 ? heapUsage.getMax() : heapUsage.getCommitted();

        AdminSystemMonitorVO monitor = new AdminSystemMonitorVO();
        monitor.setOsName(System.getProperty("os.name", "unknown"));
        monitor.setOsVersion(System.getProperty("os.version", "unknown"));
        monitor.setOsArch(System.getProperty("os.arch", "unknown"));
        monitor.setJavaVersion(System.getProperty("java.version", "unknown"));
        monitor.setCpuUsage(cpuUsage.systemUsage());
        monitor.setProcessCpuUsage(cpuUsage.processUsage());
        monitor.setSystemLoadAverage(systemLoadAverage());
        monitor.setCpuCoreCount(ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors());
        monitor.setPhysicalMemoryUsedBytes(physicalMemory.usedBytes());
        monitor.setPhysicalMemoryTotalBytes(physicalMemory.totalBytes());
        monitor.setPhysicalMemoryFreeBytes(physicalMemory.freeBytes());
        monitor.setPhysicalMemoryUsage(ratio(physicalMemory.usedBytes(), physicalMemory.totalBytes()));
        monitor.setJvmHeapUsedBytes(heapUsage.getUsed());
        monitor.setJvmHeapMaxBytes(heapMax);
        monitor.setJvmHeapUsage(ratio(heapUsage.getUsed(), heapMax));
        monitor.setDiskUsedBytes(diskUsage.usedBytes());
        monitor.setDiskTotalBytes(diskUsage.totalBytes());
        monitor.setDiskFreeBytes(diskUsage.freeBytes());
        monitor.setDiskUsage(ratio(diskUsage.usedBytes(), diskUsage.totalBytes()));
        monitor.setThreadCount(threadBean.getThreadCount());
        monitor.setDaemonThreadCount(threadBean.getDaemonThreadCount());
        monitor.setGcCount(gcCount);
        monitor.setGcTimeMillis(gcTimeMillis);
        monitor.setUptimeMillis(ManagementFactory.getRuntimeMXBean().getUptime());
        monitor.setRedisKeyCount(redisMonitor.keyCount());
        monitor.setRedisUsedMemoryBytes(redisMonitor.usedMemoryBytes());
        monitor.setRedisMaxMemoryBytes(redisMonitor.maxMemoryBytes());
        monitor.setRedisMemoryUsage(ratio(redisMonitor.usedMemoryBytes(), redisMonitor.maxMemoryBytes()));
        monitor.setRedisHitRate(ratio(redisMonitor.keyspaceHits(), redisMonitor.keyspaceHits() + redisMonitor.keyspaceMisses()));
        monitor.setRedisKeyspaceHits(redisMonitor.keyspaceHits());
        monitor.setRedisKeyspaceMisses(redisMonitor.keyspaceMisses());
        monitor.setRedisConnectedClients(redisMonitor.connectedClients());
        monitor.setRedisOpsPerSecond(redisMonitor.opsPerSecond());
        monitor.setNetworkReceivedBytes(networkMonitor.receivedBytes());
        monitor.setNetworkTransmittedBytes(networkMonitor.transmittedBytes());
        monitor.setNetworkInterfaceCount(networkMonitor.interfaceCount());
        monitor.setNetworkActiveInterfaceCount(networkMonitor.activeInterfaceCount());
        monitor.setNetworkTrafficSupported(networkMonitor.trafficSupported());
        monitor.setMysqlQueriesPerSecond(mySqlMonitor.queriesPerSecond());
        monitor.setMysqlQuestionCount(mySqlMonitor.questionCount());
        monitor.setMysqlSlowQueryCount(mySqlMonitor.slowQueryCount());
        monitor.setMysqlUptimeSeconds(mySqlMonitor.uptimeSeconds());
        monitor.setMysqlThreadsConnected(mySqlMonitor.threadsConnected());
        monitor.setMysqlThreadsRunning(mySqlMonitor.threadsRunning());
        monitor.setTimestamp(LocalDateTime.now());
        return monitor;
    }

    private CpuUsage calculateCpuUsage() {
        Object operatingSystemBean = ManagementFactory.getOperatingSystemMXBean();
        if (operatingSystemBean instanceof OperatingSystemMXBean extendedBean) {
            double systemCpuLoad = normalizeRatio(extendedBean.getCpuLoad());
            double processCpuLoad = normalizeRatio(extendedBean.getProcessCpuLoad());
            if (systemCpuLoad > 0D || processCpuLoad > 0D) {
                return new CpuUsage(systemCpuLoad, processCpuLoad);
            }
        }

        double loadAverage = systemLoadAverage();
        int processors = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
        if (loadAverage <= 0D || processors <= 0) {
            return new CpuUsage(0D, 0D);
        }
        return new CpuUsage(Math.min(1D, loadAverage / processors), 0D);
    }

    @SuppressWarnings("deprecation")
    private PhysicalMemory calculatePhysicalMemory() {
        Object operatingSystemBean = ManagementFactory.getOperatingSystemMXBean();
        if (operatingSystemBean instanceof OperatingSystemMXBean extendedBean) {
            long totalBytes = positiveOrZero(extendedBean.getTotalPhysicalMemorySize());
            long freeBytes = positiveOrZero(extendedBean.getFreePhysicalMemorySize());
            return new PhysicalMemory(totalBytes, freeBytes, Math.max(0L, totalBytes - freeBytes));
        }
        return new PhysicalMemory(0L, 0L, 0L);
    }

    private Double systemLoadAverage() {
        double loadAverage = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        return loadAverage < 0 ? 0D : loadAverage;
    }

    private DiskUsage calculateDiskUsage() {
        File[] roots = File.listRoots();
        long totalBytes = 0L;
        long freeBytes = 0L;
        if (roots != null) {
            for (File root : roots) {
                totalBytes += root.getTotalSpace();
                freeBytes += root.getUsableSpace();
            }
        }
        long usedBytes = Math.max(0L, totalBytes - freeBytes);
        return new DiskUsage(totalBytes, freeBytes, usedBytes);
    }

    private RedisMonitor readRedisMonitor() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            Properties info = connection.serverCommands().info();
            Long keyCount = connection.serverCommands().dbSize();
            return new RedisMonitor(
                    safeLong(keyCount),
                    propertyLong(info, REDIS_USED_MEMORY),
                    propertyLong(info, REDIS_MAX_MEMORY),
                    propertyLong(info, REDIS_KEYSPACE_HITS),
                    propertyLong(info, REDIS_KEYSPACE_MISSES),
                    propertyLong(info, REDIS_CONNECTED_CLIENTS),
                    propertyLong(info, REDIS_OPS_PER_SECOND)
            );
        } catch (RuntimeException exception) {
            log.warn("读取 Redis 监控信息失败", exception);
            return RedisMonitor.empty();
        }
    }

    private NetworkMonitor readNetworkMonitor() {
        InterfaceCounter interfaceCounter = countNetworkInterfaces();
        NetworkTraffic traffic = readLinuxNetworkTraffic();
        return new NetworkMonitor(
                traffic.receivedBytes(),
                traffic.transmittedBytes(),
                interfaceCounter.interfaceCount(),
                interfaceCounter.activeInterfaceCount(),
                traffic.supported()
        );
    }

    private InterfaceCounter countNetworkInterfaces() {
        int interfaceCount = 0;
        int activeInterfaceCount = 0;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return new InterfaceCounter(0, 0);
            }
            for (NetworkInterface networkInterface : Collections.list(interfaces)) {
                interfaceCount++;
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                    activeInterfaceCount++;
                }
            }
        } catch (IOException | RuntimeException exception) {
            log.warn("读取网络接口信息失败", exception);
        }
        return new InterfaceCounter(interfaceCount, activeInterfaceCount);
    }

    private NetworkTraffic readLinuxNetworkTraffic() {
        if (!Files.isReadable(LINUX_NETWORK_STATS_PATH)) {
            return new NetworkTraffic(0L, 0L, false);
        }

        long receivedBytes = 0L;
        long transmittedBytes = 0L;
        try (BufferedReader reader = Files.newBufferedReader(LINUX_NETWORK_STATS_PATH, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            while (line != null) {
                if (line.contains(":")) {
                    String[] sections = line.split(":");
                    if (sections.length == 2 && !sections[0].trim().equals("lo")) {
                        String[] values = sections[1].trim().split("\\s+");
                        if (values.length >= 16) {
                            receivedBytes += parseLong(values[0]);
                            transmittedBytes += parseLong(values[8]);
                        }
                    }
                }
                line = reader.readLine();
            }
            return new NetworkTraffic(receivedBytes, transmittedBytes, true);
        } catch (IOException exception) {
            log.warn("读取 Linux 网络流量信息失败", exception);
            return new NetworkTraffic(0L, 0L, false);
        }
    }

    private MySqlMonitor readMySqlMonitor() {
        long questionCount = statusLong(MYSQL_QUESTIONS);
        if (questionCount <= 0L) {
            questionCount = statusLong(MYSQL_QUERIES);
        }
        long uptimeSeconds = statusLong(MYSQL_UPTIME);
        double queriesPerSecond = uptimeSeconds > 0L ? questionCount / (double) uptimeSeconds : 0D;
        return new MySqlMonitor(
                questionCount,
                statusLong(MYSQL_SLOW_QUERIES),
                uptimeSeconds,
                statusLong(MYSQL_THREADS_CONNECTED),
                statusLong(MYSQL_THREADS_RUNNING),
                queriesPerSecond
        );
    }

    private long statusLong(String name) {
        try {
            List<Map<String, Object>> rows = adminMonitorMapper.selectGlobalStatus(name);
            if (rows == null || rows.isEmpty()) {
                return 0L;
            }
            Object value = rows.get(0).get("Value");
            return value == null ? 0L : parseLong(String.valueOf(value));
        } catch (RuntimeException exception) {
            log.warn("读取 MySQL 状态变量失败，name={}", name, exception);
            return 0L;
        }
    }

    private Double ratio(long used, long total) {
        if (total <= 0) {
            return 0D;
        }
        return Math.min(1D, Math.max(0D, used / (double) total));
    }

    private Double normalizeRatio(double value) {
        if (Double.isNaN(value) || value < 0D) {
            return 0D;
        }
        return Math.min(1D, value);
    }

    private long positiveOrZero(long value) {
        return Math.max(0L, value);
    }

    private long propertyLong(Properties properties, String key) {
        if (properties == null) {
            return 0L;
        }
        return parseLong(properties.getProperty(key));
    }

    private long safeLong(Long value) {
        return value == null ? 0L : Math.max(0L, value);
    }

    private long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            log.debug("监控数值解析失败，value={}", value);
            return 0L;
        }
    }

    /**
     * CPU 使用情况。
     *
     * @param systemUsage 系统 CPU 使用率
     * @param processUsage 当前进程 CPU 使用率
     */
    private record CpuUsage(Double systemUsage, Double processUsage) {
    }

    /**
     * 物理内存使用情况。
     *
     * @param totalBytes 总字节数
     * @param freeBytes 可用字节数
     * @param usedBytes 已用字节数
     */
    private record PhysicalMemory(long totalBytes, long freeBytes, long usedBytes) {
    }

    /**
     * 磁盘使用情况。
     *
     * @param totalBytes 总字节数
     * @param freeBytes 可用字节数
     * @param usedBytes 已用字节数
     */
    private record DiskUsage(long totalBytes, long freeBytes, long usedBytes) {
    }

    /**
     * Redis 监控信息。
     *
     * @param keyCount Key 数量
     * @param usedMemoryBytes 已用内存
     * @param maxMemoryBytes 最大内存
     * @param keyspaceHits 命中次数
     * @param keyspaceMisses 未命中次数
     * @param connectedClients 客户端连接数
     * @param opsPerSecond 每秒操作数
     */
    private record RedisMonitor(
            long keyCount,
            long usedMemoryBytes,
            long maxMemoryBytes,
            long keyspaceHits,
            long keyspaceMisses,
            long connectedClients,
            long opsPerSecond
    ) {

        private static RedisMonitor empty() {
            return new RedisMonitor(0L, 0L, 0L, 0L, 0L, 0L, 0L);
        }
    }

    /**
     * 网络监控信息。
     *
     * @param receivedBytes 接收字节
     * @param transmittedBytes 发送字节
     * @param interfaceCount 网卡数量
     * @param activeInterfaceCount 活跃网卡数量
     * @param trafficSupported 是否支持流量读取
     */
    private record NetworkMonitor(
            long receivedBytes,
            long transmittedBytes,
            int interfaceCount,
            int activeInterfaceCount,
            boolean trafficSupported
    ) {
    }

    /**
     * 网络接口计数。
     *
     * @param interfaceCount 网卡数量
     * @param activeInterfaceCount 活跃网卡数量
     */
    private record InterfaceCounter(int interfaceCount, int activeInterfaceCount) {
    }

    /**
     * 网络流量信息。
     *
     * @param receivedBytes 接收字节
     * @param transmittedBytes 发送字节
     * @param supported 是否支持读取
     */
    private record NetworkTraffic(long receivedBytes, long transmittedBytes, boolean supported) {
    }

    /**
     * MySQL 监控信息。
     *
     * @param questionCount 查询计数
     * @param slowQueryCount 慢查询计数
     * @param uptimeSeconds 运行秒数
     * @param threadsConnected 连接线程数
     * @param threadsRunning 运行线程数
     * @param queriesPerSecond 平均 QPS
     */
    private record MySqlMonitor(
            long questionCount,
            long slowQueryCount,
            long uptimeSeconds,
            long threadsConnected,
            long threadsRunning,
            double queriesPerSecond
    ) {
    }
}
