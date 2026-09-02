package com.notaskflow.service;

import com.notaskflow.domain.dto.request.SpaceCreateRequest;
import com.notaskflow.domain.dto.request.SpaceMemberAddRequest;
import com.notaskflow.domain.dto.request.SpaceMemberRoleUpdateRequest;
import com.notaskflow.domain.dto.request.SpaceUpdateRequest;
import com.notaskflow.domain.vo.SpaceMemberVO;
import com.notaskflow.domain.vo.SpaceEventTicketConsumeVO;
import com.notaskflow.domain.vo.SpaceEventTicketVO;
import com.notaskflow.domain.vo.SpaceVO;
import java.util.List;

/**
 * 空间服务接口。
 *
 * @author LIN
 */
public interface SpaceService {

    /**
     * 查询当前用户可访问空间。
     *
     * @return 空间列表
     */
    List<SpaceVO> listMySpaces();

    /**
     * 创建团队空间。
     *
     * @param request 创建请求
     * @return 空间信息
     */
    SpaceVO createTeamSpace(SpaceCreateRequest request);

    /**
     * 获取空间详情。
     *
     * @param spaceId 空间标识
     * @return 空间信息
     */
    SpaceVO getSpace(Long spaceId);

    /**
     * 更新空间信息。
     *
     * @param spaceId 空间标识
     * @param request 更新请求
     * @return 空间信息
     */
    SpaceVO updateSpace(Long spaceId, SpaceUpdateRequest request);

    /**
     * 删除团队空间。
     *
     * @param spaceId 空间标识
     */
    void deleteSpace(Long spaceId);

    /**
     * 查询空间成员列表。
     *
     * @param spaceId 空间标识
     * @return 空间成员列表
     */
    List<SpaceMemberVO> listMembers(Long spaceId);

    /**
     * 刷新当前用户在指定团队空间的在线心跳。
     *
     * @param spaceId 空间标识
     * @param clientId 客户端标识
     */
    void heartbeatMember(Long spaceId, String clientId);

    /**
     * 清理当前用户在指定团队空间的在线状态。
     *
     * @param spaceId 空间标识
     * @param clientId 客户端标识
     */
    void offlineMember(Long spaceId, String clientId);

    /**
     * 添加空间成员。
     *
     * @param spaceId 空间标识
     * @param request 添加请求
     * @return 成员信息
     */
    SpaceMemberVO addMember(Long spaceId, SpaceMemberAddRequest request);

    /**
     * 更新空间成员角色。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     * @param request 更新请求
     * @return 成员信息
     */
    SpaceMemberVO updateMemberRole(Long spaceId, Long userId, SpaceMemberRoleUpdateRequest request);

    /**
     * 移除空间成员。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     */
    void removeMember(Long spaceId, Long userId);

    /**
     * 创建空间实时事件 Ticket。
     *
     * @param spaceId 空间标识
     * @return 空间实时事件 Ticket
     */
    SpaceEventTicketVO createSpaceEventTicket(Long spaceId);

    /**
     * 消费空间实时事件 Ticket。
     *
     * @param ticket Ticket 值
     * @return Ticket 消费结果
     */
    SpaceEventTicketConsumeVO consumeSpaceEventTicket(String ticket);

    void leaveSpace(Long spaceId);
}
