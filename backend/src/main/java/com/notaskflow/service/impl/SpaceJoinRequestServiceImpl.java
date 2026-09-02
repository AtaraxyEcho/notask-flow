package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.notaskflow.common.enums.BusinessType;
import com.notaskflow.common.enums.JoinRequestStatus;
import com.notaskflow.common.enums.NotificationType;
import com.notaskflow.common.enums.RoleCode;
import com.notaskflow.common.enums.SpaceType;
import com.notaskflow.domain.dto.request.SpaceJoinApplyRequest;
import com.notaskflow.domain.dto.request.SpaceJoinApproveRequest;
import com.notaskflow.domain.dto.request.SpaceJoinRejectRequest;
import com.notaskflow.domain.entity.Role;
import com.notaskflow.domain.entity.Space;
import com.notaskflow.domain.entity.SpaceJoinRequest;
import com.notaskflow.domain.entity.SpaceMember;
import com.notaskflow.domain.entity.User;
import com.notaskflow.domain.vo.SpaceJoinRequestVO;
import com.notaskflow.event.NotificationCreateEvent;
import com.notaskflow.exception.AccessDeniedException;
import com.notaskflow.exception.BusinessException;
import com.notaskflow.exception.ErrorCode;
import com.notaskflow.exception.ResourceNotFoundException;
import com.notaskflow.mapper.RoleMapper;
import com.notaskflow.mapper.SpaceJoinRequestMapper;
import com.notaskflow.mapper.SpaceMapper;
import com.notaskflow.mapper.SpaceMemberMapper;
import com.notaskflow.mapper.UserMapper;
import com.notaskflow.security.PermissionValidator;
import com.notaskflow.service.SpaceJoinRequestService;
import com.notaskflow.utils.LoginUserUtil;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 团队加入申请服务实现，处理申请提交和审批流程。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpaceJoinRequestServiceImpl implements SpaceJoinRequestService {

    private final SpaceJoinRequestMapper spaceJoinRequestMapper;

    private final SpaceMapper spaceMapper;

    private final SpaceMemberMapper spaceMemberMapper;

    private final RoleMapper roleMapper;

    private final UserMapper userMapper;

    private final PermissionValidator permissionValidator;

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 创建团队加入申请。
     *
     * @param request 创建请求
     * @return 申请信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpaceJoinRequestVO apply(SpaceJoinApplyRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        User supervisor = findUserByAccount(request.getSupervisorAccount());
        if (currentUserId.equals(supervisor.getId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上级账号不能为当前用户");
        }
        Long pendingCount = spaceJoinRequestMapper.selectCount(Wrappers.<SpaceJoinRequest>lambdaQuery()
                .eq(SpaceJoinRequest::getApplicantUserId, currentUserId)
                .eq(SpaceJoinRequest::getSupervisorUserId, supervisor.getId())
                .eq(SpaceJoinRequest::getStatus, JoinRequestStatus.PENDING));
        if (pendingCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "已存在待审批的团队加入申请");
        }

        SpaceJoinRequest entity = new SpaceJoinRequest();
        entity.setApplicantUserId(currentUserId);
        entity.setSupervisorUserId(supervisor.getId());
        entity.setTeamName(request.getTeamName());
        entity.setTargetSpaceId(resolveTargetSpaceIdForApply(supervisor.getId(), request.getTeamName()));
        entity.setStatus(JoinRequestStatus.PENDING);
        entity.setRemark(request.getRemark());
        if (entity.getTargetSpaceId() != null
                && permissionValidator.findSpaceMember(entity.getTargetSpaceId(), currentUserId) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "你已在该团队空间中");
        }
        spaceJoinRequestMapper.insert(entity);

        User applicant = userMapper.selectById(currentUserId);
        String applicantName = applicant == null ? "有新成员" : resolveDisplayName(applicant);
        String teamName = request.getTeamName() == null ? "" : request.getTeamName().trim();
        String content = teamName.isEmpty()
                ? applicantName + " 提交了一条团队加入申请，请及时处理。"
                : applicantName + " 申请加入团队「" + teamName + "」，请及时处理。";
        publishNotification(
                supervisor.getId(),
                NotificationType.SPACE_JOIN_APPLIED,
                BusinessType.SPACE_JOIN_REQUEST,
                entity.getId(),
                "新的团队加入申请",
                content
        );

        log.info("团队加入申请创建完成，requestId={}，applicantUserId={}，supervisorUserId={}",
                entity.getId(), currentUserId, supervisor.getId());
        return toVO(entity);
    }

    /**
     * 查询当前用户提交的申请。
     *
     * @return 申请列表
     */
    @Override
    public List<SpaceJoinRequestVO> listMine() {
        Long currentUserId = LoginUserUtil.currentUserId();
        return spaceJoinRequestMapper.selectList(Wrappers.<SpaceJoinRequest>lambdaQuery()
                        .eq(SpaceJoinRequest::getApplicantUserId, currentUserId)
                        .orderByDesc(SpaceJoinRequest::getGmtCreate))
                .stream()
                .map(this::toVO)
                .toList();
    }

    /**
     * 查询当前用户待处理的申请。
     *
     * @return 申请列表
     */
    @Override
    public List<SpaceJoinRequestVO> listPendingForMe() {
        Long currentUserId = LoginUserUtil.currentUserId();
        return spaceJoinRequestMapper.selectList(Wrappers.<SpaceJoinRequest>lambdaQuery()
                        .eq(SpaceJoinRequest::getSupervisorUserId, currentUserId)
                        .eq(SpaceJoinRequest::getStatus, JoinRequestStatus.PENDING)
                        .orderByAsc(SpaceJoinRequest::getGmtCreate))
                .stream()
                .map(this::toVO)
                .toList();
    }

    /**
     * 审批通过加入申请。
     *
     * @param requestId 申请标识
     * @param request 审批请求
     * @return 申请信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpaceJoinRequestVO approve(Long requestId, SpaceJoinApproveRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        SpaceJoinRequest entity = findRequest(requestId);
        ensureSupervisor(entity, currentUserId);
        ensurePending(entity);
        permissionValidator.ensureSpaceAdminOrOwner(request.getSpaceId(), currentUserId);

        Space space = findTeamSpace(request.getSpaceId());
        if (entity.getTargetSpaceId() != null && !entity.getTargetSpaceId().equals(space.getId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "申请目标团队与当前审批空间不一致");
        }
        Role role = findJoinableRole(request.getRoleCode());
        User applicant = userMapper.selectById(entity.getApplicantUserId());
        if (applicant == null) {
            throw new ResourceNotFoundException("申请用户不存在");
        }

        SpaceMember exists = permissionValidator.findSpaceMember(space.getId(), applicant.getId());
        if (exists == null) {
            SpaceMember member = new SpaceMember();
            member.setSpaceId(space.getId());
            member.setUserId(applicant.getId());
            member.setRoleId(role.getId());
            spaceMemberMapper.insert(member);
        }

        entity.setTargetSpaceId(space.getId());
        entity.setStatus(JoinRequestStatus.APPROVED);
        entity.setReviewerUserId(currentUserId);
        entity.setReviewedAt(LocalDateTime.now());
        spaceJoinRequestMapper.updateById(entity);

        publishNotification(
                applicant.getId(),
                NotificationType.SPACE_JOIN_APPROVED,
                BusinessType.SPACE_JOIN_REQUEST,
                entity.getId(),
                "团队加入申请已通过",
                "你的团队加入申请已通过，现已加入空间「" + space.getName() + "」。"
        );

        log.info("团队加入申请审批通过，requestId={}，spaceId={}，applicantUserId={}，operatorId={}",
                requestId, space.getId(), applicant.getId(), currentUserId);
        return toVO(entity);
    }

    /**
     * 拒绝加入申请。
     *
     * @param requestId 申请标识
     * @param request 拒绝请求
     * @return 申请信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpaceJoinRequestVO reject(Long requestId, SpaceJoinRejectRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        SpaceJoinRequest entity = findRequest(requestId);
        ensureSupervisor(entity, currentUserId);
        ensurePending(entity);

        entity.setStatus(JoinRequestStatus.REJECTED);
        entity.setRejectReason(request.getReason());
        entity.setReviewerUserId(currentUserId);
        entity.setReviewedAt(LocalDateTime.now());
        spaceJoinRequestMapper.updateById(entity);

        String rejectReason = request.getReason() == null ? "" : request.getReason().trim();
        String content = rejectReason.isEmpty()
                ? "你的团队加入申请未通过审核。"
                : "你的团队加入申请未通过审核。原因：" + rejectReason;
        publishNotification(
                entity.getApplicantUserId(),
                NotificationType.SPACE_JOIN_REJECTED,
                BusinessType.SPACE_JOIN_REQUEST,
                entity.getId(),
                "团队加入申请已拒绝",
                content
        );

        log.info("团队加入申请已拒绝，requestId={}，operatorId={}", requestId, currentUserId);
        return toVO(entity);
    }

    /**
     * 查询申请实体。
     *
     * @param requestId 申请标识
     * @return 申请实体
     */
    private SpaceJoinRequest findRequest(Long requestId) {
        SpaceJoinRequest entity = spaceJoinRequestMapper.selectById(requestId);
        if (entity == null) {
            throw new ResourceNotFoundException("团队加入申请不存在");
        }
        return entity;
    }

    /**
     * 校验当前用户是否为申请审批人。
     *
     * @param entity 申请实体
     * @param currentUserId 当前用户标识
     */
    private void ensureSupervisor(SpaceJoinRequest entity, Long currentUserId) {
        if (!currentUserId.equals(entity.getSupervisorUserId())) {
            throw new AccessDeniedException("仅申请指定上级可审批该申请");
        }
    }

    /**
     * 校验申请是否处于待审批状态。
     *
     * @param entity 申请实体
     */
    private void ensurePending(SpaceJoinRequest entity) {
        if (!JoinRequestStatus.PENDING.equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅待审批申请可处理");
        }
    }

    /**
     * 查询团队空间。
     *
     * @param spaceId 空间标识
     * @return 团队空间实体
     */
    private Space findTeamSpace(Long spaceId) {
        Space space = spaceMapper.selectById(spaceId);
        if (space == null) {
            throw new ResourceNotFoundException("空间不存在");
        }
        if (!SpaceType.TEAM.equals(space.getType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只能审批加入团队空间");
        }
        return space;
    }

    /**
     * 根据团队名称和审批人解析目标团队空间。
     *
     * @param supervisorUserId 审批人标识
     * @param teamName 团队名称
     * @return 目标团队空间标识，无法确定时返回null
     */
    private Long resolveTargetSpaceId(Long supervisorUserId, String teamName) {
        String normalizedTeamName = teamName == null ? "" : teamName.trim();
        if (normalizedTeamName.isEmpty()) {
            return null;
        }
        List<Space> spaces = spaceMapper.selectList(Wrappers.<Space>lambdaQuery()
                .eq(Space::getType, SpaceType.TEAM)
                .eq(Space::getName, normalizedTeamName));
        for (Space space : spaces) {
            if (supervisorUserId.equals(space.getOwnerUserId())) {
                return space.getId();
            }
            SpaceMember member = permissionValidator.findSpaceMember(space.getId(), supervisorUserId);
            if (member == null) {
                continue;
            }
            Role role = roleMapper.selectById(member.getRoleId());
            if (role != null && (RoleCode.SPACE_OWNER.getCode().equals(role.getCode())
                    || RoleCode.SPACE_ADMIN.getCode().equals(role.getCode()))) {
                return space.getId();
            }
        }
        return null;
    }

    /**
     * 解析并校验申请目标团队空间。
     *
     * @param supervisorUserId 审批人标识
     * @param teamName 团队名称
     * @return 目标团队空间标识，多团队且未指定名称时返回null
     */
    private Long resolveTargetSpaceIdForApply(Long supervisorUserId, String teamName) {
        if (teamName != null && !teamName.trim().isEmpty()) {
            Long targetSpaceId = resolveTargetSpaceId(supervisorUserId, teamName);
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
     * 查询用户可管理的团队空间。
     *
     * @param supervisorUserId 用户标识
     * @return 可管理团队空间
     */
    private List<Space> findManageableTeamSpaces(Long supervisorUserId) {
        return spaceMapper.selectList(Wrappers.<Space>lambdaQuery()
                        .eq(Space::getType, SpaceType.TEAM))
                .stream()
                .filter(space -> isSpaceOwnerOrAdmin(space, supervisorUserId))
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
        SpaceMember member = permissionValidator.findSpaceMember(space.getId(), userId);
        if (member == null) {
            return false;
        }
        Role role = roleMapper.selectById(member.getRoleId());
        return role != null && (RoleCode.SPACE_OWNER.getCode().equals(role.getCode())
                || RoleCode.SPACE_ADMIN.getCode().equals(role.getCode()));
    }

    /**
     * 根据账号查询用户。
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
     * 查询可审批加入的角色。
     *
     * @param roleCode 角色编码
     * @return 角色实体
     */
    private Role findJoinableRole(String roleCode) {
        if (RoleCode.SPACE_OWNER.getCode().equals(roleCode)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不可通过审批授予空间所有者角色");
        }
        Role role = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getCode, roleCode));
        if (role == null) {
            throw new ResourceNotFoundException("系统角色不存在: " + roleCode);
        }
        return role;
    }

    /**
     * 发布通知创建事件，通知会在当前事务提交后投递到 RabbitMQ。
     *
     * @param userId 接收用户标识
     * @param type 通知类型
     * @param businessType 业务类型
     * @param businessId 业务标识
     * @param title 通知标题
     * @param content 通知内容
     */
    private void publishNotification(Long userId, NotificationType type, BusinessType businessType, Long businessId,
                                     String title, String content) {
        applicationEventPublisher.publishEvent(new NotificationCreateEvent(
                userId,
                type,
                businessType,
                businessId,
                title,
                content
        ));
    }

    /**
     * 转换团队加入申请视图对象。
     *
     * @param entity 申请实体
     * @return 申请视图对象
     */
    private SpaceJoinRequestVO toVO(SpaceJoinRequest entity) {
        User applicant = userMapper.selectById(entity.getApplicantUserId());
        User supervisor = userMapper.selectById(entity.getSupervisorUserId());
        Space targetSpace = entity.getTargetSpaceId() == null ? null : spaceMapper.selectById(entity.getTargetSpaceId());
        String applicantUsername = applicant == null ? "" : applicant.getUsername();
        String applicantEmail = applicant == null ? "" : applicant.getEmail();
        String supervisorUsername = supervisor == null ? "" : supervisor.getUsername();
        String targetSpaceName = targetSpace == null ? "" : targetSpace.getName();
        return new SpaceJoinRequestVO(
                entity.getId(),
                entity.getApplicantUserId(),
                applicantUsername,
                applicantEmail,
                entity.getSupervisorUserId(),
                supervisorUsername,
                entity.getTargetSpaceId(),
                targetSpaceName,
                entity.getTeamName(),
                entity.getStatus(),
                entity.getRemark(),
                entity.getRejectReason(),
                entity.getReviewerUserId(),
                entity.getReviewedAt(),
                entity.getGmtCreate()
        );
    }

    /**
     * 解析显示名称。
     *
     * @param user 用户实体
     * @return 显示名称
     */
    private String resolveDisplayName(User user) {
        if (user == null) {
            return "";
        }
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        return user.getUsername();
    }
}
