package com.notaskflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.notaskflow.common.ApiResponse;
import com.notaskflow.domain.dto.request.NoteTagBindRequest;
import com.notaskflow.domain.dto.request.TagSaveRequest;
import com.notaskflow.domain.vo.TagVO;
import com.notaskflow.service.TagService;
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
 * 标签控制器。
 *
 * @author LIN
 */
@Tag(name = "标签管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/spaces/{spaceId}")
public class TagController {

    private final TagService tagService;

    /**
     * 查询空间标签。
     *
     * @param spaceId 空间标识
     * @return 标签列表
     */
    @SaCheckPermission("space:note:view")
    @Operation(summary = "查询空间标签")
    @GetMapping("/tags")
    public ApiResponse<List<TagVO>> list(@PathVariable Long spaceId) {
        return ApiResponse.success(tagService.list(spaceId));
    }

    /**
     * 创建标签。
     *
     * @param spaceId 空间标识
     * @param request 保存请求
     * @return 标签详情
     */
    @SaCheckPermission("space:note:edit")
    @Operation(summary = "创建标签")
    @PostMapping("/tags")
    public ApiResponse<TagVO> create(@PathVariable Long spaceId,
                                     @Valid @RequestBody TagSaveRequest request) {
        return ApiResponse.success(tagService.create(spaceId, request));
    }

    /**
     * 更新标签。
     *
     * @param spaceId 空间标识
     * @param id 标签标识
     * @param request 保存请求
     * @return 标签详情
     */
    @SaCheckPermission("space:note:edit")
    @Operation(summary = "更新标签")
    @PutMapping("/tags/{id}")
    public ApiResponse<TagVO> update(@PathVariable Long spaceId,
                                     @PathVariable Long id,
                                     @Valid @RequestBody TagSaveRequest request) {
        return ApiResponse.success(tagService.update(spaceId, id, request));
    }

    /**
     * 删除标签。
     *
     * @param spaceId 空间标识
     * @param id 标签标识
     * @return 空响应
     */
    @SaCheckPermission("space:note:edit")
    @Operation(summary = "删除标签")
    @DeleteMapping("/tags/{id}")
    public ApiResponse<Void> delete(@PathVariable Long spaceId, @PathVariable Long id) {
        tagService.delete(spaceId, id);
        return ApiResponse.success();
    }

    /**
     * 绑定笔记标签。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     * @param request 绑定请求
     * @return 空响应
     */
    @SaCheckPermission("space:note:edit")
    @Operation(summary = "绑定笔记标签")
    @PostMapping("/notes/{id}/tags")
    public ApiResponse<Void> bindTags(@PathVariable Long spaceId,
                                      @PathVariable Long id,
                                      @Valid @RequestBody NoteTagBindRequest request) {
        tagService.bindTags(spaceId, id, request);
        return ApiResponse.success();
    }

    /**
     * 移除笔记标签。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     * @param tagId 标签标识
     * @return 空响应
     */
    @SaCheckPermission("space:note:edit")
    @Operation(summary = "移除笔记标签")
    @DeleteMapping("/notes/{id}/tags/{tagId}")
    public ApiResponse<Void> removeTag(@PathVariable Long spaceId,
                                       @PathVariable Long id,
                                       @PathVariable Long tagId) {
        tagService.removeTag(spaceId, id, tagId);
        return ApiResponse.success();
    }
}
