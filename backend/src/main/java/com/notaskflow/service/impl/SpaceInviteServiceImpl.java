package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notaskflow.common.enums.RoleCode;
import com.notaskflow.common.enums.SpaceType;
import com.notaskflow.domain.dto.cache.SpaceInviteCachePayload;
import com.notaskflow.domain.dto.request.SpaceInviteCreateRequest;
import com.notaskflow.domain.entity.Role;
import com.notaskflow.domain.entity.Space;
import com.notaskflow.domain.entity.SpaceMember;
import com.notaskflow.domain.entity.User;
import com.notaskflow.domain.vo.SpaceInvitePreviewVO;
import com.notaskflow.domain.vo.SpaceInviteVO;
import com.notaskflow.domain.vo.SpaceMemberVO;
import com.notaskflow.exception.BusinessException;
import com.notaskflow.exception.ErrorCode;
import com.notaskflow.exception.ResourceNotFoundException;
import com.notaskflow.mapper.RoleMapper;
import com.notaskflow.mapper.SpaceMapper;
import com.notaskflow.mapper.SpaceMemberMapper;
import com.notaskflow.mapper.UserMapper;
import com.notaskflow.security.PermissionValidator;
import com.notaskflow.service.SpaceInviteService;
import com.notaskflow.utils.LoginUserUtil;
import com.notaskflow.utils.RedisUtil;
import com.notaskflow.utils.StringGenerator;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 团队邀请码服务实现，使用 Redis 保存短期有效的邀请码。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpaceInviteServiceImpl implements SpaceInviteService {

    private static final String INVITE_KEY_PREFIX = "notask:space-invite:";

    private static final int CODE_LENGTH = 10;

    private static final int MAX_GENERATE_RETRY = 5;

    private final RedisUtil redisUtil;

    private final ObjectMapper objectMapper;

    private final SpaceMapper spaceMapper;

    private final SpaceMemberMapper spaceMemberMapper;

    private final RoleMapper roleMapper;

    private final UserMapper userMapper;

    private final PermissionValidator permissionValidator;

    @Value("${notask-flow.invite.default-expire-minutes:30}")
    private Integer defaultExpireMinutes;

    /**
     * 创建团队邀请码。
     *
     * @param spaceId 空间标识
     * @param request 创建请求
     * @return 邀请码信息
     */
    @Override
    public SpaceInviteVO createInvite(Long spaceId, SpaceInviteCreateRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceAdminOrOwner(spaceId, currentUserId);
        Space space = findSpace(spaceId);
        ensureTeamSpace(space);
        Role role = findJoinableRole(request.getRoleCode());
        int expireMinutes = request.getExpireMinutes() == null ? defaultExpireMinutes : request.getExpireMinutes();
        Duration ttl = Duration.ofMinutes(expireMinutes);
        SpaceInviteCachePayload payload = new SpaceInviteCachePayload(spaceId, role.getCode(), currentUserId);
        String code = createUniqueCode(payload, ttl);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expireMinutes);
        log.info("团队邀请码创建完成，spaceId={}，roleCode={}，operatorId={}，expireMinutes={}",
                spaceId, role.getCode(), currentUserId, expireMinutes);
        return new SpaceInviteVO(code, spaceId, role.getCode(), expiresAt);
    }

    /**
     * 预览团队邀请码对应的信息。
     *
     * @param code 邀请码
     * @return 邀请码预览信息
     */
    @Override
    public SpaceInvitePreviewVO preview(String code) {
        String normalizedCode = normalizeCode(code);
        SpaceInviteCachePayload payload = readPayload(normalizedCode);
        Space space = findSpace(payload.getSpaceId());
        ensureTeamSpace(space);
        User owner = userMapper.selectById(space.getOwnerUserId());
        Long memberCount = spaceMemberMapper.selectCount(Wrappers.<SpaceMember>lambdaQuery()
                .eq(SpaceMember::getSpaceId, space.getId()));
        Long expireSeconds = redisUtil.getExpireSeconds(buildKey(normalizedCode));
        LocalDateTime expiresAt = expireSeconds == null || expireSeconds < 0
                ? null
                : LocalDateTime.now().plusSeconds(expireSeconds);
        return new SpaceInvitePreviewVO(normalizedCode, space.getId(), space.getName(),
                owner == null ? "" : owner.getUsername(), payload.getRoleCode(), memberCount, expiresAt);
    }

    /**
     * 当前登录用户使用邀请码加入空间。
     *
     * @param code 邀请码
     * @return 新增成员信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpaceMemberVO joinByCode(String code) {
        return joinByCode(LoginUserUtil.currentUserId(), code);
    }

    /**
     * 指定用户使用邀请码加入空间。
     *
     * @param userId 用户标识
     * @param code 邀请码
     * @return 新增成员信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpaceMemberVO joinByCode(Long userId, String code) {
        SpaceInviteCachePayload payload = readPayload(code);
        Space space = findSpace(payload.getSpaceId());
        ensureTeamSpace(space);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }
        Role role = findJoinableRole(payload.getRoleCode());
        SpaceMember exists = permissionValidator.findSpaceMember(payload.getSpaceId(), userId);
        if (exists != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户已是空间成员");
        }
        SpaceMember member = new SpaceMember();
        member.setSpaceId(payload.getSpaceId());
        member.setUserId(userId);
        member.setRoleId(role.getId());
        spaceMemberMapper.insert(member);
        log.info("用户通过邀请码加入团队，spaceId={}，userId={}，roleCode={}",
                payload.getSpaceId(), userId, role.getCode());
        return toSpaceMemberVO(member);
    }

    /**
     * 生成未被占用的邀请码。
     *
     * @param payload 邀请码负载
     * @param ttl 有效期
     * @return 邀请码
     */
    private String createUniqueCode(SpaceInviteCachePayload payload, Duration ttl) {
        String json = writePayload(payload);
        for (int index = 0; index < MAX_GENERATE_RETRY; index++) {
            String code = StringGenerator.randomAlphanumeric(CODE_LENGTH);
            boolean created = redisUtil.setIfAbsent(buildKey(code), json, ttl);
            if (created) {
                return code;
            }
        }
        throw new BusinessException(ErrorCode.CONFLICT, "邀请码生成冲突，请重试");
    }

    /**
     * 序列化邀请码负载。
     *
     * @param payload 邀请码负载
     * @return JSON 字符串
     */
    private String writePayload(SpaceInviteCachePayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            log.error("团队邀请码负载序列化失败，spaceId={}", payload.getSpaceId(), exception);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "邀请码生成失败");
        }
    }

    /**
     * 读取邀请码负载。
     *
     * @param code 邀请码
     * @return 邀请码负载
     */
    private SpaceInviteCachePayload readPayload(String code) {
        String normalizedCode = normalizeCode(code);
        String json = redisUtil.getString(buildKey(normalizedCode));
        if (!StringUtils.hasText(json)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "邀请码不存在或已过期");
        }
        try {
            return objectMapper.readValue(json, SpaceInviteCachePayload.class);
        } catch (JsonProcessingException exception) {
            log.error("团队邀请码负载解析失败，code={}", normalizedCode, exception);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "邀请码解析失败");
        }
    }

    /**
     * 规范化邀请码。
     *
     * @param code 邀请码
     * @return 去除空白后的邀请码
     */
    private String normalizeCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邀请码不能为空");
        }
        return code.trim();
    }

    /**
     * 构造 Redis 键名。
     *
     * @param code 邀请码
     * @return Redis 键名
     */
    private String buildKey(String code) {
        return INVITE_KEY_PREFIX + code;
    }

    /**
     * 查询空间实体。
     *
     * @param spaceId 空间标识
     * @return 空间实体
     */
    private Space findSpace(Long spaceId) {
        Space space = spaceMapper.selectById(spaceId);
        if (space == null) {
            throw new ResourceNotFoundException("空间不存在");
        }
        return space;
    }

    /**
     * 校验空间是否为团队空间。
     *
     * @param space 空间实体
     */
    private void ensureTeamSpace(Space space) {
        if (!SpaceType.TEAM.equals(space.getType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邀请码仅支持团队空间");
        }
    }

    /**
     * 查询可通过邀请加入的角色。
     *
     * @param roleCode 角色编码
     * @return 角色实体
     */
    private Role findJoinableRole(String roleCode) {
        if (RoleCode.SPACE_OWNER.getCode().equals(roleCode)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不可通过邀请码授予空间所有者角色");
        }
        Role role = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getCode, roleCode));
        if (role == null) {
            throw new ResourceNotFoundException("系统角色不存在: " + roleCode);
        }
        return role;
    }

    /**
     * 转换空间成员视图对象。
     *
     * @param member 空间成员实体
     * @return 空间成员视图对象
     */
    private SpaceMemberVO toSpaceMemberVO(SpaceMember member) {
        User user = userMapper.selectById(member.getUserId());
        Role role = roleMapper.selectById(member.getRoleId());
        String username = user == null ? "" : user.getUsername();
        String nickname = user == null ? "" : resolveNickname(user);
        String email = user == null ? "" : user.getEmail();
        String roleCode = role == null ? "" : role.getCode();
        String roleName = role == null ? "" : role.getName();
        return new SpaceMemberVO(member.getSpaceId(), member.getUserId(), username, nickname, email, null,
                member.getRoleId(), roleCode, roleName, member.getGmtJoined(), false);
    }

    /**
     * 解析展示昵称。
     *
     * @param user 用户实体
     * @return 昵称
     */
    private String resolveNickname(User user) {
        if (user == null) {
            return "";
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }
}
