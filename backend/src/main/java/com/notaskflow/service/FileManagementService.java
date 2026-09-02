package com.notaskflow.service;

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
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 空间文件管理服务。
 *
 * @author LIN
 */
public interface FileManagementService {

    /**
     * 分页查询文件。
     *
     * @param spaceId 空间标识
     * @param query 查询条件
     * @return 文件分页
     */
    PageResponse<ManagedFileVO> page(Long spaceId, ManagedFileQuery query);

    /**
     * 查询文件详情。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 文件信息
     */
    ManagedFileVO detail(Long spaceId, Long fileId);

    /**
     * 查询文件夹树。
     *
     * @param spaceId 空间标识
     * @return 文件夹树
     */
    List<FileFolderVO> tree(Long spaceId);

    /**
     * 查询文件上传配置。
     *
     * @param spaceId 空间标识
     * @return 文件上传配置
     */
    FileUploadConfigVO uploadConfig(Long spaceId);

    /**
     * 更新文件上传配置。
     *
     * @param spaceId 空间标识
     * @param request 上传配置更新请求
     * @return 文件上传配置
     */
    FileUploadConfigVO updateUploadConfig(Long spaceId, FileUploadConfigUpdateRequest request);

    /**
     * 创建文件夹。
     *
     * @param spaceId 空间标识
     * @param request 文件夹保存请求
     * @return 文件夹信息
     */
    FileFolderVO createFolder(Long spaceId, FileFolderSaveRequest request);

    /**
     * 更新文件夹。
     *
     * @param spaceId 空间标识
     * @param folderId 文件夹标识
     * @param request 文件夹保存请求
     * @return 文件夹信息
     */
    FileFolderVO updateFolder(Long spaceId, Long folderId, FileFolderSaveRequest request);

    /**
     * 删除文件夹。
     *
     * @param spaceId 空间标识
     * @param folderId 文件夹标识
     */
    void deleteFolder(Long spaceId, Long folderId);

    /**
     * 上传并登记文件。
     *
     * @param spaceId 空间标识
     * @param folderId 文件夹标识
     * @param file 上传文件
     * @return 文件信息
     */
    ManagedFileVO upload(Long spaceId, Long folderId, MultipartFile file);

    /**
     * 从编辑器上传文件并纳入文件管理。
     *
     * @param spaceId 空间标识
     * @param file 上传文件
     * @return 文件信息
     */
    ManagedFileVO uploadFromEditor(Long spaceId, MultipartFile file);

    /**
     * 初始化文件分片上传。
     *
     * @param spaceId 空间标识
     * @param request 分片上传初始化请求
     * @return 分片上传会话
     */
    ManagedFileChunkUploadVO initChunkUpload(Long spaceId, ManagedFileChunkUploadInitRequest request);

    /**
     * 上传文件分片。
     *
     * @param spaceId 空间标识
     * @param uploadToken 上传会话令牌
     * @param chunkIndex 分片序号
     * @param chunk 分片文件
     */
    void uploadChunk(Long spaceId, String uploadToken, Integer chunkIndex, MultipartFile chunk);

    /**
     * 完成文件分片上传。
     *
     * @param spaceId 空间标识
     * @param request 上传完成请求
     * @return 文件信息
     */
    ManagedFileVO completeChunkUpload(Long spaceId, ManagedFileUploadCompleteRequest request);

    /**
     * 创建文件预签名上传地址。
     *
     * @param spaceId 空间标识
     * @param request 上传地址申请请求
     * @return 预签名上传地址
     */
    ManagedFileUploadUrlVO createUploadUrl(Long spaceId, ManagedFileUploadUrlRequest request);

    /**
     * 完成文件直传登记。
     *
     * @param spaceId 空间标识
     * @param request 上传完成请求
     * @return 文件信息
     */
    ManagedFileVO completeUpload(Long spaceId, ManagedFileUploadCompleteRequest request);

    /**
     * 更新文件名称或所在文件夹。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @param request 文件更新请求
     * @return 文件信息
     */
    ManagedFileVO updateFile(Long spaceId, Long fileId, ManagedFileUpdateRequest request);

    /**
     * 生成文件下载地址。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 文件信息
     */
    ManagedFileVO download(Long spaceId, Long fileId);

    /**
     * 生成文件预览地址。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 文件信息
     */
    ManagedFileVO preview(Long spaceId, Long fileId);

    /**
     * 查询文件预览资源元信息。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 文件预览资源信息
     */
    FilePreviewResourceVO previewResource(Long spaceId, Long fileId);

    /**
     * 查询文件文本预览内容。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 文件文本预览内容
     */
    FilePreviewTextVO previewText(Long spaceId, Long fileId);

    /**
     * 查询文件 HTML 预览内容。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 文件 HTML 预览内容
     */
    FilePreviewHtmlVO previewHtml(Long spaceId, Long fileId);

    /**
     * 写出文件预览内容。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @param outputStream 输出流
     * @throws IOException 写出失败
     */
    void writePreviewContent(Long spaceId, Long fileId, OutputStream outputStream) throws IOException;

    /**
     * 将文件放入回收站。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     */
    void trash(Long spaceId, Long fileId);

    /**
     * 从回收站恢复文件。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 文件信息
     */
    ManagedFileVO restore(Long spaceId, Long fileId);

    /**
     * 物理删除文件。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @param force 是否强制删除被引用文件
     */
    void physicalDelete(Long spaceId, Long fileId, boolean force);

    /**
     * 查询文件引用关系。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 引用列表
     */
    List<FileReferenceVO> references(Long spaceId, Long fileId);

    /**
     * 分页查询文件操作日志。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 文件操作日志分页
     */
    PageResponse<FileOperationLogVO> operationLogs(Long spaceId, Long fileId, long pageNum, long pageSize);

    /**
     * 查询文件统计。
     *
     * @param spaceId 空间标识
     * @return 文件统计
     */
    FileStatsVO stats(Long spaceId);
}
