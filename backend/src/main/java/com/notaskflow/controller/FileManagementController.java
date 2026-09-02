package com.notaskflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.notaskflow.common.ApiResponse;
import com.notaskflow.common.PageResponse;
import com.notaskflow.domain.dto.request.FileFolderSaveRequest;
import com.notaskflow.domain.dto.request.FileUploadConfigUpdateRequest;
import com.notaskflow.domain.dto.request.ManagedFileChunkUploadInitRequest;
import com.notaskflow.domain.dto.request.ManagedFileUploadCompleteRequest;
import com.notaskflow.domain.dto.request.ManagedFileUploadUrlRequest;
import com.notaskflow.domain.dto.request.ManagedFileUpdateRequest;
import com.notaskflow.domain.query.ManagedFileQuery;
import com.notaskflow.domain.vo.FileFolderVO;
import com.notaskflow.domain.vo.FileOperationLogVO;
import com.notaskflow.domain.vo.FilePreviewHtmlVO;
import com.notaskflow.domain.vo.FilePreviewResourceVO;
import com.notaskflow.domain.vo.FilePreviewTextVO;
import com.notaskflow.domain.vo.FileReferenceVO;
import com.notaskflow.domain.vo.FileStatsVO;
import com.notaskflow.domain.vo.FileUploadConfigVO;
import com.notaskflow.domain.vo.ManagedFileChunkUploadVO;
import com.notaskflow.domain.vo.ManagedFileVO;
import com.notaskflow.domain.vo.ManagedFileUploadUrlVO;
import com.notaskflow.service.FileManagementService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * 文件管理控制器，提供个人空间和团队空间的文件资产管理接口。
 *
 * @author LIN
 */
@Tag(name = "文件管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/spaces/{spaceId}/files")
public class FileManagementController {

    private final FileManagementService fileManagementService;

    /**
     * 分页查询文件。
     *
     * @param spaceId 空间标识
     * @param query 查询条件
     * @return 文件分页
     */
    @SaCheckPermission("space:file:view")
    @Operation(summary = "分页查询文件")
    @GetMapping
    public ApiResponse<PageResponse<ManagedFileVO>> page(@PathVariable Long spaceId,
                                                         @Valid @ModelAttribute ManagedFileQuery query) {
        return ApiResponse.success(fileManagementService.page(spaceId, query));
    }

    /**
     * 查询文件详情。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 文件信息
     */
    @SaCheckPermission("space:file:view")
    @Operation(summary = "查询文件详情")
    @GetMapping("/{fileId}")
    public ApiResponse<ManagedFileVO> detail(@PathVariable Long spaceId, @PathVariable Long fileId) {
        return ApiResponse.success(fileManagementService.detail(spaceId, fileId));
    }

    /**
     * 查询文件夹树。
     *
     * @param spaceId 空间标识
     * @return 文件夹树
     */
    @SaCheckPermission("space:file:view")
    @Operation(summary = "查询文件夹树")
    @GetMapping("/tree")
    public ApiResponse<List<FileFolderVO>> tree(@PathVariable Long spaceId) {
        return ApiResponse.success(fileManagementService.tree(spaceId));
    }

    /**
     * 查询文件上传配置。
     *
     * @param spaceId 空间标识
     * @return 文件上传配置
     */
    @SaCheckPermission("space:file:view")
    @Operation(summary = "查询文件上传配置")
    @GetMapping("/upload-config")
    public ApiResponse<FileUploadConfigVO> uploadConfig(@PathVariable Long spaceId) {
        return ApiResponse.success(fileManagementService.uploadConfig(spaceId));
    }

    /**
     * 更新文件上传配置。
     *
     * @param spaceId 空间标识
     * @param request 文件上传配置更新请求
     * @return 文件上传配置
     */
    @SaCheckPermission("space:file:manage")
    @Operation(summary = "更新文件上传配置")
    @PutMapping("/upload-config")
    public ApiResponse<FileUploadConfigVO> updateUploadConfig(@PathVariable Long spaceId,
                                                              @Valid @RequestBody
                                                              FileUploadConfigUpdateRequest request) {
        return ApiResponse.success(fileManagementService.updateUploadConfig(spaceId, request));
    }

    /**
     * 创建文件夹。
     *
     * @param spaceId 空间标识
     * @param request 文件夹保存请求
     * @return 文件夹信息
     */
    @SaCheckPermission("space:file:manage")
    @Operation(summary = "创建文件夹")
    @PostMapping("/folders")
    public ApiResponse<FileFolderVO> createFolder(@PathVariable Long spaceId,
                                                  @Valid @RequestBody FileFolderSaveRequest request) {
        return ApiResponse.success(fileManagementService.createFolder(spaceId, request));
    }

    /**
     * 更新文件夹。
     *
     * @param spaceId 空间标识
     * @param folderId 文件夹标识
     * @param request 文件夹保存请求
     * @return 文件夹信息
     */
    @SaCheckPermission("space:file:manage")
    @Operation(summary = "更新文件夹")
    @PutMapping("/folders/{folderId}")
    public ApiResponse<FileFolderVO> updateFolder(@PathVariable Long spaceId,
                                                  @PathVariable Long folderId,
                                                  @Valid @RequestBody FileFolderSaveRequest request) {
        return ApiResponse.success(fileManagementService.updateFolder(spaceId, folderId, request));
    }

    /**
     * 删除文件夹。
     *
     * @param spaceId 空间标识
     * @param folderId 文件夹标识
     * @return 空响应
     */
    @SaCheckPermission("space:file:manage")
    @Operation(summary = "删除文件夹")
    @DeleteMapping("/folders/{folderId}")
    public ApiResponse<Void> deleteFolder(@PathVariable Long spaceId, @PathVariable Long folderId) {
        fileManagementService.deleteFolder(spaceId, folderId);
        return ApiResponse.success();
    }

    /**
     * 上传文件。
     *
     * @param spaceId 空间标识
     * @param folderId 文件夹标识
     * @param file 上传文件
     * @return 文件信息
     */
    @SaCheckPermission("space:file:manage")
    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public ApiResponse<ManagedFileVO> upload(@PathVariable Long spaceId,
                                             @RequestParam(required = false) Long folderId,
                                             @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(fileManagementService.upload(spaceId, folderId, file));
    }

    /**
     * 从编辑器上传文件。
     *
     * @param spaceId 空间标识
     * @param file 上传文件
     * @return 文件信息
     */
    @SaCheckPermission(value = {"space:file:manage", "space:note:edit"}, mode = SaMode.OR)
    @Operation(summary = "从编辑器上传文件")
    @PostMapping("/editor-upload")
    public ApiResponse<ManagedFileVO> uploadFromEditor(@PathVariable Long spaceId,
                                                       @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(fileManagementService.uploadFromEditor(spaceId, file));
    }

    /**
     * 初始化文件分片上传。
     *
     * @param spaceId 空间标识
     * @param request 分片上传初始化请求
     * @return 分片上传会话
     */
    @SaCheckPermission("space:file:manage")
    @Operation(summary = "初始化文件分片上传")
    @PostMapping("/chunk-upload/init")
    public ApiResponse<ManagedFileChunkUploadVO> initChunkUpload(
            @PathVariable Long spaceId,
            @Valid @RequestBody ManagedFileChunkUploadInitRequest request) {
        return ApiResponse.success(fileManagementService.initChunkUpload(spaceId, request));
    }

    /**
     * 上传文件分片。
     *
     * @param spaceId 空间标识
     * @param uploadToken 上传会话令牌
     * @param chunkIndex 分片序号
     * @param chunk 分片文件
     * @return 空响应
     */
    @SaCheckPermission("space:file:manage")
    @Operation(summary = "上传文件分片")
    @PostMapping("/chunk-upload/{uploadToken}/chunks")
    public ApiResponse<Void> uploadChunk(@PathVariable Long spaceId,
                                         @PathVariable String uploadToken,
                                         @RequestParam Integer chunkIndex,
                                         @RequestPart("chunk") MultipartFile chunk) {
        fileManagementService.uploadChunk(spaceId, uploadToken, chunkIndex, chunk);
        return ApiResponse.success();
    }

    /**
     * 完成文件分片上传。
     *
     * @param spaceId 空间标识
     * @param request 上传完成请求
     * @return 文件信息
     */
    @SaCheckPermission("space:file:manage")
    @Operation(summary = "完成文件分片上传")
    @PostMapping("/chunk-upload/complete")
    public ApiResponse<ManagedFileVO> completeChunkUpload(
            @PathVariable Long spaceId,
            @Valid @RequestBody ManagedFileUploadCompleteRequest request) {
        return ApiResponse.success(fileManagementService.completeChunkUpload(spaceId, request));
    }

    /**
     * 创建文件预签名上传地址。
     *
     * @param spaceId 空间标识
     * @param request 上传地址申请请求
     * @return 预签名上传地址
     */
    @SaCheckPermission("space:file:manage")
    @Operation(summary = "创建文件预签名上传地址")
    @PostMapping("/upload-url")
    public ApiResponse<ManagedFileUploadUrlVO> createUploadUrl(
            @PathVariable Long spaceId,
            @Valid @RequestBody ManagedFileUploadUrlRequest request) {
        return ApiResponse.success(fileManagementService.createUploadUrl(spaceId, request));
    }

    /**
     * 完成文件直传登记。
     *
     * @param spaceId 空间标识
     * @param request 上传完成请求
     * @return 文件信息
     */
    @SaCheckPermission("space:file:manage")
    @Operation(summary = "完成文件直传登记")
    @PostMapping("/complete")
    public ApiResponse<ManagedFileVO> completeUpload(@PathVariable Long spaceId,
                                                     @Valid @RequestBody ManagedFileUploadCompleteRequest request) {
        return ApiResponse.success(fileManagementService.completeUpload(spaceId, request));
    }

    /**
     * 更新文件。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @param request 文件更新请求
     * @return 文件信息
     */
    @SaCheckPermission("space:file:manage")
    @Operation(summary = "更新文件")
    @PutMapping("/{fileId}")
    public ApiResponse<ManagedFileVO> updateFile(@PathVariable Long spaceId,
                                                 @PathVariable Long fileId,
                                                 @Valid @RequestBody ManagedFileUpdateRequest request) {
        return ApiResponse.success(fileManagementService.updateFile(spaceId, fileId, request));
    }

    /**
     * 生成文件下载地址。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 文件信息
     */
    @SaCheckPermission("space:file:view")
    @Operation(summary = "生成文件下载地址")
    @GetMapping("/{fileId}/download-url")
    public ApiResponse<ManagedFileVO> download(@PathVariable Long spaceId, @PathVariable Long fileId) {
        return ApiResponse.success(fileManagementService.download(spaceId, fileId));
    }

    /**
     * 生成文件预览地址。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 文件信息
     */
    @SaCheckPermission("space:file:view")
    @Operation(summary = "生成文件预览地址")
    @GetMapping("/{fileId}/preview-url")
    public ApiResponse<ManagedFileVO> preview(@PathVariable Long spaceId, @PathVariable Long fileId) {
        return ApiResponse.success(fileManagementService.preview(spaceId, fileId));
    }

    /**
     * 流式预览文件内容。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 文件预览流
     */
    @SaCheckPermission("space:file:view")
    @Operation(summary = "流式预览文件内容")
    @GetMapping("/{fileId}/preview")
    public ResponseEntity<StreamingResponseBody> previewContent(@PathVariable Long spaceId, @PathVariable Long fileId) {
        FilePreviewResourceVO resource = fileManagementService.previewResource(spaceId, fileId);
        StreamingResponseBody body = outputStream -> fileManagementService.writePreviewContent(spaceId, fileId, outputStream);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(resolvePreviewMediaType(resource.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, inlineDisposition(resource.getFileName()));
        if (resource.getFileSize() != null) {
            builder.contentLength(resource.getFileSize());
        }
        return builder.body(body);
    }

    /**
     * 查询文件文本预览内容。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 文件文本预览内容
     */
    @SaCheckPermission("space:file:view")
    @Operation(summary = "查询文件文本预览内容")
    @GetMapping("/{fileId}/preview-text")
    public ApiResponse<FilePreviewTextVO> previewText(@PathVariable Long spaceId, @PathVariable Long fileId) {
        return ApiResponse.success(fileManagementService.previewText(spaceId, fileId));
    }

    /**
     * 查询文件 HTML 预览内容。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 文件 HTML 预览内容
     */
    @SaCheckPermission("space:file:view")
    @Operation(summary = "查询文件 HTML 预览内容")
    @GetMapping("/{fileId}/preview-html")
    public ApiResponse<FilePreviewHtmlVO> previewHtml(@PathVariable Long spaceId, @PathVariable Long fileId) {
        return ApiResponse.success(fileManagementService.previewHtml(spaceId, fileId));
    }

    /**
     * 文件移入回收站。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @param force 是否强制删除被引用文件
     * @return 空响应
     */
    @SaCheckPermission("space:file:manage")
    @Operation(summary = "文件移入回收站")
    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> trash(@PathVariable Long spaceId, @PathVariable Long fileId) {
        fileManagementService.trash(spaceId, fileId);
        return ApiResponse.success();
    }

    /**
     * 从回收站恢复文件。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 文件信息
     */
    @SaCheckPermission("space:file:manage")
    @Operation(summary = "从回收站恢复文件")
    @PostMapping("/{fileId}/restore")
    public ApiResponse<ManagedFileVO> restore(@PathVariable Long spaceId, @PathVariable Long fileId) {
        return ApiResponse.success(fileManagementService.restore(spaceId, fileId));
    }

    /**
     * 物理删除文件。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 空响应
     */
    @SaCheckPermission("space:file:manage")
    @Operation(summary = "物理删除文件")
    @DeleteMapping("/{fileId}/physical")
    public ApiResponse<Void> physicalDelete(@PathVariable Long spaceId,
                                            @PathVariable Long fileId,
                                            @RequestParam(defaultValue = "false") boolean force) {
        fileManagementService.physicalDelete(spaceId, fileId, force);
        return ApiResponse.success();
    }

    /**
     * 查询文件引用关系。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 引用列表
     */
    @SaCheckPermission("space:file:view")
    @Operation(summary = "查询文件引用关系")
    @GetMapping("/{fileId}/references")
    public ApiResponse<List<FileReferenceVO>> references(@PathVariable Long spaceId, @PathVariable Long fileId) {
        return ApiResponse.success(fileManagementService.references(spaceId, fileId));
    }

    /**
     * 分页查询文件操作日志。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 文件操作日志分页
     */
    @SaCheckPermission("space:file:manage")
    @Operation(summary = "分页查询文件操作日志")
    @GetMapping("/{fileId}/operation-logs")
    public ApiResponse<PageResponse<FileOperationLogVO>> operationLogs(@PathVariable Long spaceId,
                                                                       @PathVariable Long fileId,
                                                                       @RequestParam(defaultValue = "1") long pageNum,
                                                                       @RequestParam(defaultValue = "10")
                                                                       long pageSize) {
        return ApiResponse.success(fileManagementService.operationLogs(spaceId, fileId, pageNum, pageSize));
    }

    /**
     * 查询文件统计。
     *
     * @param spaceId 空间标识
     * @return 文件统计
     */
    @SaCheckPermission("space:file:view")
    @Operation(summary = "查询文件统计")
    @GetMapping("/stats")
    public ApiResponse<FileStatsVO> stats(@PathVariable Long spaceId) {
        return ApiResponse.success(fileManagementService.stats(spaceId));
    }

    private MediaType resolvePreviewMediaType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return MediaType.parseMediaType(mimeType);
    }

    private String inlineDisposition(String fileName) {
        String safeFileName = fileName == null || fileName.isBlank() ? "preview" : fileName;
        String encodedFileName = URLEncoder.encode(safeFileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "inline; filename*=UTF-8''" + encodedFileName;
    }
}
