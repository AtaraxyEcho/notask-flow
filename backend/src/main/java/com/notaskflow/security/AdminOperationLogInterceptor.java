package com.notaskflow.security;

import cn.dev33.satoken.stp.StpUtil;
import com.notaskflow.service.AdminLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理端操作日志拦截器。
 *
 * @author LIN
 */
@Component
@RequiredArgsConstructor
public class AdminOperationLogInterceptor implements HandlerInterceptor {

    private static final String ADMIN_API_PREFIX = "/api/v1/admin/";

    private static final String ADMIN_LOGIN_PATH = "/api/v1/admin/auth/login";

    private final AdminLogService adminLogService;

    /**
     * 请求完成后记录管理端非查询操作。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param handler 处理器
     * @param exception 异常
     */
    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {
        if (!shouldRecord(request)) {
            return;
        }
        boolean success = exception == null && response.getStatus() < HttpServletResponse.SC_BAD_REQUEST;
        String errorMessage = exception == null ? "" : exception.getMessage();
        adminLogService.recordOperation(
                resolveOperator(),
                request.getMethod(),
                request.getRequestURI(),
                resolveOperationName(request),
                success,
                errorMessage);
    }

    /**
     * 判断是否需要记录。
     *
     * @param request HTTP 请求
     * @return 是否需要记录
     */
    private boolean shouldRecord(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!path.startsWith(ADMIN_API_PREFIX) || ADMIN_LOGIN_PATH.equals(path)) {
            return false;
        }
        return !"GET".equalsIgnoreCase(request.getMethod());
    }

    /**
     * 解析操作人。
     *
     * @return 操作人
     */
    private String resolveOperator() {
        if (!StpUtil.isLogin()) {
            return "anonymous";
        }
        return String.valueOf(StpUtil.getLoginId());
    }

    /**
     * 解析操作名称。
     *
     * @param request HTTP 请求
     * @return 操作名称
     */
    private String resolveOperationName(HttpServletRequest request) {
        return request.getMethod() + " " + request.getRequestURI();
    }
}
