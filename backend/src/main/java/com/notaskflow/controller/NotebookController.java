package com.notaskflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.notaskflow.common.ApiResponse;
import com.notaskflow.domain.dto.request.NotebookSaveRequest;
import com.notaskflow.domain.vo.NotebookVO;
import com.notaskflow.service.NotebookService;
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
 * 笔记本控制器。
 *
 * @author LIN
 */
@Tag(name = "笔记本管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/spaces/{spaceId}/notebooks")
public class NotebookController {

    private final NotebookService notebookService;

    /**
     * 查询笔记本树。
     *
     * @param spaceId 空间标识
     * @return 笔记本树
     */
    @SaCheckPermission("space:note:view")
    @Operation(summary = "查询笔记本树")
    @GetMapping
    public ApiResponse<List<NotebookVO>> listTree(@PathVariable Long spaceId) {
        return ApiResponse.success(notebookService.listTree(spaceId));
    }

    /**
     * 查询笔记本详情。
     *
     * @param spaceId 空间标识
     * @param id 笔记本标识
     * @return 笔记本详情
     */
    @SaCheckPermission("space:note:view")
    @Operation(summary = "查询笔记本详情")
    @GetMapping("/{id}")
    public ApiResponse<NotebookVO> get(@PathVariable Long spaceId, @PathVariable Long id) {
        return ApiResponse.success(notebookService.get(spaceId, id));
    }

    /**
     * 创建笔记本。
     *
     * @param spaceId 空间标识
     * @param request 保存请求
     * @return 笔记本详情
     */
    @SaCheckPermission("space:note:edit")
    @Operation(summary = "创建笔记本")
    @PostMapping
    public ApiResponse<NotebookVO> create(@PathVariable Long spaceId,
                                          @Valid @RequestBody NotebookSaveRequest request) {
        return ApiResponse.success(notebookService.create(spaceId, request));
    }

    /**
     * 更新笔记本。
     *
     * @param spaceId 空间标识
     * @param id 笔记本标识
     * @param request 保存请求
     * @return 笔记本详情
     */
    @SaCheckPermission("space:note:edit")
    @Operation(summary = "更新笔记本")
    @PutMapping("/{id}")
    public ApiResponse<NotebookVO> update(@PathVariable Long spaceId,
                                          @PathVariable Long id,
                                          @Valid @RequestBody NotebookSaveRequest request) {
        return ApiResponse.success(notebookService.update(spaceId, id, request));
    }

    /**
     * 删除笔记本。
     *
     * @param spaceId 空间标识
     * @param id 笔记本标识
     * @return 空响应
     */
    @SaCheckPermission("space:note:edit")
    @Operation(summary = "删除笔记本")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long spaceId, @PathVariable Long id) {
        notebookService.delete(spaceId, id);
        return ApiResponse.success();
    }
}
