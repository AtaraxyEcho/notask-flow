package com.notaskflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.notaskflow.common.ApiResponse;
import com.notaskflow.domain.dto.request.AttachmentBindRequest;
import com.notaskflow.domain.dto.request.AttachmentUnbindRequest;
import com.notaskflow.domain.vo.AttachmentVO;
import com.notaskflow.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 附件控制器，提供空间内附件上传、下载、绑定和查询接口。
 *
 * @author LIN
 */
@Tag(name = "附件管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/spaces/{spaceId}")
public class AttachmentController {

    private final AttachmentService attachmentService;

    /**
     * 上传空间附件。
     *
     * @param spaceId 空间标识
     * @param file 文件对象
     * @return 附件信息
     */
    @SaCheckPermission("space:file:manage")
    @Operation(summary = "上传附件")
    @PostMapping("/attachments")
    public ApiResponse<AttachmentVO> upload(@PathVariable Long spaceId,
                                            @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(attachmentService.upload(spaceId, file));
    }

    /**
     * 获取空间附件元信息。
     *
     * @param spaceId 空间标识
     * @param id 附件标识
     * @return 附件信息
     */
    @SaCheckPermission("space:file:view")
    @Operation(summary = "获取附件元信息")
    @GetMapping("/attachments/{id}")
    public ApiResponse<AttachmentVO> get(@PathVariable Long spaceId, @PathVariable Long id) {
        return ApiResponse.success(attachmentService.get(spaceId, id));
    }

    /**
     * 生成空间附件下载地址。
     *
     * @param spaceId 空间标识
     * @param id 附件标识
     * @return 附件信息
     */
    @SaCheckPermission("space:file:view")
    @Operation(summary = "生成附件下载地址")
    @GetMapping("/attachments/{id}/download")
    public ApiResponse<AttachmentVO> download(@PathVariable Long spaceId, @PathVariable Long id) {
        return ApiResponse.success(attachmentService.download(spaceId, id));
    }

    /**
     * 删除空间附件。
     *
     * @param spaceId 空间标识
     * @param id 附件标识
     * @return 空响应
     */
    @SaCheckPermission("space:file:manage")
    @Operation(summary = "删除附件")
    @DeleteMapping("/attachments/{id}")
    public ApiResponse<Void> delete(@PathVariable Long spaceId, @PathVariable Long id) {
        attachmentService.delete(spaceId, id);
        return ApiResponse.success();
    }

    /**
     * 绑定附件到空间内业务对象。
     *
     * @param spaceId 空间标识
     * @param request 绑定请求
     * @return 空响应
     */
    @SaCheckPermission(value = {"space:file:manage", "space:note:edit"}, mode = SaMode.OR)
    @Operation(summary = "绑定附件")
    @PostMapping("/attachments/bind")
    public ApiResponse<Void> bind(@PathVariable Long spaceId,
                                  @Valid @RequestBody AttachmentBindRequest request) {
        attachmentService.bind(spaceId, request);
        return ApiResponse.success();
    }

    /**
     * 解除附件与空间内业务对象的绑定。
     *
     * @param spaceId 空间标识
     * @param id 附件标识
     * @param request 解绑请求
     * @return 空响应
     */
    @SaCheckPermission(value = {"space:file:manage", "space:note:edit"}, mode = SaMode.OR)
    @Operation(summary = "解除附件绑定")
    @DeleteMapping("/attachments/{id}/unbind")
    public ApiResponse<Void> unbind(@PathVariable Long spaceId,
                                    @PathVariable Long id,
                                    @Valid @RequestBody AttachmentUnbindRequest request) {
        attachmentService.unbind(spaceId, id, request);
        return ApiResponse.success();
    }

    /**
     * 查询笔记关联的附件。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     * @return 附件列表
     */
    @SaCheckPermission(value = {"space:file:view", "space:note:view"}, mode = SaMode.AND)
    @Operation(summary = "查询笔记附件")
    @GetMapping("/notes/{id}/attachments")
    public ApiResponse<List<AttachmentVO>> listNoteAttachments(@PathVariable Long spaceId, @PathVariable Long id) {
        return ApiResponse.success(attachmentService.listNoteAttachments(spaceId, id));
    }

    /**
     * 查询任务关联的附件。
     *
     * @param spaceId 空间标识
     * @param id 任务标识
     * @return 附件列表
     */
    @SaCheckPermission(value = {"space:file:view", "space:task:view"}, mode = SaMode.AND)
    @Operation(summary = "查询任务附件")
    @GetMapping("/tasks/{id}/attachments")
    public ApiResponse<List<AttachmentVO>> listTaskAttachments(@PathVariable Long spaceId, @PathVariable Long id) {
        return ApiResponse.success(attachmentService.listTaskAttachments(spaceId, id));
    }
}
