package com.notaskflow.service;

import com.notaskflow.common.PageResponse;
import com.notaskflow.common.enums.UserStatus;
import com.notaskflow.domain.dto.request.AdminUserPasswordResetRequest;
import com.notaskflow.domain.query.AdminUserQuery;
import com.notaskflow.domain.vo.AdminUserStatsVO;
import com.notaskflow.domain.vo.AdminUserVO;

/**
 * 管理端用户服务。
 *
 * @author LIN
 */
public interface AdminUserService {

    /**
     * 分页查询用户。
     *
     * @param query 查询条件
     * @return 用户分页
     */
    PageResponse<AdminUserVO> page(AdminUserQuery query);

    /**
     * 查询用户统计。
     *
     * @return 用户统计
     */
    AdminUserStatsVO stats();

    /**
     * 更新用户状态。
     *
     * @param userId 用户标识
     * @param status 用户状态
     * @return 用户视图对象
     */
    AdminUserVO updateStatus(Long userId, UserStatus status);

    /**
     * 重置用户密码。
     *
     * @param userId 用户标识
     * @param request 密码重置请求
     */
    void resetPassword(Long userId, AdminUserPasswordResetRequest request);

    /**
     * 删除用户。
     *
     * @param userId 用户标识
     */
    void deleteUser(Long userId);
}
