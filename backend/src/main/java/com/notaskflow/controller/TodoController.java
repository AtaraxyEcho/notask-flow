package com.notaskflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.notaskflow.common.ApiResponse;
import com.notaskflow.common.PageResponse;
import com.notaskflow.domain.dto.request.TodoSaveRequest;
import com.notaskflow.domain.query.TodoQuery;
import com.notaskflow.domain.vo.TodoVO;
import com.notaskflow.service.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
 * 待办控制器。
 *
 * @author LIN
 */
@Validated
@Tag(name = "待办管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/spaces/{spaceId}/todos")
public class TodoController {

    private final TodoService todoService;

    /**
     * 分页查询待办。
     *
     * @param spaceId 空间标识
     * @param query 查询条件
     * @return 分页待办
     */
    @SaCheckPermission("space:todo:view")
    @Operation(summary = "分页查询待办")
    @GetMapping
    public ApiResponse<PageResponse<TodoVO>> page(@PathVariable Long spaceId,
                                                  @Valid @ModelAttribute TodoQuery query) {
        return ApiResponse.success(todoService.page(spaceId, query));
    }

    /**
     * 查询待办详情。
     *
     * @param spaceId 空间标识
     * @param id 待办标识
     * @return 待办详情
     */
    @SaCheckPermission("space:todo:view")
    @Operation(summary = "查询待办详情")
    @GetMapping("/{id}")
    public ApiResponse<TodoVO> get(@PathVariable Long spaceId, @PathVariable Long id) {
        return ApiResponse.success(todoService.get(spaceId, id));
    }

    /**
     * 创建待办。
     *
     * @param spaceId 空间标识
     * @param request 保存请求
     * @return 待办详情
     */
    @SaCheckPermission("space:todo:edit")
    @Operation(summary = "创建待办")
    @PostMapping
    public ApiResponse<TodoVO> create(@PathVariable Long spaceId,
                                      @Valid @RequestBody TodoSaveRequest request) {
        return ApiResponse.success(todoService.create(spaceId, request));
    }

    /**
     * 更新待办。
     *
     * @param spaceId 空间标识
     * @param id 待办标识
     * @param request 保存请求
     * @return 待办详情
     */
    @SaCheckPermission("space:todo:edit")
    @Operation(summary = "更新待办")
    @PutMapping("/{id}")
    public ApiResponse<TodoVO> update(@PathVariable Long spaceId,
                                      @PathVariable Long id,
                                      @Valid @RequestBody TodoSaveRequest request) {
        return ApiResponse.success(todoService.update(spaceId, id, request));
    }

    /**
     * 标记待办完成。
     *
     * @param spaceId 空间标识
     * @param id 待办标识
     * @return 待办详情
     */
    @SaCheckPermission("space:todo:edit")
    @Operation(summary = "标记待办完成")
    @PutMapping("/{id}/complete")
    public ApiResponse<TodoVO> complete(@PathVariable Long spaceId, @PathVariable Long id) {
        return ApiResponse.success(todoService.complete(spaceId, id));
    }

    /**
     * 标记待办未完成。
     *
     * @param spaceId 空间标识
     * @param id 待办标识
     * @return 待办详情
     */
    @SaCheckPermission("space:todo:edit")
    @Operation(summary = "标记待办未完成")
    @PutMapping("/{id}/uncomplete")
    public ApiResponse<TodoVO> uncomplete(@PathVariable Long spaceId, @PathVariable Long id) {
        return ApiResponse.success(todoService.uncomplete(spaceId, id));
    }

    /**
     * 删除待办。
     *
     * @param spaceId 空间标识
     * @param id 待办标识
     * @return 空响应
     */
    @SaCheckPermission("space:todo:edit")
    @Operation(summary = "删除待办")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long spaceId, @PathVariable Long id) {
        todoService.delete(spaceId, id);
        return ApiResponse.success();
    }
}
