package com.notaskflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.notaskflow.common.ApiResponse;
import com.notaskflow.common.PageResponse;
import com.notaskflow.domain.dto.request.TaskAssignmentRequest;
import com.notaskflow.domain.dto.request.TaskClaimRequest;
import com.notaskflow.domain.dto.request.TaskCommentCreateRequest;
import com.notaskflow.domain.dto.request.TaskCreateRequest;
import com.notaskflow.domain.dto.request.TaskMemberCompleteRequest;
import com.notaskflow.domain.dto.request.TaskStatusUpdateRequest;
import com.notaskflow.domain.dto.request.TaskUpdateRequest;
import com.notaskflow.domain.query.TaskQuery;
import com.notaskflow.domain.vo.TaskCommentVO;
import com.notaskflow.domain.vo.TaskMemberVO;
import com.notaskflow.domain.vo.TaskVO;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务控制器。
 *
 * @author LIN
 */
@Validated
@Tag(name = "任务管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/spaces/{spaceId}/tasks")
public class TaskController {

    private final TaskService taskService;

    /**
     * 分页查询任务。
     *
     * @param spaceId 空间标识
     * @param query 查询条件
     * @return 分页任务
     */
    @SaCheckPermission("space:task:view")
    @Operation(summary = "分页查询任务")
    @GetMapping
    public ApiResponse<PageResponse<TaskVO>> page(@PathVariable Long spaceId,
                                                  @Valid @ModelAttribute TaskQuery query) {
        return ApiResponse.success(taskService.page(spaceId, query));
    }

    /**
     * 创建任务。
     *
     * @param spaceId 空间标识
     * @param request 创建请求
     * @return 任务详情
     */
    @SaCheckPermission(value = {"space:task:assign", "space:task:claim"}, mode = SaMode.OR)
    @Operation(summary = "创建任务")
    @PostMapping
    public ApiResponse<TaskVO> create(@PathVariable Long spaceId,
                                      @Valid @RequestBody TaskCreateRequest request) {
        return ApiResponse.success(taskService.create(spaceId, request));
    }

    /**
     * 查询任务详情。
     *
     * @param spaceId 空间标识
     * @param id 任务标识
     * @return 任务详情
     */
    @SaCheckPermission("space:task:view")
    @Operation(summary = "查询任务详情")
    @GetMapping("/{id}")
    public ApiResponse<TaskVO> get(@PathVariable Long spaceId, @PathVariable Long id) {
        return ApiResponse.success(taskService.get(spaceId, id));
    }

    /**
     * 更新任务基础信息。
     *
     * @param spaceId 空间标识
     * @param id 任务标识
     * @param request 更新请求
     * @return 任务详情
     */
    @SaCheckPermission("space:task:assign")
    @Operation(summary = "更新任务")
    @PutMapping("/{id}")
    public ApiResponse<TaskVO> update(@PathVariable Long spaceId,
                                      @PathVariable Long id,
                                      @Valid @RequestBody TaskUpdateRequest request) {
        return ApiResponse.success(taskService.update(spaceId, id, request));
    }

    /**
     * 修改任务状态。
     *
     * @param spaceId 空间标识
     * @param id 任务标识
     * @param request 状态请求
     * @return 任务详情
     */
    @SaCheckPermission("space:task:assign")
    @Operation(summary = "修改任务状态")
    @PatchMapping("/{id}/status")
    public ApiResponse<TaskVO> changeStatus(@PathVariable Long spaceId,
                                            @PathVariable Long id,
                                            @Valid @RequestBody TaskStatusUpdateRequest request) {
        return ApiResponse.success(taskService.changeStatus(spaceId, id, request));
    }

    /**
     * 删除任务。
     *
     * @param spaceId 空间标识
     * @param id 任务标识
     * @return 空响应
     */
    @SaCheckPermission("space:task:assign")
    @Operation(summary = "删除任务")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long spaceId, @PathVariable Long id) {
        taskService.delete(spaceId, id);
        return ApiResponse.success();
    }

    /**
     * 开始处理职责。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     * @param memberId 成员工作项标识
     * @return 成员工作项
     */
    @SaCheckPermission("space:task:claim")
    @Operation(summary = "开始处理职责")
    @PostMapping("/{taskId}/members/{memberId}/start")
    public ApiResponse<TaskMemberVO> startMember(@PathVariable Long spaceId,
                                                 @PathVariable Long taskId,
                                                 @PathVariable Long memberId) {
        return ApiResponse.success(taskService.startMember(spaceId, taskId, memberId));
    }

    /**
     * 完成职责。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     * @param memberId 成员工作项标识
     * @param request 完成提交请求
     * @return 成员工作项
     */
    @SaCheckPermission("space:task:claim")
    @Operation(summary = "完成职责")
    @PostMapping("/{taskId}/members/{memberId}/complete")
    public ApiResponse<TaskMemberVO> completeMember(@PathVariable Long spaceId,
                                                    @PathVariable Long taskId,
                                                    @PathVariable Long memberId,
                                                    @Valid @RequestBody(required = false)
                                                    TaskMemberCompleteRequest request) {
        return ApiResponse.success(taskService.completeMember(spaceId, taskId, memberId, request));
    }

    /**
     * 认领开放任务。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     * @param request 认领请求
     * @return 成员工作项
     */
    @SaCheckPermission("space:task:claim")
    @Operation(summary = "认领开放任务")
    @PostMapping("/{taskId}/claim")
    public ApiResponse<TaskMemberVO> claim(@PathVariable Long spaceId,
                                           @PathVariable Long taskId,
                                           @Valid @RequestBody TaskClaimRequest request) {
        return ApiResponse.success(taskService.claim(spaceId, taskId, request));
    }

    /**
     * 指派任务职责。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     * @param request 指派请求
     * @return 成员工作项
     */
    @SaCheckPermission("space:task:assign")
    @Operation(summary = "指派任务职责")
    @PostMapping("/{taskId}/assign")
    public ApiResponse<TaskMemberVO> assign(@PathVariable Long spaceId,
                                            @PathVariable Long taskId,
                                            @Valid @RequestBody TaskAssignmentRequest request) {
        return ApiResponse.success(taskService.assign(spaceId, taskId, request));
    }

    /**
     * 移除任务成员职责。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     * @param memberId 成员工作项标识
     * @return 空响应
     */
    @SaCheckPermission("space:task:assign")
    @Operation(summary = "移除任务成员职责")
    @DeleteMapping("/{taskId}/members/{memberId}")
    public ApiResponse<Void> removeMember(@PathVariable Long spaceId,
                                          @PathVariable Long taskId,
                                          @PathVariable Long memberId) {
        taskService.removeMember(spaceId, taskId, memberId);
        return ApiResponse.success();
    }

    /**
     * 查询任务评论。
     *
     * @param spaceId 空间标识
     * @param id 任务标识
     * @return 评论列表
     */
    @SaCheckPermission("space:comment:add")
    @Operation(summary = "查询任务评论")
    @GetMapping("/{id}/comments")
    public ApiResponse<List<TaskCommentVO>> listComments(@PathVariable Long spaceId, @PathVariable Long id) {
        return ApiResponse.success(taskService.listComments(spaceId, id));
    }

    /**
     * 添加任务评论。
     *
     * @param spaceId 空间标识
     * @param id 任务标识
     * @param request 评论请求
     * @return 评论详情
     */
    @SaCheckPermission("space:comment:add")
    @Operation(summary = "添加任务评论")
    @PostMapping("/{id}/comments")
    public ApiResponse<TaskCommentVO> addComment(@PathVariable Long spaceId,
                                                 @PathVariable Long id,
                                                 @Valid @RequestBody TaskCommentCreateRequest request) {
        return ApiResponse.success(taskService.addComment(spaceId, id, request));
    }
}
