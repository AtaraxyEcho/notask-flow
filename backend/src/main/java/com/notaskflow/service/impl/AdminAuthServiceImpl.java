package com.notaskflow.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.notaskflow.common.enums.ClientType;
import com.notaskflow.config.AdminProperties;
import com.notaskflow.domain.dto.request.LoginRequest;
import com.notaskflow.domain.dto.response.LoginResponse;
import com.notaskflow.domain.vo.AdminMeVO;
import com.notaskflow.domain.vo.LoginSessionVO;
import com.notaskflow.exception.BusinessException;
import com.notaskflow.exception.ErrorCode;
import com.notaskflow.service.AdminLogService;
import com.notaskflow.service.AdminAuthService;
import com.notaskflow.service.LoginSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 管理端认证服务实现。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final Long ADMIN_USER_ID = 0L;

    private static final String BCRYPT_PREFIX = "$2";

    private final AdminProperties adminProperties;

    private final PasswordEncoder passwordEncoder;

    private final LoginSessionService loginSessionService;

    private final AdminLogService adminLogService;

    /**
     * Administrator 登录管理平台。
     *
     * @param request 登录请求
     * @return 登录响应
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        if (!adminProperties.getUsername().equals(request.getAccount()) || !passwordMatches(request.getPassword())) {
            adminLogService.recordLogin(ADMIN_USER_ID, request.getAccount(), ClientType.ADMIN_WEB,
                    request.getDeviceId(), false, "管理员账号或密码错误");
            log.warn("管理端登录失败，account={}", request.getAccount());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "管理员账号或密码错误");
        }

        LoginSessionVO session = loginSessionService.createAdminSession(adminProperties.getUsername(), request);
        StpUtil.login(adminLoginId(), loginSessionService.buildLoginModel(session));
        loginSessionService.bindCurrentToken(session);
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        LoginResponse response = new LoginResponse(ADMIN_USER_ID, tokenInfo.getTokenName(), tokenInfo.getTokenValue(),
                tokenInfo.getTokenTimeout());
        response.setSessionId(session.getSessionId());
        response.setClientType(ClientType.ADMIN_WEB);
        adminLogService.recordLogin(ADMIN_USER_ID, request.getAccount(), ClientType.ADMIN_WEB,
                session.getDeviceId(), true, "");
        log.info("管理端登录完成，username={}", adminProperties.getUsername());
        return response;
    }

    /**
     * 管理端登出。
     */
    @Override
    public void logout() {
        loginSessionService.revokeCurrentSession();
        StpUtil.logout();
        log.info("管理端登出完成，username={}", adminProperties.getUsername());
    }

    /**
     * 获取当前管理端登录信息。
     *
     * @return 管理端登录信息
     */
    @Override
    public AdminMeVO me() {
        LoginSessionVO session = loginSessionService.currentSession();
        return new AdminMeVO(adminProperties.getUsername(), session.getClientType(), session.getSessionId());
    }

    private boolean passwordMatches(String rawPassword) {
        String configuredPassword = adminProperties.getPassword();
        if (configuredPassword != null && configuredPassword.startsWith(BCRYPT_PREFIX)) {
            return passwordEncoder.matches(rawPassword, configuredPassword);
        }
        return configuredPassword != null && configuredPassword.equals(rawPassword);
    }

    private String adminLoginId() {
        return "admin:" + adminProperties.getUsername();
    }
}
