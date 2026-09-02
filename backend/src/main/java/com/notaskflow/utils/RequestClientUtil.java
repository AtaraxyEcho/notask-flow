package com.notaskflow.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 请求客户端信息工具类。
 *
 * @author LIN
 */
public final class RequestClientUtil {

    private static final String UNKNOWN = "";

    private RequestClientUtil() {
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
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return emptyToUnknown(request.getRemoteAddr());
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
     * 将空字符串转为空值占位。
     *
     * @param value 原始值
     * @return 安全值
     */
    private static String emptyToUnknown(String value) {
        return StringUtils.hasText(value) ? value : UNKNOWN;
    }
}
