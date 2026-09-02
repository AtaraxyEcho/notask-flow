package com.notaskflow.service.impl;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import com.notaskflow.common.constant.RedisKeyConstants;
import com.notaskflow.common.enums.ClientType;
import com.notaskflow.domain.dto.request.LoginRequest;
import com.notaskflow.domain.vo.LoginSessionVO;
import com.notaskflow.exception.BusinessException;
import com.notaskflow.exception.ErrorCode;
import com.notaskflow.service.LoginSessionService;
import com.notaskflow.service.SystemSettingService;
import com.notaskflow.utils.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 登录会话服务实现，基于 Redis 维护多端会话元信息。
 *
 * @author LIN
 */
@Service
@RequiredArgsConstructor
public class LoginSessionServiceImpl implements LoginSessionService {

    private static final Long ADMIN_USER_ID = 0L;

    private static final int ACTIVE_UPDATE_INTERVAL_SECONDS = 60;

    private static final long REDIS_SCAN_COUNT = 1000L;

    private static final String CLIENT_TYPE_HEADER = "X-Client-Type";

    private final RedisUtil redisUtil;

    private final SystemSettingService systemSettingService;

    /**
     * 创建普通用户登录会话。
     *
     * @param userId 用户标识
     * @param username 用户名
     * @param request 登录请求
     * @return 登录会话
     */
    @Override
    public LoginSessionVO createUserSession(Long userId, String username, LoginRequest request) {
        ClientType clientType = resolveUserClientType(request);
        String deviceId = resolveDeviceId(request, clientType);
        if (ClientType.WEB.equals(clientType) && systemSettingService.isSingleDeviceLoginOnly()) {
            revokeSessionsByClientType(userId, ClientType.WEB);
        }
        if (ClientType.ANDROID.equals(clientType) || ClientType.IOS.equals(clientType)) {
            revokeClientDeviceSession(userId, clientType, deviceId);
        }
        return createSession(userId, username, clientType, request, deviceId, tokenTtl(clientType));
    }

    /**
     * 创建管理端登录会话。
     *
     * @param username 管理员账号
     * @param request 登录请求
     * @return 登录会话
     */
    @Override
    public LoginSessionVO createAdminSession(String username, LoginRequest request) {
        revokeUserSessions(ADMIN_USER_ID);
        return createSession(ADMIN_USER_ID, username, ClientType.ADMIN_WEB, request,
                resolveDeviceId(request, ClientType.ADMIN_WEB), tokenTtl(ClientType.ADMIN_WEB));
    }

    /**
     * 根据会话信息构造 Sa-Token 登录模型。
     *
     * @param session 登录会话
     * @return 登录模型
     */
    @Override
    public SaLoginModel buildLoginModel(LoginSessionVO session) {
        return new SaLoginModel()
                .setTimeout(session.getExpireSeconds());
    }

    /**
     * 绑定当前请求令牌与会话。
     *
     * @param session 登录会话
     */
    @Override
    public void bindCurrentToken(LoginSessionVO session) {
        String tokenValue = StpUtil.getTokenValue();
        if (!StringUtils.hasText(tokenValue)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录令牌签发失败");
        }
        redisUtil.set(RedisKeyConstants.tokenSession(tokenValue), session.getSessionId(),
                Duration.ofSeconds(Math.max(1L, session.getExpireSeconds())));
    }

    /**
     * 校验当前请求的登录会话。
     *
     * @param adminApi 是否为管理端接口
     */
    @Override
    public void validateCurrentSession(boolean adminApi) {
        LoginSessionVO session = currentSession();
        if (adminApi && !ClientType.ADMIN_WEB.equals(session.getClientType())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "请使用管理端账号访问");
        }
        if (!adminApi && ClientType.ADMIN_WEB.equals(session.getClientType())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "管理端账号不能访问普通业务接口");
        }
        refreshActiveTimeIfNecessary(session);
    }

    /**
     * 获取当前请求绑定的会话。
     *
     * @return 登录会话
     */
    @Override
    public LoginSessionVO currentSession() {
        LoginSessionVO session = findSession(currentSessionId());
        if (session == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录会话已失效，请重新登录");
        }
        return session;
    }

    /**
     * 撤销当前登录会话。
     */
    @Override
    public void revokeCurrentSession() {
        if (!StpUtil.isLogin()) {
            return;
        }
        String tokenValue = StpUtil.getTokenValue();
        if (!StringUtils.hasText(tokenValue)) {
            return;
        }
        String sessionId = redisUtil.getString(RedisKeyConstants.tokenSession(tokenValue));
        if (StringUtils.hasText(sessionId)) {
            revokeSession(sessionId);
        }
        redisUtil.delete(RedisKeyConstants.tokenSession(tokenValue));
    }

    /**
     * 撤销指定用户全部会话。
     *
     * @param userId 用户标识
     */
    @Override
    public void revokeUserSessions(Long userId) {
        Set<String> sessionIds = redisUtil.setMembers(RedisKeyConstants.userSessions(userId));
        for (String sessionId : sessionIds) {
            revokeSession(sessionId);
        }
        redisUtil.delete(RedisKeyConstants.userSessions(userId));
    }

    /**
     * 撤销指定会话。
     *
     * @param sessionId 会话标识
     */
    @Override
    public void revokeSession(String sessionId) {
        LoginSessionVO session = findSession(sessionId);
        if (session == null) {
            redisUtil.delete(RedisKeyConstants.loginSession(sessionId));
            return;
        }
        redisUtil.delete(RedisKeyConstants.loginSession(sessionId));
        deleteTokenSessionMappings(sessionId);
        redisUtil.setRemove(RedisKeyConstants.userSessions(session.getUserId()), sessionId);
        redisUtil.delete(RedisKeyConstants.userClientSession(
                session.getUserId(),
                session.getClientType().name(),
                session.getDeviceId()
        ));
    }

    /**
     * 查询当前活跃会话。
     *
     * @return 活跃会话列表
     */
    @Override
    public List<LoginSessionVO> listActiveSessions() {
        return redisUtil.scanKeys(RedisKeyConstants.loginSessionPattern(), REDIS_SCAN_COUNT)
                .stream()
                .map(key -> redisUtil.getObject(key, LoginSessionVO.class))
                .filter(session -> session != null)
                .sorted(Comparator.comparing(LoginSessionVO::getLastActiveTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /**
     * 解析登录请求中的客户端类型。
     *
     * @param clientType 客户端类型
     * @return 客户端类型
     */
    @Override
    public ClientType normalizeUserClientType(ClientType clientType) {
        if (clientType == null) {
            return inferCurrentRequestClientType();
        }
        return switch (clientType) {
            case WEB, ANDROID, IOS -> clientType;
            case ADMIN_WEB -> throw new BusinessException(ErrorCode.FORBIDDEN, "管理员账号请通过管理端登录");
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的客户端类型");
        };
    }

    private ClientType resolveUserClientType(LoginRequest request) {
        if (request == null) {
            return inferCurrentRequestClientType();
        }
        return normalizeUserClientType(request.getClientType());
    }

    private LoginSessionVO createSession(Long userId, String username, ClientType clientType, LoginRequest request,
                                         String deviceId, Duration ttl) {
        LocalDateTime now = LocalDateTime.now();
        LoginSessionVO session = new LoginSessionVO(
                UUID.randomUUID().toString(),
                userId,
                username,
                clientType,
                deviceId,
                request.getDeviceName(),
                request.getAppVersion(),
                currentIp(),
                currentUserAgent(),
                now,
                now,
                ttl.toSeconds()
        );
        redisUtil.set(RedisKeyConstants.loginSession(session.getSessionId()), session, ttl);
        redisUtil.setAdd(RedisKeyConstants.userSessions(userId), session.getSessionId());
        redisUtil.expire(RedisKeyConstants.userSessions(userId), ttl);
        redisUtil.set(RedisKeyConstants.userClientSession(userId, clientType.name(), deviceId),
                session.getSessionId(), ttl);
        return session;
    }

    private LoginSessionVO findSession(String sessionId) {
        return redisUtil.getObject(RedisKeyConstants.loginSession(sessionId), LoginSessionVO.class);
    }

    private String currentSessionId() {
        String tokenValue = StpUtil.getTokenValue();
        if (!StringUtils.hasText(tokenValue)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录令牌不存在");
        }
        String sessionId = redisUtil.getString(RedisKeyConstants.tokenSession(tokenValue));
        if (!StringUtils.hasText(sessionId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录会话已失效");
        }
        return sessionId;
    }

    private void deleteTokenSessionMappings(String sessionId) {
        Set<String> tokenSessionKeys = redisUtil.scanKeys(RedisKeyConstants.tokenSessionPattern(), REDIS_SCAN_COUNT);
        for (String tokenSessionKey : tokenSessionKeys) {
            String mappedSessionId = redisUtil.getString(tokenSessionKey);
            if (sessionId.equals(mappedSessionId)) {
                redisUtil.delete(tokenSessionKey);
            }
        }
    }

    private void revokeSessionsByClientType(Long userId, ClientType clientType) {
        Set<String> sessionIds = redisUtil.setMembers(RedisKeyConstants.userSessions(userId));
        for (String sessionId : sessionIds) {
            LoginSessionVO session = findSession(sessionId);
            if (session != null && clientType.equals(session.getClientType())) {
                revokeSession(sessionId);
            }
        }
    }

    private void revokeClientDeviceSession(Long userId, ClientType clientType, String deviceId) {
        String sessionId = redisUtil.getString(RedisKeyConstants.userClientSession(userId, clientType.name(), deviceId));
        if (StringUtils.hasText(sessionId)) {
            revokeSession(sessionId);
        }
    }

    private void refreshActiveTimeIfNecessary(LoginSessionVO session) {
        LocalDateTime lastActiveTime = session.getLastActiveTime();
        if (lastActiveTime != null
                && ChronoUnit.SECONDS.between(lastActiveTime, LocalDateTime.now()) < ACTIVE_UPDATE_INTERVAL_SECONDS) {
            return;
        }
        session.setLastActiveTime(LocalDateTime.now());
        Duration ttl = Duration.ofSeconds(Math.max(1L, session.getExpireSeconds()));
        redisUtil.set(RedisKeyConstants.loginSession(session.getSessionId()), session, ttl);
        expireCurrentTokenMapping(ttl);
        redisUtil.expire(RedisKeyConstants.userSessions(session.getUserId()), ttl);
        redisUtil.expire(RedisKeyConstants.userClientSession(
                session.getUserId(),
                session.getClientType().name(),
                session.getDeviceId()
        ), ttl);
    }

    private void expireCurrentTokenMapping(Duration ttl) {
        String tokenValue = StpUtil.getTokenValue();
        if (StringUtils.hasText(tokenValue)) {
            redisUtil.expire(RedisKeyConstants.tokenSession(tokenValue), ttl);
        }
    }

    private Duration tokenTtl(ClientType clientType) {
        return switch (clientType) {
            case ADMIN_WEB -> Duration.ofSeconds(systemSettingService.getAdminAccessTokenSeconds());
            case ANDROID, IOS -> Duration.ofSeconds(systemSettingService.getMobileAccessTokenSeconds());
            case WEB -> Duration.ofSeconds(systemSettingService.getWebAccessTokenSeconds());
            default -> Duration.ofSeconds(systemSettingService.getWebAccessTokenSeconds());
        };
    }

    private ClientType inferCurrentRequestClientType() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return ClientType.WEB;
        }
        ClientType headerClientType = parseClientType(request.getHeader(CLIENT_TYPE_HEADER));
        if (headerClientType != null) {
            return headerClientType;
        }
        String userAgent = request.getHeader("User-Agent");
        if (!StringUtils.hasText(userAgent)) {
            return ClientType.WEB;
        }
        String normalizedUserAgent = userAgent.toLowerCase(Locale.ROOT);
        if (normalizedUserAgent.contains("notaskflow-android") || normalizedUserAgent.contains("android")) {
            return ClientType.ANDROID;
        }
        if (normalizedUserAgent.contains("notaskflow-ios") || normalizedUserAgent.contains("iphone")
                || normalizedUserAgent.contains("ipad") || normalizedUserAgent.contains("ios")) {
            return ClientType.IOS;
        }
        return ClientType.WEB;
    }

    private ClientType parseClientType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            ClientType clientType = ClientType.valueOf(value.trim().toUpperCase(Locale.ROOT));
            return normalizeUserClientType(clientType);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String resolveDeviceId(LoginRequest request, ClientType clientType) {
        if (StringUtils.hasText(request.getDeviceId())) {
            return request.getDeviceId().trim();
        }
        return clientType.name().toLowerCase() + "-default";
    }

    private String currentIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return "";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String currentUserAgent() {
        HttpServletRequest request = currentRequest();
        return request == null ? "" : request.getHeader("User-Agent");
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }
}
