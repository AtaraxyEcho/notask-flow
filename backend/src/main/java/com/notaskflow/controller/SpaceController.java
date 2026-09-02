package com.notaskflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.notaskflow.common.ApiResponse;
import com.notaskflow.domain.dto.request.SpaceInviteCreateRequest;
import com.notaskflow.domain.dto.request.SpaceCreateRequest;
import com.notaskflow.domain.dto.request.SpaceMemberAddRequest;
import com.notaskflow.domain.dto.request.SpaceMemberPresenceRequest;
import com.notaskflow.domain.dto.request.SpaceMemberRoleUpdateRequest;
import com.notaskflow.domain.dto.request.SpaceUpdateRequest;
import com.notaskflow.domain.vo.SpaceEventTicketVO;
import com.notaskflow.domain.vo.SpaceInvitePreviewVO;
import com.notaskflow.domain.vo.SpaceInviteVO;
import com.notaskflow.domain.vo.SpaceMemberVO;
import com.notaskflow.domain.vo.SpaceVO;
import com.notaskflow.service.SpaceInviteService;
import com.notaskflow.service.SpaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 空间管理控制器。
 *
 * @author LIN
 */
@Tag(name = "空间管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/spaces")
public class SpaceController {

    private final SpaceService spaceService;

    private final SpaceInviteService spaceInviteService;

    /**
     * 获取当前用户可访问空间列表。
     *
     * @return 空间列表
     */
    @Operation(summary = "获取空间列表")
    @GetMapping
    public ApiResponse<List<SpaceVO>> listMySpaces() {
        return ApiResponse.success(spaceService.listMySpaces());
    }

    /**
     * 创建团队空间。
     *
     * @param request 创建请求
     * @return 空间信息
     */
    @Operation(summary = "创建团队空间")
    @PostMapping
    public ApiResponse<SpaceVO> createTeamSpace(@Valid @RequestBody SpaceCreateRequest request) {
        return ApiResponse.success(spaceService.createTeamSpace(request));
    }

    /**
     * 创建团队空间邀请码。
     *
     * @param spaceId 空间标识
     * @param request 创建请求
     * @return 邀请码信息
     */
    @SaCheckPermission("space:member:manage")
    @Operation(summary = "创建团队空间邀请码")
    @PostMapping("/{spaceId}/invites")
    public ApiResponse<SpaceInviteVO> createInvite(@PathVariable Long spaceId,
                                                   @Valid @RequestBody SpaceInviteCreateRequest request) {
        return ApiResponse.success(spaceInviteService.createInvite(spaceId, request));
    }

    /**
     * 预览团队空间邀请码。
     *
     * @param code 邀请码
     * @return 邀请预览信息
     */
    @Operation(summary = "预览团队空间邀请码")
    @GetMapping("/invites/{code}")
    public ApiResponse<SpaceInvitePreviewVO> previewInvite(@PathVariable String code) {
        return ApiResponse.success(spaceInviteService.preview(code));
    }

    /**
     * 使用邀请码加入团队空间。
     *
     * @param code 邀请码
     * @return 成员信息
     */
    @Operation(summary = "使用邀请码加入团队空间")
    @PostMapping("/invites/{code}/join")
    public ApiResponse<SpaceMemberVO> joinByInviteCode(@PathVariable String code) {
        return ApiResponse.success(spaceInviteService.joinByCode(code));
    }

    /**
     * 获取空间详情。
     *
     * @param spaceId 空间标识
     * @return 空间信息
     */
    @SaCheckPermission("space:member:view")
    @Operation(summary = "获取空间详情")
    @GetMapping("/{spaceId}")
    public ApiResponse<SpaceVO> getSpace(@PathVariable Long spaceId) {
        return ApiResponse.success(spaceService.getSpace(spaceId));
    }

    /**
     * 获取当前用户在指定空间下的权限码列表。
     *
     * @param spaceId 空间标识
     * @return 权限码列表
     */
    @Operation(summary = "获取当前空间权限码列表")
    @GetMapping("/{spaceId}/permissions")
    public ApiResponse<List<String>> permissions(@PathVariable Long spaceId) {
        return ApiResponse.success(StpUtil.getPermissionList());
    }

    /**
     * 创建空间实时事件 Ticket。
     *
     * @param spaceId 空间标识
     * @return 空间实时事件 Ticket
     */
    @Operation(summary = "创建空间实时事件 Ticket")
    @PostMapping("/{spaceId}/events-ticket")
    public ApiResponse<SpaceEventTicketVO> createSpaceEventTicket(@PathVariable Long spaceId) {
        return ApiResponse.success(spaceService.createSpaceEventTicket(spaceId));
    }

    /**
     * 更新空间信息。
     *
     * @param spaceId 空间标识
     * @param request 更新请求
     * @return 空间信息
     */
    @SaCheckPermission("space:member:manage")
    @Operation(summary = "更新空间信息")
    @PutMapping("/{spaceId}")
    public ApiResponse<SpaceVO> updateSpace(@PathVariable Long spaceId,
                                            @Valid @RequestBody SpaceUpdateRequest request) {
        return ApiResponse.success(spaceService.updateSpace(spaceId, request));
    }

    /**
     * 删除团队空间。
     *
     * @param spaceId 空间标识
     * @return 空响应
     */
    @SaCheckPermission("space:member:manage")
    @Operation(summary = "删除团队空间")
    @DeleteMapping("/{spaceId}")
    public ApiResponse<Void> deleteSpace(@PathVariable Long spaceId) {
        spaceService.deleteSpace(spaceId);
        return ApiResponse.success();
    }

    /**
     * 查询空间成员。
     *
     * @param spaceId 空间标识
     * @return 成员列表
     */
    @SaCheckPermission("space:member:view")
    @Operation(summary = "查询空间成员")
    @GetMapping("/{spaceId}/members")
    public ApiResponse<List<SpaceMemberVO>> listMembers(@PathVariable Long spaceId) {
        return ApiResponse.success(spaceService.listMembers(spaceId));
    }

    /**
     * 刷新当前用户在团队空间内的在线状态。
     *
     * @param spaceId 空间标识
     * @param request 在线状态请求
     * @return 空响应
     */
    @SaCheckPermission("space:member:view")
    @Operation(summary = "刷新团队空间成员在线状态")
    @PostMapping("/{spaceId}/members/heartbeat")
    public ApiResponse<Void> heartbeatMember(@PathVariable Long spaceId,
                                             @RequestBody(required = false) SpaceMemberPresenceRequest request) {
        spaceService.heartbeatMember(spaceId, clientIdOf(request));
        return ApiResponse.success();
    }

    /**
     * 清理当前用户在团队空间内的在线状态。
     *
     * @param spaceId 空间标识
     * @param request 在线状态请求
     * @return 空响应
     */
    @SaCheckPermission("space:member:view")
    @Operation(summary = "清理团队空间成员在线状态")
    @PostMapping("/{spaceId}/members/offline")
    public ApiResponse<Void> offlineMember(@PathVariable Long spaceId,
                                           @RequestBody(required = false) SpaceMemberPresenceRequest request) {
        spaceService.offlineMember(spaceId, clientIdOf(request));
        return ApiResponse.success();
    }

    /**
     * 添加空间成员。
     *
     * @param spaceId 空间标识
     * @param request 添加请求
     * @return 成员信息
     */
    @SaCheckPermission("space:member:manage")
    @Operation(summary = "添加空间成员")
    @PostMapping("/{spaceId}/members")
    public ApiResponse<SpaceMemberVO> addMember(@PathVariable Long spaceId,
                                                @Valid @RequestBody SpaceMemberAddRequest request) {
        return ApiResponse.success(spaceService.addMember(spaceId, request));
    }

    /**
     * 更新成员角色。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     * @param request 更新请求
     * @return 成员信息
     */
    @SaCheckPermission("space:member:manage")
    @Operation(summary = "更新成员角色")
    @PutMapping("/{spaceId}/members/{userId}")
    public ApiResponse<SpaceMemberVO> updateMemberRole(@PathVariable Long spaceId,
                                                       @PathVariable Long userId,
                                                       @Valid @RequestBody SpaceMemberRoleUpdateRequest request) {
        return ApiResponse.success(spaceService.updateMemberRole(spaceId, userId, request));
    }

    /**
     * 移除空间成员。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     * @return 空响应
     */
    @SaCheckPermission("space:member:manage")
    @Operation(summary = "移除空间成员")
    @DeleteMapping("/{spaceId}/members/{userId}")
    public ApiResponse<Void> removeMember(@PathVariable Long spaceId, @PathVariable Long userId) {
        spaceService.removeMember(spaceId, userId);
        return ApiResponse.success();
    }

    @Operation(summary = "退出团队空间")
    @DeleteMapping("/{spaceId}/members/me")
    public ApiResponse<Void> leaveSpace(@PathVariable Long spaceId) {
        spaceService.leaveSpace(spaceId);
        return ApiResponse.success();
    }

    /**
     * 解析在线状态客户端标识。
     *
     * @param request 在线状态请求
     * @return 客户端标识
     */
    private String clientIdOf(SpaceMemberPresenceRequest request) {
        return request == null ? null : request.getClientId();
    }
}
