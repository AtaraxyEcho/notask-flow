package com.notaskflow.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.notaskflow.common.enums.ClientType;
import com.notaskflow.common.constant.RedisKeyConstants;
import com.notaskflow.common.enums.BusinessType;
import com.notaskflow.common.enums.JoinRequestStatus;
import com.notaskflow.common.enums.NotificationType;
import com.notaskflow.common.enums.RegisterTeamMode;
import com.notaskflow.common.enums.RoleCode;
import com.notaskflow.common.enums.SpaceType;
import com.notaskflow.common.enums.UserStatus;
import com.notaskflow.config.AdminProperties;
import com.notaskflow.domain.dto.request.ForgotPasswordRequest;
import com.notaskflow.domain.dto.request.LoginRequest;
import com.notaskflow.domain.dto.request.RegisterRequest;
import com.notaskflow.domain.dto.request.ResetPasswordRequest;
import com.notaskflow.domain.dto.request.SendRegisterEmailCodeRequest;
import com.notaskflow.domain.dto.request.VerifyResetCodeRequest;
import com.notaskflow.domain.dto.response.LoginResponse;
import com.notaskflow.domain.dto.response.PasswordResetVerifyResponse;
import com.notaskflow.domain.entity.Role;
import com.notaskflow.domain.entity.Space;
import com.notaskflow.domain.entity.SpaceJoinRequest;
import com.notaskflow.domain.entity.SpaceMember;
import com.notaskflow.domain.entity.User;
import com.notaskflow.domain.vo.UserProfileVO;
import com.notaskflow.domain.vo.LoginSessionVO;
import com.notaskflow.event.NotificationCreateEvent;
import com.notaskflow.exception.BusinessException;
import com.notaskflow.exception.ErrorCode;
import com.notaskflow.exception.ResourceNotFoundException;
import com.notaskflow.mapper.RoleMapper;
import com.notaskflow.mapper.SpaceJoinRequestMapper;
import com.notaskflow.mapper.SpaceMapper;
import com.notaskflow.mapper.SpaceMemberMapper;
import com.notaskflow.mapper.UserMapper;
import com.notaskflow.service.AdminLogService;
import com.notaskflow.service.AuthService;
import com.notaskflow.service.LoginSessionService;
import com.notaskflow.service.MailNotificationService;
import com.notaskflow.service.SpaceInviteService;
import com.notaskflow.service.SpaceMemberPresenceService;
import com.notaskflow.service.SystemSettingService;
import com.notaskflow.utils.AvatarUrlUtil;
import com.notaskflow.utils.RedisUtil;
import com.notaskflow.utils.StringGenerator;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 认证服务实现，处理注册、登录、登出和密码找回等能力。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int REGISTER_EMAIL_CODE_LENGTH = 6;

    private static final int PASSWORD_RESET_CODE_LENGTH = 6;

    private static final int PASSWORD_RESET_TOKEN_LENGTH = 48;

    private static final Duration REGISTER_EMAIL_CODE_TTL = Duration.ofMinutes(10);

    private static final Duration PASSWORD_RESET_CODE_TTL = Duration.ofMinutes(10);

    private static final Duration PASSWORD_RESET_TOKEN_TTL = Duration.ofMinutes(15);

    private static final int EMAIL_CODE_RATE_LIMIT = 5;

    private static final Duration EMAIL_CODE_RATE_LIMIT_WINDOW = Duration.ofMinutes(5);

    private final UserMapper userMapper;

    private final RoleMapper roleMapper;

    private final SpaceMapper spaceMapper;

    private final SpaceMemberMapper spaceMemberMapper;

    private final SpaceJoinRequestMapper spaceJoinRequestMapper;

    private final PasswordEncoder passwordEncoder;

    private final SpaceInviteService spaceInviteService;

    private final MailNotificationService mailNotificationService;

    private final SpaceMemberPresenceService spaceMemberPresenceService;

    private final RedisUtil redisUtil;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final SystemSettingService systemSettingService;

    private final LoginSessionService loginSessionService;

    private final AdminLogService adminLogService;

    private final AdminProperties adminProperties;

    /**
     * 发送注册邮箱验证码。
     *
     * @param request 发送注册邮箱验证码请求
     */
    @Override
    public void sendRegisterEmailCode(SendRegisterEmailCodeRequest request) {
        ensureRegistrationEnabled();
        String normalizedEmail = normalizeEmail(request.getEmail());
        redisUtil.limit(
                RedisKeyConstants.rateLimit("register-email-code", normalizedEmail),
                EMAIL_CODE_RATE_LIMIT,
                EMAIL_CODE_RATE_LIMIT_WINDOW,
                "验证码发送过于频繁，请稍后再试"
        );
        Long emailCount = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getEmail, normalizedEmail));
        if (emailCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该邮箱已被注册");
        }

        String code = StringGenerator.randomNumeric(REGISTER_EMAIL_CODE_LENGTH);
        String codeKey = buildRegisterEmailCodeKey(normalizedEmail);
        redisUtil.delete(codeKey);
        redisUtil.set(codeKey, code, REGISTER_EMAIL_CODE_TTL);

        boolean mailSent = mailNotificationService.sendRegisterCodeMail(normalizedEmail, code);
        if (!mailSent) {
            redisUtil.delete(codeKey);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "邮件服务不可用，请稍后再试");
        }
        log.info("注册邮箱验证码发送完成，email={}, expireMinutes={}", normalizedEmail, REGISTER_EMAIL_CODE_TTL.toMinutes());
    }

    /**
     * 注册用户并创建个人空间。
     *
     * @param request 注册请求
     * @return 用户资料
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO register(RegisterRequest request) {
        ensureRegistrationEnabled();
        String normalizedEmail = normalizeEmail(request.getEmail());
        validateUniqueUser(request.getUsername(), normalizedEmail);
        validateRegisterEmailCodeIfNecessary(normalizedEmail, request.getEmailCode());

        Role ownerRole = findRoleByCode(RoleCode.SPACE_OWNER.getCode());
        User user = new User();
        user.setUsername(request.getUsername());
        user.setNickname(resolveNickname(request.getNickname(), request.getUsername()));
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.NORMAL);
        userMapper.insert(user);

        Space personalSpace = createOwnedSpace(user.getId(), "个人空间", SpaceType.PERSONAL, ownerRole);
        handleTeamRegistration(request, user, ownerRole);
        log.info("用户注册完成，userId={}, personalSpaceId={}", user.getId(), personalSpace.getId());
        return toUserProfileVO(user);
    }

    /**
     * 发送找回密码验证码。
     *
     * @param request 忘记密码请求
     */
    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        redisUtil.limit(
                RedisKeyConstants.rateLimit("password-reset-code", normalizedEmail),
                EMAIL_CODE_RATE_LIMIT,
                EMAIL_CODE_RATE_LIMIT_WINDOW,
                "验证码发送过于频繁，请稍后再试"
        );
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getEmail, normalizedEmail));
        if (user == null) {
            log.info("忘记密码请求已接收，但邮箱未注册，email={}", request.getEmail());
            return;
        }

        String code = StringGenerator.randomNumeric(PASSWORD_RESET_CODE_LENGTH);
        String codeKey = buildPasswordResetCodeKey(normalizedEmail);
        redisUtil.delete(codeKey);
        clearPasswordResetTokenByEmail(normalizedEmail);
        redisUtil.set(codeKey, code, PASSWORD_RESET_CODE_TTL);

        boolean mailSent = mailNotificationService.sendPasswordResetCodeMail(user.getEmail(), code);
        if (!mailSent) {
            redisUtil.delete(codeKey);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "邮件服务不可用，请稍后再试");
        }
        log.info("密码重置验证码发送完成，userId={}, email={}, expireMinutes={}",
                user.getId(), user.getEmail(), PASSWORD_RESET_CODE_TTL.toMinutes());
    }

    /**
     * 校验找回密码验证码。
     *
     * @param request 校验验证码请求
     * @return 重置凭证
     */
    @Override
    public PasswordResetVerifyResponse verifyResetCode(VerifyResetCodeRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        String cachedCode = redisUtil.getString(buildPasswordResetCodeKey(normalizedEmail));
        if (!StringUtils.hasText(cachedCode) || !cachedCode.equals(request.getCode().trim())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码错误或已过期");
        }

        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getEmail, normalizedEmail));
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }

        redisUtil.delete(buildPasswordResetCodeKey(normalizedEmail));
        clearPasswordResetTokenByEmail(normalizedEmail);

        String resetToken = StringGenerator.randomAlphanumeric(PASSWORD_RESET_TOKEN_LENGTH);
        redisUtil.set(buildPasswordResetTokenKey(resetToken), String.valueOf(user.getId()),
                PASSWORD_RESET_TOKEN_TTL);
        redisUtil.set(buildPasswordResetEmailTokenKey(normalizedEmail), resetToken,
                PASSWORD_RESET_TOKEN_TTL);
        log.info("密码重置验证码校验通过，userId={}, email={}", user.getId(), user.getEmail());
        return new PasswordResetVerifyResponse(resetToken, PASSWORD_RESET_TOKEN_TTL.toSeconds());
    }

    /**
     * 使用重置凭证设置新密码。
     *
     * @param request 重置密码请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入的新密码不一致");
        }

        String key = buildPasswordResetTokenKey(request.getResetToken().trim());
        String userIdValue = redisUtil.getString(key);
        if (!StringUtils.hasText(userIdValue)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "重置凭证不存在或已过期");
        }

        User user = userMapper.selectById(Long.valueOf(userIdValue));
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        redisUtil.delete(key);
        clearPasswordResetTokenByEmail(normalizeEmail(user.getEmail()));
        loginSessionService.revokeUserSessions(user.getId());
        StpUtil.logout(user.getId());
        log.info("用户密码重置完成，userId={}", user.getId());
    }

    /**
     * 校验账号密码并签发登录令牌。
     *
     * @param request 登录请求
     * @return 登录响应
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        if (adminProperties.getUsername().equals(request.getAccount())) {
            adminLogService.recordLogin(null, request.getAccount(), request.getClientType(), request.getDeviceId(),
                    false, "管理员账号通过普通端登录");
            throw new BusinessException(ErrorCode.FORBIDDEN, "管理员账号请通过管理端登录");
        }
        String account = request.getAccount() == null ? "" : request.getAccount().trim().toLowerCase(Locale.ROOT);
        String loginLimitKey = RedisKeyConstants.rateLimit("login", account);
        redisUtil.checkFailureLimit(
                loginLimitKey,
                systemSettingService.getLoginFailureLimit(),
                systemSettingService.getLoginFailureWindow(),
                "登录尝试过于频繁，请稍后再试"
        );
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .and(wrapper -> wrapper.eq(User::getUsername, request.getAccount())
                        .or()
                        .eq(User::getEmail, request.getAccount())));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            adminLogService.recordLogin(null, request.getAccount(), request.getClientType(), request.getDeviceId(),
                    false, "账号或密码错误");
            redisUtil.recordLimitedFailure(
                    loginLimitKey,
                    systemSettingService.getLoginFailureLimit(),
                    systemSettingService.getLoginFailureWindow(),
                    "登录失败次数过多，请稍后再试");
            log.warn("用户登录失败，account={}", request.getAccount());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        if (UserStatus.DISABLED.equals(user.getStatus())) {
            adminLogService.recordLogin(user.getId(), request.getAccount(), request.getClientType(), request.getDeviceId(),
                    false, "账号已被禁用");
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }
        LoginSessionVO session = loginSessionService.createUserSession(user.getId(), user.getUsername(), request);
        StpUtil.login(user.getId(), loginSessionService.buildLoginModel(session));
        loginSessionService.bindCurrentToken(session);
        redisUtil.delete(loginLimitKey);
        adminLogService.recordLogin(user.getId(), request.getAccount(), session.getClientType(), session.getDeviceId(),
                true, "");
        log.info("用户登录完成，userId={}", user.getId());
        return buildLoginResponse(user.getId(), session);
    }

    /**
     * 注销当前登录会话。
     */
    @Override
    public void logout() {
        Long currentUserId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        if (currentUserId != null) {
            spaceMemberPresenceService.clearUserOnline(currentUserId);
            loginSessionService.revokeCurrentSession();
        }
        StpUtil.logout();
        log.info("用户登出完成，userId={}", currentUserId);
    }

    /**
     * 刷新当前登录响应中的令牌信息。
     *
     * @return 登录响应
     */
    @Override
    public LoginResponse refresh() {
        StpUtil.checkLogin();
        Long currentUserId = StpUtil.getLoginIdAsLong();
        LoginSessionVO session = loginSessionService.currentSession();
        log.debug("刷新登录令牌信息，userId={}", currentUserId);
        return buildLoginResponse(currentUserId, session);
    }

    /**
     * 处理注册时选择的团队能力。
     *
     * @param request 注册请求
     * @param user 注册用户
     * @param ownerRole 空间所有者角色
     */
    private void handleTeamRegistration(RegisterRequest request, User user, Role ownerRole) {
        RegisterTeamMode teamMode = request.getTeamMode() == null ? RegisterTeamMode.PERSONAL_ONLY
                : request.getTeamMode();
        switch (teamMode) {
            case PERSONAL_ONLY:
                break;
            case CREATE_TEAM:
                createOwnedSpace(user.getId(), requireTeamName(request.getTeamName()), SpaceType.TEAM, ownerRole);
                break;
            case APPLY_SUPERVISOR:
                createJoinRequest(request, user);
                break;
            case JOIN_INVITE_CODE:
                joinTeamByInviteCode(request, user);
                break;
            default:
                throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的团队注册方式");
        }
    }

    /**
     * 创建用户拥有的空间和成员关系。
     *
     * @param userId 用户标识
     * @param spaceName 空间名称
     * @param spaceType 空间类型
     * @param ownerRole 空间所有者角色
     * @return 空间实体
     */
    private Space createOwnedSpace(Long userId, String spaceName, SpaceType spaceType, Role ownerRole) {
        Space space = new Space();
        space.setName(spaceName);
        space.setType(spaceType);
        space.setOwnerUserId(userId);
        space.setJoinApprovalRequired(SpaceType.TEAM.equals(spaceType)
                && systemSettingService.isNewTeamJoinApprovalRequired());
        spaceMapper.insert(space);

        SpaceMember member = new SpaceMember();
        member.setSpaceId(space.getId());
        member.setUserId(userId);
        member.setRoleId(ownerRole.getId());
        spaceMemberMapper.insert(member);
        return space;
    }

    /**
     * 校验并返回团队名称。
     *
     * @param teamName 团队名称
     * @return 团队名称
     */
    private String requireTeamName(String teamName) {
        if (!StringUtils.hasText(teamName)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "团队名称不能为空");
        }
        return teamName.trim();
    }

    /**
     * 解析注册昵称。
     *
     * @param nickname 昵称
     * @param username 用户名
     * @return 规范化后的昵称
     */
    private String resolveNickname(String nickname, String username) {
        return StringUtils.hasText(nickname) ? nickname.trim() : username;
    }

    /**
     * 根据上级账号创建团队加入申请。
     *
     * @param request 注册请求
     * @param user 注册用户
     */
    private void createJoinRequest(RegisterRequest request, User user) {
        if (!StringUtils.hasText(request.getSupervisorAccount())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上级账号不能为空");
        }
        User supervisor = findUserByAccount(request.getSupervisorAccount());
        if (user.getId().equals(supervisor.getId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上级账号不能为当前用户");
        }

        Long pendingCount = spaceJoinRequestMapper.selectCount(Wrappers.<SpaceJoinRequest>lambdaQuery()
                .eq(SpaceJoinRequest::getApplicantUserId, user.getId())
                .eq(SpaceJoinRequest::getSupervisorUserId, supervisor.getId())
                .eq(SpaceJoinRequest::getStatus, JoinRequestStatus.PENDING));
        if (pendingCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "已存在待审批的团队加入申请");
        }

        SpaceJoinRequest joinRequest = new SpaceJoinRequest();
        joinRequest.setApplicantUserId(user.getId());
        joinRequest.setSupervisorUserId(supervisor.getId());
        joinRequest.setTeamName(request.getTeamName());
        joinRequest.setTargetSpaceId(resolveJoinTargetSpaceId(supervisor.getId(), request.getTeamName()));
        joinRequest.setStatus(JoinRequestStatus.PENDING);
        joinRequest.setRemark(request.getTeamApplyRemark());
        spaceJoinRequestMapper.insert(joinRequest);
        publishJoinRequestNotification(supervisor, user, joinRequest);
        log.info("注册时创建团队加入申请，requestId={}, applicantUserId={}, supervisorUserId={}",
                joinRequest.getId(), user.getId(), supervisor.getId());
    }

    /**
     * 解析注册申请的目标团队空间。
     *
     * @param supervisorUserId 审批人标识
     * @param teamName 团队名称
     * @return 目标团队空间标识，多团队且未指定名称时返回null
     */
    private Long resolveJoinTargetSpaceId(Long supervisorUserId, String teamName) {
        if (StringUtils.hasText(teamName)) {
            Long targetSpaceId = resolveJoinTargetSpaceIdByName(supervisorUserId, teamName.trim());
            if (targetSpaceId == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "目标团队不存在或上级无管理权限");
            }
            return targetSpaceId;
        }

        List<Space> manageableSpaces = findManageableTeamSpaces(supervisorUserId);
        if (manageableSpaces.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该上级账号暂无可审批的团队空间");
        }
        if (manageableSpaces.size() == 1) {
            return manageableSpaces.get(0).getId();
        }
        return null;
    }

    /**
     * 按名称解析注册申请的目标团队空间。
     *
     * @param supervisorUserId 审批人标识
     * @param teamName 团队名称
     * @return 目标团队空间标识
     */
    private Long resolveJoinTargetSpaceIdByName(Long supervisorUserId, String teamName) {
        List<Space> spaces = spaceMapper.selectList(Wrappers.<Space>lambdaQuery()
                .eq(Space::getType, SpaceType.TEAM)
                .eq(Space::getName, teamName));
        return spaces.stream()
                .filter(space -> isSpaceOwnerOrAdmin(space, supervisorUserId))
                .map(Space::getId)
                .findFirst()
                .orElse(null);
    }

    /**
     * 查询用户可管理的团队空间。
     *
     * @param userId 用户标识
     * @return 可管理团队空间
     */
    private List<Space> findManageableTeamSpaces(Long userId) {
        return spaceMapper.selectList(Wrappers.<Space>lambdaQuery()
                        .eq(Space::getType, SpaceType.TEAM))
                .stream()
                .filter(space -> isSpaceOwnerOrAdmin(space, userId))
                .toList();
    }

    /**
     * 判断用户是否为空间所有者或管理员。
     *
     * @param space 空间实体
     * @param userId 用户标识
     * @return 是否拥有管理权限
     */
    private boolean isSpaceOwnerOrAdmin(Space space, Long userId) {
        if (userId.equals(space.getOwnerUserId())) {
            return true;
        }
        SpaceMember member = spaceMemberMapper.selectOne(Wrappers.<SpaceMember>lambdaQuery()
                .eq(SpaceMember::getSpaceId, space.getId())
                .eq(SpaceMember::getUserId, userId));
        if (member == null) {
            return false;
        }
        Role role = roleMapper.selectById(member.getRoleId());
        return role != null && (RoleCode.SPACE_OWNER.getCode().equals(role.getCode())
                || RoleCode.SPACE_ADMIN.getCode().equals(role.getCode()));
    }

    /**
     * 发布注册时团队加入申请通知。
     *
     * @param supervisor 审批人
     * @param applicant 申请人
     * @param joinRequest 加入申请
     */
    private void publishJoinRequestNotification(User supervisor, User applicant, SpaceJoinRequest joinRequest) {
        String applicantName = resolveDisplayName(applicant);
        String teamName = joinRequest.getTeamName() == null ? "" : joinRequest.getTeamName().trim();
        String content = teamName.isEmpty()
                ? applicantName + " 提交了一条团队加入申请，请及时处理。"
                : applicantName + " 申请加入团队「" + teamName + "」，请及时处理。";
        applicationEventPublisher.publishEvent(new NotificationCreateEvent(
                supervisor.getId(),
                NotificationType.SPACE_JOIN_APPLIED,
                BusinessType.SPACE_JOIN_REQUEST,
                joinRequest.getId(),
                "新的团队加入申请",
                content
        ));
    }

    /**
     * 解析用户显示名称。
     *
     * @param user 用户实体
     * @return 显示名称
     */
    private String resolveDisplayName(User user) {
        if (user == null) {
            return "";
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        return user.getUsername();
    }

    /**
     * 注册时使用邀请码加入团队。
     *
     * @param request 注册请求
     * @param user 注册用户
     */
    private void joinTeamByInviteCode(RegisterRequest request, User user) {
        if (!StringUtils.hasText(request.getInviteCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邀请码不能为空");
        }
        spaceInviteService.joinByCode(user.getId(), request.getInviteCode());
    }

    /**
     * 校验用户名和邮箱是否已存在。
     *
     * @param username 用户名
     * @param email 邮箱
     */
    private void validateUniqueUser(String username, String email) {
        Long count = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .and(wrapper -> wrapper.eq(User::getUsername, username)
                        .or()
                        .eq(User::getEmail, email)));
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名或邮箱已存在");
        }
    }

    /**
     * 校验注册邮箱验证码。
     *
     * @param email 规范化邮箱
     * @param emailCode 邮箱验证码
     */
    private void validateRegisterEmailCodeIfNecessary(String email, String emailCode) {
        boolean verificationRequired = systemSettingService.isRegisterEmailVerificationRequired();
        if (!verificationRequired) {
            return;
        }
        if (!StringUtils.hasText(emailCode) || !emailCode.trim().matches("\\d{6}")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邮箱验证码必须为6位数字");
        }
        String cachedCode = redisUtil.getString(buildRegisterEmailCodeKey(email));
        if (!StringUtils.hasText(cachedCode) || !cachedCode.equals(emailCode.trim())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邮箱验证码错误或已过期");
        }
        redisUtil.delete(buildRegisterEmailCodeKey(email));
    }

    private void ensureRegistrationEnabled() {
        if (!systemSettingService.isRegistrationEnabled()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "系统暂未开放新用户注册");
        }
    }

    /**
     * 根据角色编码查询角色。
     *
     * @param roleCode 角色编码
     * @return 角色实体
     */
    private Role findRoleByCode(String roleCode) {
        Role role = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getCode, roleCode));
        if (role == null) {
            throw new ResourceNotFoundException("系统角色不存在: " + roleCode);
        }
        return role;
    }

    /**
     * 根据用户名或邮箱查询用户。
     *
     * @param account 用户名或邮箱
     * @return 用户实体
     */
    private User findUserByAccount(String account) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .and(wrapper -> wrapper.eq(User::getUsername, account)
                        .or()
                        .eq(User::getEmail, account)));
        if (user == null) {
            throw new ResourceNotFoundException("上级账号不存在");
        }
        return user;
    }

    /**
     * 构造登录响应。
     *
     * @param userId 用户标识
     * @return 登录响应
     */
    private LoginResponse buildLoginResponse(Long userId, LoginSessionVO session) {
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        LoginResponse response = new LoginResponse(userId, tokenInfo.getTokenName(), tokenInfo.getTokenValue(),
                tokenInfo.getTokenTimeout());
        response.setSessionId(session.getSessionId());
        response.setClientType(session.getClientType() == null ? ClientType.WEB : session.getClientType());
        return response;
    }

    /**
     * 构造注册邮箱验证码缓存键。
     *
     * @param email 规范化邮箱
     * @return Redis 键
     */
    private String buildRegisterEmailCodeKey(String email) {
        return RedisKeyConstants.REGISTER_EMAIL_CODE_PREFIX + email;
    }

    /**
     * 构造密码重置验证码缓存键。
     *
     * @param email 规范化邮箱
     * @return Redis 键
     */
    private String buildPasswordResetCodeKey(String email) {
        return RedisKeyConstants.PASSWORD_RESET_CODE_PREFIX + email;
    }

    /**
     * 构造密码重置凭证缓存键。
     *
     * @param resetToken 重置凭证
     * @return Redis 键
     */
    private String buildPasswordResetTokenKey(String resetToken) {
        return RedisKeyConstants.PASSWORD_RESET_TOKEN_PREFIX + resetToken;
    }

    /**
     * 构造邮箱与重置凭证映射缓存键。
     *
     * @param email 规范化邮箱
     * @return Redis 键
     */
    private String buildPasswordResetEmailTokenKey(String email) {
        return RedisKeyConstants.PASSWORD_RESET_EMAIL_TOKEN_PREFIX + email;
    }

    /**
     * 清理指定邮箱现有的重置凭证。
     *
     * @param email 规范化邮箱
     */
    private void clearPasswordResetTokenByEmail(String email) {
        String emailTokenKey = buildPasswordResetEmailTokenKey(email);
        String existingResetToken = redisUtil.getString(emailTokenKey);
        if (StringUtils.hasText(existingResetToken)) {
            redisUtil.delete(buildPasswordResetTokenKey(existingResetToken));
        }
        redisUtil.delete(emailTokenKey);
    }

    /**
     * 规范化邮箱。
     *
     * @param email 原始邮箱
     * @return 规范化邮箱
     */
    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 转换用户资料视图对象。
     *
     * @param user 用户实体
     * @return 用户资料
     */
    private UserProfileVO toUserProfileVO(User user) {
        return new UserProfileVO(user.getId(), user.getUsername(), user.getNickname(), user.getEmail(),
                AvatarUrlUtil.proxyUrl(user), user.getGmtCreate());
    }
}
