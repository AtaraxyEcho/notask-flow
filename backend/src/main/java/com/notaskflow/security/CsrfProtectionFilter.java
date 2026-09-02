package com.notaskflow.security;

import com.notaskflow.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 轻量 CSRF 双提交 Cookie 过滤器。
 *
 * @author LIN
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class CsrfProtectionFilter extends OncePerRequestFilter {

    private static final String CSRF_COOKIE_NAME = "NOTASK_CSRF_TOKEN";

    private static final String CSRF_HEADER_NAME = "X-CSRF-TOKEN";

    private static final String INTERNAL_API_PREFIX = "/api/v1/internal/";

    private static final int TOKEN_BYTE_LENGTH = 32;

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final SecurityProperties securityProperties;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 校验 CSRF Token。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException IO 异常
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!securityProperties.isCsrfEnabled() || isInternalApi(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String cookieToken = resolveCookieToken(request);
        if (isSafeMethod(request)) {
            ensureTokenCookie(request, response, cookieToken);
            filterChain.doFilter(request, response);
            return;
        }

        String headerToken = request.getHeader(CSRF_HEADER_NAME);
        if (!StringUtils.hasText(cookieToken)
                || !StringUtils.hasText(headerToken)
                || !cookieToken.equals(headerToken)) {
            writeForbidden(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 判断是否安全方法。
     *
     * @param request HTTP 请求
     * @return 是否安全方法
     */
    private boolean isSafeMethod(HttpServletRequest request) {
        return SAFE_METHODS.contains(request.getMethod());
    }

    /**
     * 判断是否内部接口。
     *
     * @param request HTTP 请求
     * @return 是否内部接口
     */
    private boolean isInternalApi(HttpServletRequest request) {
        return request.getRequestURI().startsWith(INTERNAL_API_PREFIX);
    }

    /**
     * 确保响应中存在 CSRF Cookie。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param currentToken 当前 Token
     */
    private void ensureTokenCookie(HttpServletRequest request, HttpServletResponse response, String currentToken) {
        if (StringUtils.hasText(currentToken)) {
            return;
        }
        Cookie cookie = new Cookie(CSRF_COOKIE_NAME, generateToken());
        cookie.setPath("/");
        cookie.setSecure(request.isSecure());
        cookie.setHttpOnly(false);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    /**
     * 读取 CSRF Cookie Token。
     *
     * @param request HTTP 请求
     * @return Token
     */
    private String resolveCookieToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (CSRF_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 生成 CSRF Token。
     *
     * @return Token
     */
    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 写入 CSRF 校验失败响应。
     *
     * @param response HTTP 响应
     * @throws IOException IO 异常
     */
    private void writeForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":403,\"message\":\"CSRF Token无效\",\"data\":null}");
    }
}
