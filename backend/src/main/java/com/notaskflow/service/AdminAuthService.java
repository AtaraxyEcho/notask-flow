package com.notaskflow.service;

import com.notaskflow.domain.dto.request.LoginRequest;
import com.notaskflow.domain.dto.response.LoginResponse;
import com.notaskflow.domain.vo.AdminMeVO;

/**
 * 管理端认证服务。
 *
 * @author LIN
 */
public interface AdminAuthService {

    /**
     * Administrator 登录管理平台。
     *
     * @param request 登录请求
     * @return 登录响应
     */
    LoginResponse login(LoginRequest request);

    /**
     * 管理端登出。
     */
    void logout();

    /**
     * 获取当前管理端登录信息。
     *
     * @return 管理端登录信息
     */
    AdminMeVO me();
}
