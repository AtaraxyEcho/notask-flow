package com.notaskflow.service;

import com.notaskflow.common.PageResponse;
import com.notaskflow.domain.dto.request.ProjectArchiveRequest;
import com.notaskflow.domain.dto.request.ProjectMemberRoleUpdateRequest;
import com.notaskflow.domain.dto.request.ProjectMemberSaveRequest;
import com.notaskflow.domain.dto.request.ProjectSaveRequest;
import com.notaskflow.domain.query.ProjectQuery;
import com.notaskflow.domain.vo.ProjectMemberVO;
import com.notaskflow.domain.vo.ProjectVO;
import java.util.List;

/**
 * 项目服务接口。
 *
 * @author LIN
 */
public interface ProjectService {

    /**
     * 分页查询项目。
     *
     * @param spaceId 空间标识
     * @param query 查询条件
     * @return 项目分页结果
     */
    PageResponse<ProjectVO> page(Long spaceId, ProjectQuery query);

    /**
     * 查询项目选项列表。
     *
     * @param spaceId 空间标识
     * @return 项目列表
     */
    List<ProjectVO> listOptions(Long spaceId);

    /**
     * 查询项目详情。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @return 项目详情
     */
    ProjectVO get(Long spaceId, Long projectId);

    /**
     * 创建项目。
     *
     * @param spaceId 空间标识
     * @param request 项目保存请求
     * @return 项目详情
     */
    ProjectVO create(Long spaceId, ProjectSaveRequest request);

    /**
     * 更新项目。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @param request 项目保存请求
     * @return 项目详情
     */
    ProjectVO update(Long spaceId, Long projectId, ProjectSaveRequest request);

    /**
     * 更新项目归档状态。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @param request 项目归档请求
     * @return 项目详情
     */
    ProjectVO archive(Long spaceId, Long projectId, ProjectArchiveRequest request);

    /**
     * 删除项目。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     */
    void delete(Long spaceId, Long projectId);

    /**
     * 查询项目成员。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @return 项目成员列表
     */
    List<ProjectMemberVO> listMembers(Long spaceId, Long projectId);

    /**
     * 添加项目成员。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @param request 项目成员保存请求
     * @return 项目成员详情
     */
    ProjectMemberVO addMember(Long spaceId, Long projectId, ProjectMemberSaveRequest request);

    /**
     * 更新项目成员角色。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @param userId 用户标识
     * @param request 项目成员角色更新请求
     * @return 项目成员详情
     */
    ProjectMemberVO updateMemberRole(Long spaceId, Long projectId, Long userId, ProjectMemberRoleUpdateRequest request);

    /**
     * 移除项目成员。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @param userId 用户标识
     */
    void removeMember(Long spaceId, Long projectId, Long userId);
}
