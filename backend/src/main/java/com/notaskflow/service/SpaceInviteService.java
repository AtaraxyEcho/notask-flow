package com.notaskflow.service;

import com.notaskflow.domain.dto.request.SpaceInviteCreateRequest;
import com.notaskflow.domain.vo.SpaceInvitePreviewVO;
import com.notaskflow.domain.vo.SpaceInviteVO;
import com.notaskflow.domain.vo.SpaceMemberVO;

/**
 * 团队邀请码服务接口。
 *
 * @author LIN
 */
public interface SpaceInviteService {

    /**
     * 创建团队邀请码。
     *
     * @param spaceId 空间标识
     * @param request 创建请求
     * @return 邀请码信息
     */
    SpaceInviteVO createInvite(Long spaceId, SpaceInviteCreateRequest request);

    /**
     * 预览团队邀请码对应的团队信息。
     *
     * @param code 邀请码
     * @return 邀请码预览信息
     */
    SpaceInvitePreviewVO preview(String code);

    /**
     * 使用团队邀请码加入空间。
     *
     * @param code 邀请码
     * @return 新增成员信息
     */
    SpaceMemberVO joinByCode(String code);

    /**
     * 指定用户使用团队邀请码加入空间。
     *
     * @param userId 用户标识
     * @param code 邀请码
     * @return 新增成员信息
     */
    SpaceMemberVO joinByCode(Long userId, String code);
}
