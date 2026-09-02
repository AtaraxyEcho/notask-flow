package com.notaskflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.notaskflow.common.ApiResponse;
import com.notaskflow.common.PageResponse;
import com.notaskflow.domain.dto.request.ProjectArchiveRequest;
import com.notaskflow.domain.dto.request.ProjectMemberRoleUpdateRequest;
import com.notaskflow.domain.dto.request.ProjectMemberSaveRequest;
import com.notaskflow.domain.dto.request.ProjectSaveRequest;
import com.notaskflow.domain.query.NoteQuery;
import com.notaskflow.domain.query.ProjectQuery;
import com.notaskflow.domain.query.TaskQuery;
import com.notaskflow.domain.vo.NoteVO;
import com.notaskflow.domain.vo.ProjectMemberVO;
import com.notaskflow.domain.vo.ProjectVO;
import com.notaskflow.domain.vo.TaskVO;
import com.notaskflow.service.NoteService;
import com.notaskflow.service.ProjectService;
import com.notaskflow.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目控制器。
 *
 * @author LIN
 */
@Validated
@Tag(name = "项目管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/spaces/{spaceId}/projects")
public class ProjectController {

    private final ProjectService projectService;

    private final TaskService taskService;

    private final NoteService noteService;

    /**
     * 分页查询项目。
     *
     * @param spaceId 空间标识
     * @param query 查询条件
     * @return 项目分页结果
     */
    @SaCheckPermission("space:note:view")
    @Operation(summary = "分页查询项目")
    @GetMapping
    public ApiResponse<PageResponse<ProjectVO>> page(@PathVariable Long spaceId,
                                                     @Valid @ModelAttribute ProjectQuery query) {
        return ApiResponse.success(projectService.page(spaceId, query));
    }

    /**
     * 查询项目选项列表。
     *
     * @param spaceId 空间标识
     * @return 项目列表
     */
    @SaCheckPermission("space:note:view")
    @Operation(summary = "查询项目选项列表")
    @GetMapping("/options")
    public ApiResponse<List<ProjectVO>> listOptions(@PathVariable Long spaceId) {
        return ApiResponse.success(projectService.listOptions(spaceId));
    }

    /**
     * 查询项目详情。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @return 项目详情
     */
    @SaCheckPermission("space:note:view")
    @Operation(summary = "查询项目详情")
    @GetMapping("/{projectId}")
    public ApiResponse<ProjectVO> get(@PathVariable Long spaceId, @PathVariable Long projectId) {
        return ApiResponse.success(projectService.get(spaceId, projectId));
    }

    /**
     * 创建项目。
     *
     * @param spaceId 空间标识
     * @param request 项目保存请求
     * @return 项目详情
     */
    @SaCheckPermission("space:note:edit")
    @Operation(summary = "创建项目")
    @PostMapping
    public ApiResponse<ProjectVO> create(@PathVariable Long spaceId, @Valid @RequestBody ProjectSaveRequest request) {
        return ApiResponse.success(projectService.create(spaceId, request));
    }

    /**
     * 更新项目。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @param request 项目保存请求
     * @return 项目详情
     */
    @SaCheckPermission("space:note:edit")
    @Operation(summary = "更新项目")
    @PutMapping("/{projectId}")
    public ApiResponse<ProjectVO> update(@PathVariable Long spaceId,
                                         @PathVariable Long projectId,
                                         @Valid @RequestBody ProjectSaveRequest request) {
        return ApiResponse.success(projectService.update(spaceId, projectId, request));
    }

    /**
     * 更新项目归档状态。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @param request 项目归档请求
     * @return 项目详情
     */
    @SaCheckPermission("space:note:edit")
    @Operation(summary = "更新项目归档状态")
    @PutMapping("/{projectId}/archive")
    public ApiResponse<ProjectVO> archive(@PathVariable Long spaceId,
                                          @PathVariable Long projectId,
                                          @Valid @RequestBody ProjectArchiveRequest request) {
        return ApiResponse.success(projectService.archive(spaceId, projectId, request));
    }

    /**
     * 删除项目。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @return 空响应
     */
    @SaCheckPermission("space:note:edit")
    @Operation(summary = "删除项目")
    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> delete(@PathVariable Long spaceId, @PathVariable Long projectId) {
        projectService.delete(spaceId, projectId);
        return ApiResponse.success();
    }

    /**
     * 查询项目成员。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @return 项目成员列表
     */
    @SaCheckPermission("space:note:view")
    @Operation(summary = "查询项目成员")
    @GetMapping("/{projectId}/members")
    public ApiResponse<List<ProjectMemberVO>> listMembers(@PathVariable Long spaceId, @PathVariable Long projectId) {
        return ApiResponse.success(projectService.listMembers(spaceId, projectId));
    }

    /**
     * 添加项目成员。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @param request 项目成员保存请求
     * @return 项目成员详情
     */
    @SaCheckPermission("space:note:edit")
    @Operation(summary = "添加项目成员")
    @PostMapping("/{projectId}/members")
    public ApiResponse<ProjectMemberVO> addMember(@PathVariable Long spaceId,
                                                  @PathVariable Long projectId,
                                                  @Valid @RequestBody ProjectMemberSaveRequest request) {
        return ApiResponse.success(projectService.addMember(spaceId, projectId, request));
    }

    /**
     * 更新项目成员角色。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @param userId 用户标识
     * @param request 项目成员角色更新请求
     * @return 项目成员详情
     */
    @SaCheckPermission("space:note:edit")
    @Operation(summary = "更新项目成员角色")
    @PutMapping("/{projectId}/members/{userId}")
    public ApiResponse<ProjectMemberVO> updateMemberRole(@PathVariable Long spaceId,
                                                         @PathVariable Long projectId,
                                                         @PathVariable Long userId,
                                                         @Valid @RequestBody
                                                         ProjectMemberRoleUpdateRequest request) {
        return ApiResponse.success(projectService.updateMemberRole(spaceId, projectId, userId, request));
    }

    /**
     * 移除项目成员。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @param userId 用户标识
     * @return 空响应
     */
    @SaCheckPermission("space:note:edit")
    @Operation(summary = "移除项目成员")
    @DeleteMapping("/{projectId}/members/{userId}")
    public ApiResponse<Void> removeMember(@PathVariable Long spaceId,
                                          @PathVariable Long projectId,
                                          @PathVariable Long userId) {
        projectService.removeMember(spaceId, projectId, userId);
        return ApiResponse.success();
    }

    /**
     * 查询项目关联任务。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @param query 任务查询条件
     * @return 任务分页结果
     */
    @SaCheckPermission("space:note:view")
    @Operation(summary = "查询项目关联任务")
    @GetMapping("/{projectId}/tasks")
    public ApiResponse<PageResponse<TaskVO>> pageTasks(@PathVariable Long spaceId,
                                                       @PathVariable Long projectId,
                                                       @Valid @ModelAttribute TaskQuery query) {
        projectService.get(spaceId, projectId);
        query.setProjectId(projectId);
        return ApiResponse.success(taskService.page(spaceId, query));
    }

    /**
     * 查询项目关联文档。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @param query 笔记查询条件
     * @return 笔记分页结果
     */
    @SaCheckPermission("space:note:view")
    @Operation(summary = "查询项目关联文档")
    @GetMapping("/{projectId}/notes")
    public ApiResponse<PageResponse<NoteVO>> pageNotes(@PathVariable Long spaceId,
                                                       @PathVariable Long projectId,
                                                       @Valid @ModelAttribute NoteQuery query) {
        projectService.get(spaceId, projectId);
        query.setProjectId(projectId);
        return ApiResponse.success(noteService.page(spaceId, query));
    }
}
