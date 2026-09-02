package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notaskflow.common.PageResponse;
import com.notaskflow.common.enums.UserStatus;
import com.notaskflow.config.AdminProperties;
import com.notaskflow.domain.dto.request.AdminUserPasswordResetRequest;
import com.notaskflow.domain.entity.User;
import com.notaskflow.domain.query.AdminUserQuery;
import com.notaskflow.domain.vo.AdminUserStatsVO;
import com.notaskflow.domain.vo.AdminUserVO;
import com.notaskflow.domain.vo.LoginSessionVO;
import com.notaskflow.exception.BusinessException;
import com.notaskflow.exception.ErrorCode;
import com.notaskflow.exception.ResourceNotFoundException;
import com.notaskflow.mapper.UserMapper;
import com.notaskflow.service.AdminUserService;
import com.notaskflow.service.LoginSessionService;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 管理端用户服务实现。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final LoginSessionService loginSessionService;

    private final AdminProperties adminProperties;

    /**
     * 分页查询用户。
     *
     * @param query 查询条件
     * @return 用户分页
     */
    @Override
    public PageResponse<AdminUserVO> page(AdminUserQuery query) {
        Page<User> page = new Page<>(query.safePageNum(), query.safePageSize());
        LambdaQueryWrapper<User> wrapper = buildQueryWrapper(query)
                .orderByDesc(User::getGmtCreate);
        Page<User> userPage = userMapper.selectPage(page, wrapper);
        Set<Long> onlineUserIds = onlineUserIds();
        return PageResponse.of(userPage, userPage.getRecords()
                .stream()
                .map(user -> toVO(user, onlineUserIds.contains(user.getId())))
                .toList());
    }

    /**
     * 查询用户统计。
     *
     * @return 用户统计
     */
    @Override
    public AdminUserStatsVO stats() {
        Long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<User>());
        Long todayNewUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .ge(User::getGmtCreate, LocalDate.now().atStartOfDay()));
        Long disabledUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, UserStatus.DISABLED));
        Long onlineUsers = (long) onlineUserIds().size();
        return new AdminUserStatsVO(totalUsers, todayNewUsers, disabledUsers, onlineUsers);
    }

    /**
     * 更新用户状态。
     *
     * @param userId 用户标识
     * @param status 用户状态
     * @return 用户视图对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO updateStatus(Long userId, UserStatus status) {
        User user = requireUser(userId);
        ensureOrdinaryUser(user);
        user.setStatus(status);
        userMapper.updateById(user);
        if (UserStatus.DISABLED.equals(status)) {
            loginSessionService.revokeUserSessions(userId);
        }
        log.info("管理端更新用户状态，userId={}, status={}", userId, status);
        return toVO(user, onlineUserIds().contains(userId));
    }

    /**
     * 重置用户密码。
     *
     * @param userId 用户标识
     * @param request 密码重置请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, AdminUserPasswordResetRequest request) {
        User user = requireUser(userId);
        ensureOrdinaryUser(user);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        loginSessionService.revokeUserSessions(userId);
        log.info("管理端重置用户密码，userId={}", userId);
    }

    /**
     * 删除用户。
     *
     * @param userId 用户标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        User user = requireUser(userId);
        ensureOrdinaryUser(user);
        String deletedUsername = "deleted_" + userId + "_" + System.currentTimeMillis();
        String deletedEmail = deletedUsername + "@deleted.local";
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getUsername, deletedUsername)
                .set(User::getEmail, deletedEmail)
                .set(User::getStatus, UserStatus.DISABLED)
                .set(User::getIsDeleted, true);
        userMapper.update(null, wrapper);
        loginSessionService.revokeUserSessions(userId);
        log.info("管理端删除用户，userId={}", userId);
    }

    /**
     * 构造用户查询条件。
     *
     * @param query 查询条件
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<User> buildQueryWrapper(AdminUserQuery query) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(nested -> nested.like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword)
                    .or()
                    .like(User::getEmail, keyword));
        }
        if (query.getStatus() != null) {
            wrapper.eq(User::getStatus, query.getStatus());
        }
        return wrapper;
    }

    /**
     * 查询用户并校验存在。
     *
     * @param userId 用户标识
     * @return 用户实体
     */
    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }
        return user;
    }

    /**
     * 阻止管理端账号被普通用户接口管理。
     *
     * @param user 用户实体
     */
    private void ensureOrdinaryUser(User user) {
        if (adminProperties.getUsername().equals(user.getUsername())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "管理员账号不能通过用户管理操作");
        }
    }

    /**
     * 查询在线普通用户标识集合。
     *
     * @return 用户标识集合
     */
    private Set<Long> onlineUserIds() {
        return loginSessionService.listActiveSessions()
                .stream()
                .map(LoginSessionVO::getUserId)
                .filter(userId -> userId != null)
                .collect(Collectors.toSet());
    }

    /**
     * 转换管理端用户视图对象。
     *
     * @param user 用户实体
     * @param online 是否在线
     * @return 管理端用户视图对象
     */
    private AdminUserVO toVO(User user, boolean online) {
        UserStatus status = user.getStatus() == null ? UserStatus.NORMAL : user.getStatus();
        return new AdminUserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getAvatarUrl(),
                status,
                online,
                user.getGmtCreate(),
                user.getGmtModified()
        );
    }
}
