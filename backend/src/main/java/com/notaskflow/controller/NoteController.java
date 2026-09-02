package com.notaskflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.notaskflow.common.ApiResponse;
import com.notaskflow.common.PageResponse;
import com.notaskflow.common.enums.NoteExportFormat;
import com.notaskflow.domain.dto.request.CollabContentSaveRequest;
import com.notaskflow.domain.dto.request.NoteSaveRequest;
import com.notaskflow.domain.dto.request.NoteShareRequest;
import com.notaskflow.domain.query.NoteQuery;
import com.notaskflow.domain.vo.CollabTicketVO;
import com.notaskflow.domain.vo.NoteExportFileVO;
import com.notaskflow.domain.vo.NoteHistoryVO;
import com.notaskflow.domain.vo.NoteVO;
import com.notaskflow.service.NoteExportService;
import com.notaskflow.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 笔记控制器。
 *
 * @author LIN
 */
@Validated
@Tag(name = "云笔记")
@RestController
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    private final NoteExportService noteExportService;

    /**
     * 分页查询笔记。
     *
     * @param spaceId 空间标识
     * @param query 查询条件
     * @return 分页笔记
     */
    @SaCheckPermission("space:note:view")
    @Operation(summary = "分页查询笔记")
    @GetMapping("/api/v1/spaces/{spaceId}/notes")
    public ApiResponse<PageResponse<NoteVO>> page(@PathVariable Long spaceId,
                                                  @Valid @ModelAttribute NoteQuery query) {
        return ApiResponse.success(noteService.page(spaceId, query));
    }

    /**
     * 搜索笔记。
     *
     * @param spaceId 空间标识
     * @param keyword 关键词
     * @return 笔记列表
     */
    @SaCheckPermission("space:note:view")
    @Operation(summary = "搜索笔记")
    @GetMapping("/api/v1/spaces/{spaceId}/notes/search")
    public ApiResponse<List<NoteVO>> search(@PathVariable Long spaceId,
                                            @RequestParam String keyword) {
        return ApiResponse.success(noteService.search(spaceId, keyword));
    }

    /**
     * 查询笔记详情。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     * @return 笔记详情
     */
    @SaCheckPermission("space:note:view")
    @Operation(summary = "查询笔记详情")
    @GetMapping("/api/v1/spaces/{spaceId}/notes/{id}")
    public ApiResponse<NoteVO> get(@PathVariable Long spaceId, @PathVariable Long id) {
        return ApiResponse.success(noteService.get(spaceId, id));
    }

    /**
     * 导出笔记。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     * @param format 导出格式
     * @return 导出文件流
     */
    @SaCheckPermission("space:note:view")
    @Operation(summary = "导出笔记")
    @GetMapping("/api/v1/spaces/{spaceId}/notes/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long spaceId,
                                         @PathVariable Long id,
                                         @RequestParam String format) {
        NoteExportFileVO file = noteExportService.export(spaceId, id, NoteExportFormat.from(format));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, attachmentDisposition(file.getFileName()))
                .body(file.getContent());
    }

    /**
     * 创建笔记。
     *
     * @param spaceId 空间标识
     * @param request 保存请求
     * @return 笔记详情
     */
    @SaCheckPermission("space:note:edit")
    @Operation(summary = "创建笔记")
    @PostMapping("/api/v1/spaces/{spaceId}/notes")
    public ApiResponse<NoteVO> create(@PathVariable Long spaceId,
                                      @Valid @RequestBody NoteSaveRequest request) {
        return ApiResponse.success(noteService.create(spaceId, request));
    }

    /**
     * 更新笔记。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     * @param request 保存请求
     * @return 笔记详情
     */
    @SaCheckPermission("space:note:edit")
    @Operation(summary = "更新笔记")
    @PutMapping("/api/v1/spaces/{spaceId}/notes/{id}")
    public ApiResponse<NoteVO> update(@PathVariable Long spaceId,
                                      @PathVariable Long id,
                                      @Valid @RequestBody NoteSaveRequest request) {
        return ApiResponse.success(noteService.update(spaceId, id, request));
    }

    /**
     * 签发协作文档一次性 Ticket。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     * @return 协作 Ticket
     */
    @SaCheckPermission("space:note:edit")
    @Operation(summary = "签发协作文档Ticket")
    @PostMapping("/api/v1/spaces/{spaceId}/notes/{id}/collab-ticket")
    public ApiResponse<CollabTicketVO> createCollabTicket(@PathVariable Long spaceId,
                                                          @PathVariable Long id) {
        return ApiResponse.success(noteService.createCollabTicket(spaceId, id));
    }

    /**
     * 保存协作文档正文。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     * @param request 协同正文保存请求
     * @return 笔记详情
     */
    @SaCheckPermission("space:note:edit")
    @Operation(summary = "保存协作文档正文")
    @PutMapping("/api/v1/spaces/{spaceId}/notes/{id}/collab-content")
    public ApiResponse<NoteVO> saveCollabContent(@PathVariable Long spaceId,
                                                 @PathVariable Long id,
                                                 @Valid @RequestBody CollabContentSaveRequest request) {
        return ApiResponse.success(noteService.saveCollabContent(spaceId, id, request));
    }

    /**
     * 创建协作文档检查点版本。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     * @param request 协同正文保存请求
     * @return 笔记详情
     */
    @SaCheckPermission("space:note:edit")
    @Operation(summary = "创建协作文档检查点版本")
    @PostMapping("/api/v1/spaces/{spaceId}/notes/{id}/checkpoints")
    public ApiResponse<NoteVO> createCheckpoint(@PathVariable Long spaceId,
                                                @PathVariable Long id,
                                                @Valid @RequestBody CollabContentSaveRequest request) {
        return ApiResponse.success(noteService.createCheckpoint(spaceId, id, request));
    }

    /**
     * 删除笔记。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     * @return 空响应
     */
    @SaCheckPermission("space:note:edit")
    @Operation(summary = "删除笔记")
    @DeleteMapping("/api/v1/spaces/{spaceId}/notes/{id}")
    public ApiResponse<Void> delete(@PathVariable Long spaceId, @PathVariable Long id) {
        noteService.delete(spaceId, id);
        return ApiResponse.success();
    }

    /**
     * 查询历史版本。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     * @return 历史版本列表
     */
    @SaCheckPermission("space:note:view")
    @Operation(summary = "查询历史版本")
    @GetMapping("/api/v1/spaces/{spaceId}/notes/{id}/history")
    public ApiResponse<List<NoteHistoryVO>> listHistory(@PathVariable Long spaceId, @PathVariable Long id) {
        return ApiResponse.success(noteService.listHistory(spaceId, id));
    }

    /**
     * 查询指定历史版本。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     * @param version 版本号
     * @return 历史版本
     */
    @SaCheckPermission("space:note:view")
    @Operation(summary = "查询指定历史版本")
    @GetMapping("/api/v1/spaces/{spaceId}/notes/{id}/history/{version}")
    public ApiResponse<NoteHistoryVO> getHistory(@PathVariable Long spaceId,
                                                 @PathVariable Long id,
                                                 @PathVariable Integer version) {
        return ApiResponse.success(noteService.getHistory(spaceId, id, version));
    }

    /**
     * 回滚到指定历史版本。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     * @param version 版本号
     * @return 笔记详情
     */
    @SaCheckPermission("space:note:edit")
    @Operation(summary = "回滚历史版本")
    @PostMapping("/api/v1/spaces/{spaceId}/notes/{id}/history/{version}/restore")
    public ApiResponse<NoteVO> restore(@PathVariable Long spaceId,
                                       @PathVariable Long id,
                                       @PathVariable Integer version) {
        return ApiResponse.success(noteService.restore(spaceId, id, version));
    }

    /**
     * 分享笔记。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     * @param request 分享请求
     * @return 笔记详情
     */
    @SaCheckPermission("space:note:share")
    @Operation(summary = "分享笔记")
    @PostMapping("/api/v1/spaces/{spaceId}/notes/{id}/share")
    public ApiResponse<NoteVO> share(@PathVariable Long spaceId,
                                     @PathVariable Long id,
                                     @RequestBody NoteShareRequest request) {
        return ApiResponse.success(noteService.share(spaceId, id, request));
    }

    /**
     * 公开访问笔记。
     *
     * @param shareCode 分享码
     * @return 笔记详情
     */
    @Operation(summary = "公开访问笔记")
    @GetMapping("/api/v1/public/notes/{shareCode}")
    public ApiResponse<NoteVO> publicAccess(@PathVariable String shareCode) {
        return ApiResponse.success(noteService.publicAccess(shareCode));
    }

    private String attachmentDisposition(String fileName) {
        String safeFileName = fileName == null || fileName.isBlank() ? "note" : fileName;
        String encodedFileName = URLEncoder.encode(safeFileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "attachment; filename*=UTF-8''" + encodedFileName;
    }
}
