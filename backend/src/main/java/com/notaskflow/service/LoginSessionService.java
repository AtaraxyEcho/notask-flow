package com.notaskflow.service;

import cn.dev33.satoken.stp.SaLoginModel;
import com.notaskflow.common.enums.ClientType;
import com.notaskflow.domain.dto.request.LoginRequest;
import com.notaskflow.domain.vo.LoginSessionVO;
import java.util.List;

/**
 * 登录会话服务，负责 Redis 会话中心的创建、校验和撤销。
 *
 * @author LIN
 */
public interface LoginSessionService {

    /**
     * 创建普通用户登录会话。
     *
     * @param userId 用户标识
     * @param username 用户名
     * @param request 登录请求
     * @return 登录会话
     */
    LoginSessionVO createUserSession(Long userId, String username, LoginRequest request);

    /**
     * 创建管理端登录会话。
     *
     * @param username 管理员账号
     * @param request 登录请求
     * @return 登录会话
     */
    LoginSessionVO createAdminSession(String username, LoginRequest request);

    /**
     * 根据会话信息构造 Sa-Token 登录模型。
     *
     * @param session 登录会话
     * @return 登录模型
     */
    SaLoginModel buildLoginModel(LoginSessionVO session);

    /**
     * 绑定当前请求令牌与会话。
     *
     * @param session 登录会话
     */
    void bindCurrentToken(LoginSessionVO session);

    /**
     * 校验当前请求的登录会话。
     *
     * @param adminApi 是否为管理端接口
     */
    void validateCurrentSession(boolean adminApi);

    /**
     * 获取当前请求绑定的会话。
     *
     * @return 登录会话
     */
    LoginSessionVO currentSession();

    /**
     * 撤销当前登录会话。
     */
    void revokeCurrentSession();

    /**
     * 撤销指定用户全部会话。
     *
     * @param userId 用户标识
     */
    void revokeUserSessions(Long userId);

    /**
     * 撤销指定会话。
     *
     * @param sessionId 会话标识
     */
    void revokeSession(String sessionId);

    /**
     * 查询当前活跃会话。
     *
     * @return 活跃会话列表
     */
    List<LoginSessionVO> listActiveSessions();

    /**
     * 解析登录请求中的客户端类型。
     *
     * @param clientType 客户端类型
     * @return 客户端类型
     */
    ClientType normalizeUserClientType(ClientType clientType);
}
