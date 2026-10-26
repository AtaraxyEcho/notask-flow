package com.notaskflow.utils;

import com.notaskflow.config.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 请求客户端信息工具类。
 *
 * <p>仅当直连来源地址命中受信任反向代理网段时，才会解析 {@code X-Forwarded-For} /
 * {@code X-Real-IP}，防止客户端伪造审计日志与登录会话 IP。</p>
 *
 * @author LIN
 */
@Slf4j
public final class RequestClientUtil {

    private static final String UNKNOWN = "";

    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";

    private static final String HEADER_REAL_IP = "X-Real-IP";

    private static final int IPV4_BITS = 32;

    private static final int IPV6_BITS = 128;

    private static volatile List<CidrBlock> trustedProxies = List.of();

    private RequestClientUtil() {
    }

    /**
     * 配置受信任反向代理网段。
     *
     * @param cidrs CIDR 网段列表
     */
    public static void configureTrustedProxies(List<String> cidrs) {
        List<CidrBlock> blocks = new ArrayList<>();
        if (cidrs != null) {
            for (String cidr : cidrs) {
                CidrBlock block = CidrBlock.parse(cidr);
                if (block != null) {
                    blocks.add(block);
                }
            }
        }
        trustedProxies = List.copyOf(blocks);
    }

    /**
     * 获取当前请求 IP。
     *
     * @return 请求 IP
     */
    public static String currentIp() {
        return resolveIp(currentRequest());
    }

    /**
     * 获取当前请求 User-Agent。
     *
     * @return User-Agent
     */
    public static String currentUserAgent() {
        HttpServletRequest request = currentRequest();
        return request == null ? UNKNOWN : emptyToUnknown(request.getHeader("User-Agent"));
    }

    /**
     * 获取当前请求。
     *
     * @return 当前请求
     */
    public static HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest();
    }

    /**
     * 解析请求 IP。
     *
     * @param request HTTP 请求
     * @return 请求 IP
     */
    public static String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        String remoteAddr = emptyToUnknown(request.getRemoteAddr());
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }
        String forwardedFor = request.getHeader(HEADER_FORWARDED_FOR);
        if (StringUtils.hasText(forwardedFor)) {
            String clientIp = resolveFromForwardedFor(forwardedFor);
            if (StringUtils.hasText(clientIp)) {
                return clientIp;
            }
        }
        String realIp = request.getHeader(HEADER_REAL_IP);
        if (StringUtils.hasText(realIp) && isValidIp(realIp.trim())) {
            return realIp.trim();
        }
        return remoteAddr;
    }

    /**
     * 解析 User-Agent。
     *
     * @param request HTTP 请求
     * @return User-Agent
     */
    public static String resolveUserAgent(HttpServletRequest request) {
        return request == null ? UNKNOWN : emptyToUnknown(request.getHeader("User-Agent"));
    }

    /**
     * 从 X-Forwarded-For 中自右向左解析首个非受信任代理地址。
     *
     * @param forwardedFor 转发头原始值
     * @return 客户端 IP，无法解析时返回空串
     */
    private static String resolveFromForwardedFor(String forwardedFor) {
        String[] candidates = forwardedFor.split(",");
        for (int index = candidates.length - 1; index >= 0; index--) {
            String candidate = candidates[index].trim();
            if (!StringUtils.hasText(candidate) || !isValidIp(candidate)) {
                continue;
            }
            if (!isTrustedProxy(candidate)) {
                return candidate;
            }
        }
        return "";
    }

    /**
     * 判断地址是否属于受信任反向代理。
     *
     * @param address IP 地址
     * @return true 表示受信任
     */
    private static boolean isTrustedProxy(String address) {
        if (!StringUtils.hasText(address)) {
            return false;
        }
        for (CidrBlock block : trustedProxies) {
            if (block.contains(address)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断字符串是否为合法 IP 地址。
     *
     * @param address IP 地址
     * @return true 表示合法
     */
    private static boolean isValidIp(String address) {
        return toBytes(address) != null;
    }

    /**
     * 将 IP 字符串转换为字节数组。
     *
     * @param address IP 地址
     * @return 字节数组，非法时返回 null
     */
    private static byte[] toBytes(String address) {
        if (!StringUtils.hasText(address)) {
            return null;
        }
        String normalized = address.trim();
        if (normalized.startsWith("[")) {
            int endIndex = normalized.indexOf(']');
            normalized = endIndex > 0 ? normalized.substring(1, endIndex) : normalized.substring(1);
        }
        int zoneIndex = normalized.indexOf('%');
        if (zoneIndex > 0) {
            normalized = normalized.substring(0, zoneIndex);
        }
        if (!normalized.matches("[0-9a-fA-F:.]+")) {
            return null;
        }
        try {
            return InetAddress.getByName(normalized).getAddress();
        } catch (UnknownHostException exception) {
            return null;
        }
    }

    /**
     * 将空字符串转为空值占位。
     *
     * @param value 原始值
     * @return 安全值
     */
    private static String emptyToUnknown(String value) {
        return StringUtils.hasText(value) ? value : UNKNOWN;
    }

    /**
     * CIDR 网段匹配。
     *
     * @param network 网段起始地址
     * @param prefixLength 前缀长度
     */
    private record CidrBlock(byte[] network, int prefixLength) {

        /**
         * 解析 CIDR 表达式。
         *
         * @param cidr CIDR 表达式
         * @return 网段对象，非法时返回 null
         */
        static CidrBlock parse(String cidr) {
            if (!StringUtils.hasText(cidr)) {
                return null;
            }
            String value = cidr.trim();
            int slashIndex = value.indexOf('/');
            String addressPart = slashIndex > 0 ? value.substring(0, slashIndex) : value;
            byte[] addressBytes = toBytes(addressPart);
            if (addressBytes == null) {
                log.warn("忽略非法的受信任代理解析网段配置：{}", cidr);
                return null;
            }
            int maxBits = addressBytes.length * Byte.SIZE;
            int prefixLength = maxBits;
            if (slashIndex > 0) {
                try {
                    prefixLength = Integer.parseInt(value.substring(slashIndex + 1));
                } catch (NumberFormatException exception) {
                    log.warn("忽略非法的受信任代理解析网段前缀：{}", cidr);
                    return null;
                }
            }
            if (prefixLength < 0 || prefixLength > maxBits) {
                log.warn("忽略越界的受信任代理解析网段前缀：{}", cidr);
                return null;
            }
            return new CidrBlock(addressBytes, prefixLength);
        }

        /**
         * 判断地址是否命中当前网段。
         *
         * @param address IP 地址
         * @return true 表示命中
         */
        boolean contains(String address) {
            byte[] candidate = toBytes(address);
            if (candidate == null) {
                return false;
            }
            byte[] normalizedCandidate = normalize(candidate, network.length);
            if (normalizedCandidate == null) {
                return false;
            }
            int fullBytes = prefixLength / Byte.SIZE;
            int remainingBits = prefixLength % Byte.SIZE;
            for (int index = 0; index < fullBytes; index++) {
                if (normalizedCandidate[index] != network[index]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xFF << (Byte.SIZE - remainingBits);
            return (normalizedCandidate[fullBytes] & mask) == (network[fullBytes] & mask);
        }

        /**
         * 将 IPv4 映射地址规范化为与网段一致的字节长度。
         *
         * @param candidate 候选地址字节
         * @param targetLength 目标长度
         * @return 规范化后的字节数组，长度不一致且无法映射时返回 null
         */
        private static byte[] normalize(byte[] candidate, int targetLength) {
            if (candidate.length == targetLength) {
                return candidate;
            }
            if (candidate.length == IPV6_BITS / Byte.SIZE && targetLength == IPV4_BITS / Byte.SIZE) {
                return extractIpv4FromMapped(candidate);
            }
            if (candidate.length == IPV4_BITS / Byte.SIZE && targetLength == IPV6_BITS / Byte.SIZE) {
                return mapIpv4ToIpv6(candidate);
            }
            return null;
        }

        /**
         * 从 IPv4 映射的 IPv6 地址中提取 IPv4 字节。
         *
         * @param candidate IPv6 地址字节
         * @return IPv4 字节，非映射地址返回 null
         */
        private static byte[] extractIpv4FromMapped(byte[] candidate) {
            for (int index = 0; index < 10; index++) {
                if (candidate[index] != 0) {
                    return null;
                }
            }
            if ((candidate[10] & 0xFF) != 0xFF || (candidate[11] & 0xFF) != 0xFF) {
                return null;
            }
            return new byte[]{candidate[12], candidate[13], candidate[14], candidate[15]};
        }

        /**
         * 将 IPv4 字节映射为 IPv6 字节。
         *
         * @param candidate IPv4 地址字节
         * @return IPv6 地址字节
         */
        private static byte[] mapIpv4ToIpv6(byte[] candidate) {
            byte[] mapped = new byte[IPV6_BITS / Byte.SIZE];
            mapped[10] = (byte) 0xFF;
            mapped[11] = (byte) 0xFF;
            System.arraycopy(candidate, 0, mapped, 12, candidate.length);
            return mapped;
        }
    }

    /**
     * 启动时初始化受信任代理配置的兜底组件。
     *
     * @author LIN
     */
    @Component
    public static class TrustedProxyInitializer implements InitializingBean {

        private final SecurityProperties securityProperties;

        /**
         * 创建初始化器。
         *
         * @param securityProperties 安全配置
         */
        public TrustedProxyInitializer(SecurityProperties securityProperties) {
            this.securityProperties = securityProperties;
        }

        @Override
        public void afterPropertiesSet() {
            configureTrustedProxies(securityProperties.getTrustedProxies());
        }
    }
}
