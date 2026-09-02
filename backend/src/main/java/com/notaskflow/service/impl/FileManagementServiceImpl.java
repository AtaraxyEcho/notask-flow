package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notaskflow.common.PageResponse;
import com.notaskflow.common.constant.RedisKeyConstants;
import com.notaskflow.common.enums.FileProcessOperation;
import com.notaskflow.common.enums.SpaceRealtimeEventType;
import com.notaskflow.common.enums.SpaceType;
import com.notaskflow.common.enums.StatsRefreshScope;
import com.notaskflow.config.FileManagementProperties;
import com.notaskflow.domain.dto.request.FileFolderSaveRequest;
import com.notaskflow.domain.dto.request.FileUploadConfigUpdateRequest;
import com.notaskflow.domain.dto.request.ManagedFileChunkUploadInitRequest;
import com.notaskflow.domain.dto.request.ManagedFileUploadCompleteRequest;
import com.notaskflow.domain.dto.request.ManagedFileUploadUrlRequest;
import com.notaskflow.domain.dto.request.ManagedFileUpdateRequest;
import com.notaskflow.domain.entity.Attachment;
import com.notaskflow.domain.entity.BusinessAttachment;
import com.notaskflow.domain.entity.FileFolder;
import com.notaskflow.domain.entity.FileOperationLog;
import com.notaskflow.domain.entity.ManagedFile;
import com.notaskflow.domain.entity.Space;
import com.notaskflow.domain.query.ManagedFileQuery;
import com.notaskflow.domain.vo.AttachmentVO;
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
import com.notaskflow.event.FileProcessRequestedEvent;
import com.notaskflow.event.StatsRefreshRequestedEvent;
import com.notaskflow.exception.BusinessException;
import com.notaskflow.exception.ErrorCode;
import com.notaskflow.exception.ResourceNotFoundException;
import com.notaskflow.mapper.AttachmentMapper;
import com.notaskflow.mapper.BusinessAttachmentMapper;
import com.notaskflow.mapper.FileFolderMapper;
import com.notaskflow.mapper.FileOperationLogMapper;
import com.notaskflow.mapper.ManagedFileMapper;
import com.notaskflow.mapper.SpaceMapper;
import com.notaskflow.security.PermissionValidator;
import com.notaskflow.service.AttachmentService;
import com.notaskflow.service.FileManagementService;
import com.notaskflow.service.FileSearchService;
import com.notaskflow.service.FileStatsCacheService;
import com.notaskflow.service.FileTextExtractionService;
import com.notaskflow.service.SpaceRealtimeEventService;
import com.notaskflow.storage.MinioStorageService;
import com.notaskflow.utils.LoginUserUtil;
import com.notaskflow.utils.RedisUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 空间文件管理服务实现。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileManagementServiceImpl implements FileManagementService {

    private static final String OPERATION_UPLOAD = "UPLOAD";

    private static final String OPERATION_UPDATE = "UPDATE";

    private static final String OPERATION_TRASH = "TRASH";

    private static final String OPERATION_RESTORE = "RESTORE";

    private static final String OPERATION_PHYSICAL_DELETE = "PHYSICAL_DELETE";

    private static final int FILE_UPLOAD_RATE_LIMIT = 30;

    private static final int FILE_DOWNLOAD_RATE_LIMIT = 120;

    private static final int FILE_SEARCH_LIMIT = 1000;

    private static final long DEFAULT_LOG_PAGE_SIZE = 10L;

    private static final long MAX_LOG_PAGE_SIZE = 100L;

    private static final Duration FILE_RATE_LIMIT_WINDOW = Duration.ofMinutes(10);

    private static final Duration UPLOAD_SESSION_TTL = Duration.ofMinutes(15);

    private static final int UPLOAD_URL_EXPIRES_IN = 900;

    private static final String UPLOAD_URL_METHOD = "PUT";

    private static final String UPLOAD_SESSION_DELIMITER = "\n";

    private static final String CHUNK_FILE_PREFIX = "chunk-";

    private static final String COMBINED_FILE_NAME = "combined";

    private final ManagedFileMapper managedFileMapper;

    private final FileFolderMapper fileFolderMapper;

    private final FileOperationLogMapper fileOperationLogMapper;

    private final AttachmentMapper attachmentMapper;

    private final BusinessAttachmentMapper businessAttachmentMapper;

    private final SpaceMapper spaceMapper;

    private final PermissionValidator permissionValidator;

    private final AttachmentService attachmentService;

    private final FileStatsCacheService fileStatsCacheService;

    private final FileSearchService fileSearchService;

    private final FileTextExtractionService fileTextExtractionService;

    private final SpaceRealtimeEventService spaceRealtimeEventService;

    private final RedisUtil redisUtil;

    private final MinioStorageService minioStorageService;

    private final FileManagementProperties fileManagementProperties;

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 分页查询文件。
     *
     * @param spaceId 空间标识
     * @param query 查询条件
     * @return 文件分页
     */
    @Override
    public PageResponse<ManagedFileVO> page(Long spaceId, ManagedFileQuery query) {
        ensureFileManager(spaceId);
        ManagedFileQuery safeQuery = query == null ? new ManagedFileQuery() : query;
        Optional<List<Long>> searchedFileIds = searchFileIds(spaceId, safeQuery);
        boolean searchIndexApplied = searchedFileIds.isPresent();
        List<ManagedFileVO> filtered = selectManagedFiles(spaceId, safeQuery, searchedFileIds)
                .stream()
                .map(file -> toVO(file, false))
                .filter(file -> matchesMetadataQuery(file, safeQuery))
                .filter(file -> searchIndexApplied || matchesKeyword(file, safeQuery))
                .toList();
        long pageNum = safeQuery.safePageNum();
        long pageSize = safeQuery.safePageSize();
        int fromIndex = (int) Math.min(filtered.size(), (pageNum - 1) * pageSize);
        int toIndex = (int) Math.min(filtered.size(), fromIndex + pageSize);
        Page<ManagedFile> page = new Page<>(pageNum, pageSize);
        page.setTotal(filtered.size());
        return PageResponse.of(page, filtered.subList(fromIndex, toIndex));
    }

    /**
     * 查询文件夹树。
     *
     * @param spaceId 空间标识
     * @return 文件夹树
     */
    @Override
    public List<FileFolderVO> tree(Long spaceId) {
        ensureFileManager(spaceId);
        List<FileFolder> folders = fileFolderMapper.selectList(Wrappers.<FileFolder>lambdaQuery()
                .eq(FileFolder::getSpaceId, spaceId)
                .orderByAsc(FileFolder::getSortOrder)
                .orderByAsc(FileFolder::getName));
        return buildFolderTree(folders);
    }

    /**
     * 查询文件上传配置。
     *
     * @param spaceId 空间标识
     * @return 文件上传配置
     */
    @Override
    public FileUploadConfigVO uploadConfig(Long spaceId) {
        ensureFileManager(spaceId);
        return effectiveUploadConfig(spaceId);
    }

    /**
     * 更新文件上传配置。
     *
     * @param spaceId 空间标识
     * @param request 上传配置更新请求
     * @return 文件上传配置
     */
    @Override
    public FileUploadConfigVO updateUploadConfig(Long spaceId, FileUploadConfigUpdateRequest request) {
        ensureUploadConfigManager(spaceId);
        FileUploadConfigVO normalizedConfig = normalizeUploadConfig(new FileUploadConfigVO(
                request.getMaxFileSize(),
                request.getMultipartThresholdSize(),
                request.getChunkSize(),
                request.getAllowedMimeTypes()
        ));
        redisUtil.set(RedisKeyConstants.spaceFileUploadConfig(spaceId), normalizedConfig);
        spaceRealtimeEventService.publish(spaceId, SpaceRealtimeEventType.FILE_UPLOAD_CONFIG_UPDATED,
                Map.of("action", "uploadConfigUpdated"));
        return normalizedConfig;
    }

    /**
     * 查询文件详情。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 文件信息
     */
    @Override
    public ManagedFileVO detail(Long spaceId, Long fileId) {
        ensureFileManager(spaceId);
        return toVO(findManagedFile(spaceId, fileId), false);
    }

    /**
     * 创建文件夹。
     *
     * @param spaceId 空间标识
     * @param request 文件夹保存请求
     * @return 文件夹信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileFolderVO createFolder(Long spaceId, FileFolderSaveRequest request) {
        ensureFileManager(spaceId);
        Long parentId = normalizeFolderId(request.getParentId());
        ensureFolderExists(spaceId, parentId);
        String name = normalizeFolderName(request.getName());
        ensureFolderNameUnique(spaceId, parentId, name, null);
        FileFolder folder = new FileFolder();
        folder.setSpaceId(spaceId);
        folder.setParentId(parentId);
        folder.setName(name);
        folder.setSortOrder(0);
        folder.setCreatedBy(LoginUserUtil.currentUserId());
        fileFolderMapper.insert(folder);
        publishFolderRealtimeEvent(spaceId, folder, "created");
        return toFolderVO(folder, Collections.emptyList());
    }

    /**
     * 更新文件夹。
     *
     * @param spaceId 空间标识
     * @param folderId 文件夹标识
     * @param request 文件夹保存请求
     * @return 文件夹信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileFolderVO updateFolder(Long spaceId, Long folderId, FileFolderSaveRequest request) {
        ensureFileManager(spaceId);
        FileFolder folder = findFolder(spaceId, folderId);
        Long parentId = normalizeFolderId(request.getParentId());
        if (Objects.equals(folder.getId(), parentId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件夹不能移动到自身下");
        }
        ensureFolderExists(spaceId, parentId);
        String name = normalizeFolderName(request.getName());
        ensureFolderNameUnique(spaceId, parentId, name, folder.getId());
        folder.setParentId(parentId);
        folder.setName(name);
        fileFolderMapper.updateById(folder);
        publishFolderRealtimeEvent(spaceId, folder, "updated");
        return toFolderVO(folder, Collections.emptyList());
    }

    /**
     * 删除文件夹。
     *
     * @param spaceId 空间标识
     * @param folderId 文件夹标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFolder(Long spaceId, Long folderId) {
        ensureFileManager(spaceId);
        findFolder(spaceId, folderId);
        Long childCount = fileFolderMapper.selectCount(Wrappers.<FileFolder>lambdaQuery()
                .eq(FileFolder::getSpaceId, spaceId)
                .eq(FileFolder::getParentId, folderId));
        Long fileCount = managedFileMapper.selectCount(Wrappers.<ManagedFile>lambdaQuery()
                .eq(ManagedFile::getSpaceId, spaceId)
                .eq(ManagedFile::getFolderId, folderId)
                .eq(ManagedFile::getTrashed, false));
        if (childCount > 0 || fileCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "文件夹非空，无法删除");
        }
        fileFolderMapper.deleteById(folderId);
        spaceRealtimeEventService.publish(spaceId, SpaceRealtimeEventType.FILE_TREE_CHANGED,
                Map.of("folderId", folderId, "action", "deleted"));
    }

    /**
     * 上传并登记文件。
     *
     * @param spaceId 空间标识
     * @param folderId 文件夹标识
     * @param file 上传文件
     * @return 文件信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ManagedFileVO upload(Long spaceId, Long folderId, MultipartFile file) {
        ensureFileManager(spaceId);
        return uploadManagedFile(spaceId, normalizeFolderId(folderId), file, "上传文件");
    }

    /**
     * 从编辑器上传文件并登记到文件管理。
     *
     * @param spaceId 空间标识
     * @param file 上传文件
     * @return 文件信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ManagedFileVO uploadFromEditor(Long spaceId, MultipartFile file) {
        ensureEditorUploader(spaceId);
        return uploadManagedFile(spaceId, 0L, file, "编辑器上传文件");
    }

    private ManagedFileVO uploadManagedFile(Long spaceId, Long folderId, MultipartFile file, String operationDetail) {
        redisUtil.limit(
                RedisKeyConstants.rateLimit("file-upload", spaceId + ":" + LoginUserUtil.currentUserId()),
                FILE_UPLOAD_RATE_LIMIT,
                FILE_RATE_LIMIT_WINDOW,
                "文件上传过于频繁，请稍后再试"
        );
        Long normalizedFolderId = normalizeFolderId(folderId);
        ensureFolderExists(spaceId, normalizedFolderId);
        validateMultipartFile(spaceId, file);
        AttachmentVO attachment = attachmentService.upload(spaceId, file);
        ManagedFile managedFile = new ManagedFile();
        managedFile.setAttachmentId(attachment.getId());
        managedFile.setSpaceId(spaceId);
        managedFile.setFolderId(normalizedFolderId);
        managedFile.setDisplayName(attachment.getFileName());
        managedFile.setCreatedBy(LoginUserUtil.currentUserId());
        managedFile.setTrashed(false);
        managedFileMapper.insert(managedFile);
        recordOperation(managedFile.getId(), spaceId, OPERATION_UPLOAD, operationDetail);
        publishFileProcess(managedFile, FileProcessOperation.UPLOADED);
        publishFileStatsRefresh(spaceId);
        publishManagedFileRealtimeEvent(managedFile, SpaceRealtimeEventType.FILE_CREATED, "created");
        return toVO(managedFile, true);
    }

    /**
     * 初始化文件分片上传。
     *
     * @param spaceId 空间标识
     * @param request 分片上传初始化请求
     * @return 分片上传会话
     */
    @Override
    public ManagedFileChunkUploadVO initChunkUpload(Long spaceId, ManagedFileChunkUploadInitRequest request) {
        ensureFileManager(spaceId);
        Long currentUserId = LoginUserUtil.currentUserId();
        redisUtil.limit(
                RedisKeyConstants.rateLimit("file-upload", spaceId + ":" + currentUserId),
                FILE_UPLOAD_RATE_LIMIT,
                FILE_RATE_LIMIT_WINDOW,
                "文件上传过于频繁，请稍后再试"
        );
        Long normalizedFolderId = normalizeFolderId(request.getFolderId());
        ensureFolderExists(spaceId, normalizedFolderId);
        String fileName = safeFileName(request.getFileName());
        validateUploadMetadata(spaceId, fileName, request.getFileSize(), request.getMimeType());
        Long chunkSize = safeChunkSize(spaceId);
        int totalChunks = Math.toIntExact((request.getFileSize() + chunkSize - 1) / chunkSize);
        String token = UUID.randomUUID().toString();
        Path sessionDirectory = chunkSessionDirectory(token);
        try {
            Files.createDirectories(sessionDirectory);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建分片上传会话失败");
        }
        ChunkUploadSession session = new ChunkUploadSession(
                spaceId,
                normalizedFolderId,
                currentUserId,
                fileName,
                request.getFileSize(),
                request.getMimeType(),
                chunkSize,
                totalChunks,
                sessionDirectory.toString()
        );
        redisUtil.set(
                RedisKeyConstants.fileUploadSession(token),
                encodeChunkUploadSession(session),
                Duration.ofMinutes(safeChunkUploadSessionMinutes())
        );
        return new ManagedFileChunkUploadVO(token, chunkSize, totalChunks, chunkUploadExpiresIn());
    }

    /**
     * 上传文件分片。
     *
     * @param spaceId 空间标识
     * @param uploadToken 上传会话令牌
     * @param chunkIndex 分片序号
     * @param chunk 分片文件
     */
    @Override
    public void uploadChunk(Long spaceId, String uploadToken, Integer chunkIndex, MultipartFile chunk) {
        ensureFileManager(spaceId);
        ChunkUploadSession session = findChunkUploadSession(uploadToken);
        ensureChunkSessionOwner(spaceId, session);
        if (chunkIndex == null || chunkIndex < 0 || chunkIndex >= session.totalChunks()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分片序号无效");
        }
        if (chunk == null || chunk.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分片文件不能为空");
        }
        if (chunk.getSize() > session.chunkSize()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分片大小超过限制");
        }
        Path targetPath = chunkPath(session, chunkIndex);
        try {
            Files.createDirectories(targetPath.getParent());
            try (InputStream inputStream = chunk.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存分片文件失败");
        }
    }

    /**
     * 完成文件分片上传。
     *
     * @param spaceId 空间标识
     * @param request 上传完成请求
     * @return 文件信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ManagedFileVO completeChunkUpload(Long spaceId, ManagedFileUploadCompleteRequest request) {
        ensureFileManager(spaceId);
        ChunkUploadSession session = findChunkUploadSession(request.getUploadToken());
        ensureChunkSessionOwner(spaceId, session);
        ensureFolderExists(spaceId, session.folderId());
        Path combinedPath = combineChunks(session);
        String storagePath = minioStorageService.buildManagedFileObjectName(spaceId, session.fileName());
        try (InputStream inputStream = Files.newInputStream(combinedPath)) {
            minioStorageService.uploadManagedObject(storagePath, inputStream, session.fileSize(), session.mimeType());
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "合并上传文件失败");
        }

        Attachment attachment = new Attachment();
        attachment.setFileName(session.fileName());
        attachment.setStoragePath(storagePath);
        attachment.setBucketName(minioStorageService.bucketName());
        attachment.setFileSize(session.fileSize());
        attachment.setMimeType(session.mimeType());
        attachment.setUploaderId(session.uploaderId());
        attachment.setSpaceId(spaceId);
        attachmentMapper.insert(attachment);

        ManagedFile managedFile = new ManagedFile();
        managedFile.setAttachmentId(attachment.getId());
        managedFile.setSpaceId(spaceId);
        managedFile.setFolderId(session.folderId());
        managedFile.setDisplayName(resolveDisplayName(request.getDisplayName(), session.fileName()));
        managedFile.setCreatedBy(session.uploaderId());
        managedFile.setTrashed(false);
        managedFileMapper.insert(managedFile);
        redisUtil.delete(RedisKeyConstants.fileUploadSession(request.getUploadToken()));
        cleanupChunkDirectory(Path.of(session.tempDirectory()));
        recordOperation(managedFile.getId(), spaceId, OPERATION_UPLOAD, "分片上传文件");
        publishFileProcess(managedFile, FileProcessOperation.UPLOADED);
        publishFileStatsRefresh(spaceId);
        publishManagedFileRealtimeEvent(managedFile, SpaceRealtimeEventType.FILE_CREATED, "created");
        return toVO(managedFile, true);
    }

    /**
     * 创建文件预签名上传地址。
     *
     * @param spaceId 空间标识
     * @param request 上传地址申请请求
     * @return 预签名上传地址
     */
    @Override
    public ManagedFileUploadUrlVO createUploadUrl(Long spaceId, ManagedFileUploadUrlRequest request) {
        ensureFileManager(spaceId);
        Long currentUserId = LoginUserUtil.currentUserId();
        redisUtil.limit(
                RedisKeyConstants.rateLimit("file-upload", spaceId + ":" + currentUserId),
                FILE_UPLOAD_RATE_LIMIT,
                FILE_RATE_LIMIT_WINDOW,
                "文件上传过于频繁，请稍后再试"
        );
        Long normalizedFolderId = normalizeFolderId(request.getFolderId());
        ensureFolderExists(spaceId, normalizedFolderId);
        String fileName = safeFileName(request.getFileName());
        validateUploadMetadata(spaceId, fileName, request.getFileSize(), request.getMimeType());
        String storagePath = minioStorageService.buildManagedFileObjectName(spaceId, fileName);
        String uploadUrl = minioStorageService.presignedUploadUrl(storagePath);
        String token = UUID.randomUUID().toString();
        UploadSession session = new UploadSession(
                spaceId,
                normalizedFolderId,
                currentUserId,
                storagePath,
                fileName,
                request.getFileSize(),
                request.getMimeType()
        );
        redisUtil.set(RedisKeyConstants.fileUploadSession(token), encodeUploadSession(session), UPLOAD_SESSION_TTL);
        return new ManagedFileUploadUrlVO(token, uploadUrl, UPLOAD_URL_METHOD, UPLOAD_URL_EXPIRES_IN);
    }

    /**
     * 完成文件直传登记。
     *
     * @param spaceId 空间标识
     * @param request 上传完成请求
     * @return 文件信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ManagedFileVO completeUpload(Long spaceId, ManagedFileUploadCompleteRequest request) {
        ensureFileManager(spaceId);
        String sessionValue = redisUtil.getAndDeleteString(RedisKeyConstants.fileUploadSession(request.getUploadToken()));
        if (!StringUtils.hasText(sessionValue)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传会话已过期，请重新上传");
        }
        UploadSession session = decodeUploadSession(sessionValue);
        if (!spaceId.equals(session.spaceId()) || !LoginUserUtil.currentUserId().equals(session.uploaderId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权完成该文件上传");
        }
        ensureFolderExists(spaceId, session.folderId());
        if (!minioStorageService.exists(session.storagePath())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件未上传完成，请稍后再试");
        }
        Attachment attachment = new Attachment();
        attachment.setFileName(session.fileName());
        attachment.setStoragePath(session.storagePath());
        attachment.setBucketName(minioStorageService.bucketName());
        attachment.setFileSize(session.fileSize());
        attachment.setMimeType(session.mimeType());
        attachment.setUploaderId(session.uploaderId());
        attachment.setSpaceId(spaceId);
        attachmentMapper.insert(attachment);

        ManagedFile managedFile = new ManagedFile();
        managedFile.setAttachmentId(attachment.getId());
        managedFile.setSpaceId(spaceId);
        managedFile.setFolderId(session.folderId());
        managedFile.setDisplayName(resolveDisplayName(request.getDisplayName(), session.fileName()));
        managedFile.setCreatedBy(session.uploaderId());
        managedFile.setTrashed(false);
        managedFileMapper.insert(managedFile);
        recordOperation(managedFile.getId(), spaceId, OPERATION_UPLOAD, "直传文件");
        publishFileProcess(managedFile, FileProcessOperation.UPLOADED);
        publishFileStatsRefresh(spaceId);
        publishManagedFileRealtimeEvent(managedFile, SpaceRealtimeEventType.FILE_CREATED, "created");
        return toVO(managedFile, true);
    }

    /**
     * 更新文件名称或所在文件夹。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @param request 文件更新请求
     * @return 文件信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ManagedFileVO updateFile(Long spaceId, Long fileId, ManagedFileUpdateRequest request) {
        ensureFileManager(spaceId);
        ManagedFile managedFile = findManagedFile(spaceId, fileId);
        if (Boolean.TRUE.equals(managedFile.getTrashed())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "回收站中的文件不能更新");
        }
        if (request.getFolderId() != null) {
            Long folderId = normalizeFolderId(request.getFolderId());
            ensureFolderExists(spaceId, folderId);
            managedFile.setFolderId(folderId);
        }
        if (StringUtils.hasText(request.getDisplayName())) {
            managedFile.setDisplayName(request.getDisplayName().trim());
        }
        managedFileMapper.updateById(managedFile);
        recordOperation(fileId, spaceId, OPERATION_UPDATE, "更新文件信息");
        publishManagedFileRealtimeEvent(managedFile, SpaceRealtimeEventType.FILE_UPDATED, "updated");
        return toVO(managedFile, true);
    }

    /**
     * 生成文件下载地址。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 文件信息
     */
    @Override
    public ManagedFileVO download(Long spaceId, Long fileId) {
        ensureFileManager(spaceId);
        redisUtil.limit(
                RedisKeyConstants.rateLimit("file-download", spaceId + ":" + LoginUserUtil.currentUserId()),
                FILE_DOWNLOAD_RATE_LIMIT,
                FILE_RATE_LIMIT_WINDOW,
                "文件下载过于频繁，请稍后再试"
        );
        ManagedFile managedFile = findManagedFile(spaceId, fileId);
        if (Boolean.TRUE.equals(managedFile.getTrashed())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "回收站中的文件不能下载");
        }
        return toVO(managedFile, true);
    }

    /**
     * 生成文件预览地址。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 文件信息
     */
    @Override
    public ManagedFileVO preview(Long spaceId, Long fileId) {
        return download(spaceId, fileId);
    }

    /**
     * 查询文件预览资源元信息。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 文件预览资源信息
     */
    @Override
    public FilePreviewResourceVO previewResource(Long spaceId, Long fileId) {
        ensureFileManager(spaceId);
        ManagedFile managedFile = findManagedFile(spaceId, fileId);
        Attachment attachment = findPreviewAttachment(managedFile);
        return new FilePreviewResourceVO(
                resolveDisplayName(managedFile.getDisplayName(), attachment.getFileName()),
                attachment.getMimeType(),
                attachment.getFileSize()
        );
    }

    /**
     * 查询文件文本预览内容。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 文件文本预览内容
     */
    @Override
    public FilePreviewTextVO previewText(Long spaceId, Long fileId) {
        ensureFileManager(spaceId);
        ManagedFile managedFile = findManagedFile(spaceId, fileId);
        Attachment attachment = findPreviewAttachment(managedFile);
        return new FilePreviewTextVO(
                resolveDisplayName(managedFile.getDisplayName(), attachment.getFileName()),
                attachment.getMimeType(),
                fileTextExtractionService.extract(attachment)
        );
    }

    /**
     * 查询文件 HTML 预览内容。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 文件 HTML 预览内容
     */
    @Override
    public FilePreviewHtmlVO previewHtml(Long spaceId, Long fileId) {
        ensureFileManager(spaceId);
        ManagedFile managedFile = findManagedFile(spaceId, fileId);
        Attachment attachment = findPreviewAttachment(managedFile);
        return new FilePreviewHtmlVO(
                resolveDisplayName(managedFile.getDisplayName(), attachment.getFileName()),
                attachment.getMimeType(),
                fileTextExtractionService.extractHtml(attachment)
        );
    }

    /**
     * 写出文件预览内容。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @param outputStream 输出流
     * @throws IOException 写出失败
     */
    @Override
    public void writePreviewContent(Long spaceId, Long fileId, OutputStream outputStream) throws IOException {
        ensureFileManager(spaceId);
        ManagedFile managedFile = findManagedFile(spaceId, fileId);
        Attachment attachment = findPreviewAttachment(managedFile);
        try (InputStream inputStream = minioStorageService.openObject(attachment.getStoragePath())) {
            StreamUtils.copy(inputStream, outputStream);
        }
    }

    /**
     * 将文件放入回收站。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void trash(Long spaceId, Long fileId) {
        ensureFileManager(spaceId);
        ManagedFile managedFile = findManagedFile(spaceId, fileId);
        managedFile.setTrashed(true);
        managedFile.setDeletedAt(LocalDateTime.now());
        managedFile.setDeletedBy(LoginUserUtil.currentUserId());
        managedFileMapper.updateById(managedFile);
        recordOperation(fileId, spaceId, OPERATION_TRASH, "移入回收站");
        publishFileProcess(managedFile, FileProcessOperation.TRASHED);
        publishFileStatsRefresh(spaceId);
        publishManagedFileRealtimeEvent(managedFile, SpaceRealtimeEventType.FILE_DELETED, "trashed");
    }

    /**
     * 从回收站恢复文件。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 文件信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ManagedFileVO restore(Long spaceId, Long fileId) {
        ensureFileManager(spaceId);
        ManagedFile managedFile = findManagedFile(spaceId, fileId);
        managedFile.setTrashed(false);
        managedFile.setDeletedAt(null);
        managedFile.setDeletedBy(null);
        managedFileMapper.updateById(managedFile);
        recordOperation(fileId, spaceId, OPERATION_RESTORE, "从回收站恢复");
        publishFileProcess(managedFile, FileProcessOperation.RESTORED);
        publishFileStatsRefresh(spaceId);
        publishManagedFileRealtimeEvent(managedFile, SpaceRealtimeEventType.FILE_UPDATED, "restored");
        return toVO(managedFile, true);
    }

    /**
     * 物理删除文件。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void physicalDelete(Long spaceId, Long fileId, boolean force) {
        ensureFileManager(spaceId);
        ManagedFile managedFile = findManagedFile(spaceId, fileId);
        Attachment attachment = attachmentMapper.selectById(managedFile.getAttachmentId());
        Long referenceCount = businessAttachmentMapper.selectCount(Wrappers.<BusinessAttachment>lambdaQuery()
                .eq(BusinessAttachment::getAttachmentId, managedFile.getAttachmentId()));
        if (referenceCount > 0 && !force) {
            throw new BusinessException(ErrorCode.CONFLICT, "文件仍被业务对象引用，请确认后再强制删除");
        }
        businessAttachmentMapper.physicalDeleteByAttachmentId(managedFile.getAttachmentId());
        managedFileMapper.deleteById(managedFile.getId());
        if (attachment != null) {
            attachmentMapper.deleteById(attachment.getId());
            minioStorageService.delete(attachment.getStoragePath());
        }
        recordOperation(fileId, spaceId, OPERATION_PHYSICAL_DELETE, force ? "强制物理删除文件" : "物理删除文件");
        applicationEventPublisher.publishEvent(new FileProcessRequestedEvent(
                spaceId,
                fileId,
                managedFile.getAttachmentId(),
                FileProcessOperation.PHYSICAL_DELETED
        ));
        publishFileStatsRefresh(spaceId);
        spaceRealtimeEventService.publish(spaceId, SpaceRealtimeEventType.FILE_DELETED,
                Map.of("fileId", fileId, "attachmentId", managedFile.getAttachmentId(),
                        "folderId", normalizeFolderId(managedFile.getFolderId()), "action", "physicalDeleted"));
    }

    /**
     * 查询文件引用关系。
     *
     * @param spaceId 空间标识
     * @param fileId 文件标识
     * @return 引用列表
     */
    @Override
    public List<FileReferenceVO> references(Long spaceId, Long fileId) {
        ensureFileManager(spaceId);
        ManagedFile managedFile = findManagedFile(spaceId, fileId);
        return businessAttachmentMapper.selectList(Wrappers.<BusinessAttachment>lambdaQuery()
                        .eq(BusinessAttachment::getAttachmentId, managedFile.getAttachmentId()))
                .stream()
                .map(reference -> new FileReferenceVO(
                        reference.getId(),
                        reference.getAttachmentId(),
                        reference.getBusinessType(),
                        reference.getBusinessId(),
                        reference.getReferenceKey(),
                        reference.getGmtCreate()
                ))
                .toList();
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
    @Override
    public PageResponse<FileOperationLogVO> operationLogs(Long spaceId, Long fileId, long pageNum, long pageSize) {
        ensureFileManager(spaceId);
        findManagedFile(spaceId, fileId);
        Page<FileOperationLog> page = new Page<>(safeLogPageNum(pageNum), safeLogPageSize(pageSize));
        Page<FileOperationLog> result = fileOperationLogMapper.selectPage(page, Wrappers.<FileOperationLog>lambdaQuery()
                .eq(FileOperationLog::getSpaceId, spaceId)
                .eq(FileOperationLog::getFileId, fileId)
                .orderByDesc(FileOperationLog::getGmtCreate));
        List<FileOperationLogVO> list = result.getRecords().stream()
                .map(this::toOperationLogVO)
                .toList();
        return PageResponse.of(result, list);
    }

    /**
     * 查询文件统计。
     *
     * @param spaceId 空间标识
     * @return 文件统计
     */
    @Override
    public FileStatsVO stats(Long spaceId) {
        ensureFileManager(spaceId);
        FileStatsVO cachedStats = fileStatsCacheService.getCached(spaceId);
        return cachedStats == null ? fileStatsCacheService.refresh(spaceId) : cachedStats;
    }

    private Optional<List<Long>> searchFileIds(Long spaceId, ManagedFileQuery query) {
        if (!StringUtils.hasText(query.getKeyword())) {
            return Optional.empty();
        }
        return fileSearchService.searchFileIds(spaceId, query.getKeyword(), FILE_SEARCH_LIMIT);
    }

    private List<ManagedFile> selectManagedFiles(Long spaceId, ManagedFileQuery query,
                                                 Optional<List<Long>> searchedFileIds) {
        if (searchedFileIds.isPresent()) {
            return selectManagedFilesBySearchIds(spaceId, query, searchedFileIds.get());
        }
        return managedFileMapper.selectList(Wrappers.<ManagedFile>lambdaQuery()
                .eq(ManagedFile::getSpaceId, spaceId)
                .eq(query.getTrashed() != null, ManagedFile::getTrashed, query.getTrashed())
                .eq(query.getFolderId() != null && !hasGlobalFileQuery(query),
                        ManagedFile::getFolderId,
                        normalizeFolderId(query.getFolderId()))
                .orderByDesc(ManagedFile::getGmtModified));
    }

    private List<ManagedFile> selectManagedFilesBySearchIds(Long spaceId, ManagedFileQuery query, List<Long> fileIds) {
        if (fileIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<ManagedFile> files = managedFileMapper.selectList(Wrappers.<ManagedFile>lambdaQuery()
                .eq(ManagedFile::getSpaceId, spaceId)
                .eq(query.getTrashed() != null, ManagedFile::getTrashed, query.getTrashed())
                .eq(query.getFolderId() != null && !hasGlobalFileQuery(query),
                        ManagedFile::getFolderId,
                        normalizeFolderId(query.getFolderId()))
                .in(ManagedFile::getId, fileIds));
        return sortBySearchOrder(files, fileIds);
    }

    private boolean hasGlobalFileQuery(ManagedFileQuery query) {
        return StringUtils.hasText(query.getKeyword()) || StringUtils.hasText(query.getMimeType());
    }

    private List<ManagedFile> sortBySearchOrder(List<ManagedFile> files, List<Long> fileIds) {
        Map<Long, Integer> searchOrder = new HashMap<>();
        for (Long fileId : fileIds) {
            searchOrder.put(fileId, searchOrder.size());
        }
        return files.stream()
                .sorted(Comparator.comparing(file -> searchOrder.getOrDefault(file.getId(), Integer.MAX_VALUE)))
                .toList();
    }

    private boolean matchesMetadataQuery(ManagedFileVO file, ManagedFileQuery query) {
        if (query.getUploaderId() != null && !query.getUploaderId().equals(file.getUploaderId())) {
            return false;
        }
        if (StringUtils.hasText(query.getMimeType())
                && (file.getMimeType() == null || !file.getMimeType().contains(query.getMimeType()))) {
            return false;
        }
        return true;
    }

    private boolean matchesKeyword(ManagedFileVO file, ManagedFileQuery query) {
        if (!StringUtils.hasText(query.getKeyword())) {
            return true;
        }
        String keyword = query.getKeyword().trim().toLowerCase();
        String displayName = file.getDisplayName() == null ? "" : file.getDisplayName().toLowerCase();
        String fileName = file.getFileName() == null ? "" : file.getFileName().toLowerCase();
        return displayName.contains(keyword) || fileName.contains(keyword);
    }

    private List<FileFolderVO> buildFolderTree(List<FileFolder> folders) {
        Map<Long, List<FileFolder>> childrenMap = folders.stream()
                .filter(folder -> folder.getParentId() != null && folder.getParentId() > 0)
                .collect(Collectors.groupingBy(FileFolder::getParentId));
        return folders.stream()
                .filter(folder -> folder.getParentId() == null || folder.getParentId() == 0)
                .sorted(Comparator.comparing(FileFolder::getSortOrder).thenComparing(FileFolder::getName))
                .map(folder -> toFolderVO(folder, buildFolderChildren(folder, childrenMap)))
                .toList();
    }

    private List<FileFolderVO> buildFolderChildren(FileFolder folder, Map<Long, List<FileFolder>> childrenMap) {
        List<FileFolder> children = childrenMap.getOrDefault(folder.getId(), Collections.emptyList());
        if (children.isEmpty()) {
            return Collections.emptyList();
        }
        return children.stream()
                .sorted(Comparator.comparing(FileFolder::getSortOrder).thenComparing(FileFolder::getName))
                .map(child -> toFolderVO(child, buildFolderChildren(child, childrenMap)))
                .toList();
    }

    private FileFolderVO toFolderVO(FileFolder folder, List<FileFolderVO> children) {
        return new FileFolderVO(
                folder.getId(),
                folder.getSpaceId(),
                folder.getParentId(),
                folder.getName(),
                folder.getSortOrder(),
                folder.getCreatedBy(),
                folder.getGmtCreate(),
                new ArrayList<>(children)
        );
    }

    private ManagedFileVO toVO(ManagedFile managedFile, boolean includeUrl) {
        Attachment attachment = attachmentMapper.selectById(managedFile.getAttachmentId());
        String downloadUrl = includeUrl && attachment != null
                ? minioStorageService.presignedDownloadUrl(attachment.getStoragePath())
                : null;
        return new ManagedFileVO(
                managedFile.getId(),
                managedFile.getAttachmentId(),
                managedFile.getSpaceId(),
                managedFile.getFolderId(),
                managedFile.getDisplayName(),
                attachment == null ? null : attachment.getFileName(),
                attachment == null ? 0L : attachment.getFileSize(),
                attachment == null ? null : attachment.getMimeType(),
                attachment == null ? null : attachment.getUploaderId(),
                managedFile.getCreatedBy(),
                managedFile.getTrashed(),
                managedFile.getDeletedAt(),
                downloadUrl,
                managedFile.getGmtCreate()
        );
    }

    private FileOperationLogVO toOperationLogVO(FileOperationLog operationLog) {
        return new FileOperationLogVO(
                operationLog.getId(),
                operationLog.getFileId(),
                operationLog.getSpaceId(),
                operationLog.getOperatorId(),
                operationLog.getOperationType(),
                operationLog.getDetail(),
                operationLog.getGmtCreate()
        );
    }

    private Attachment findPreviewAttachment(ManagedFile managedFile) {
        if (Boolean.TRUE.equals(managedFile.getTrashed())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "回收站中的文件不能预览");
        }
        Attachment attachment = attachmentMapper.selectById(managedFile.getAttachmentId());
        if (attachment == null) {
            throw new ResourceNotFoundException("文件对象不存在");
        }
        return attachment;
    }

    private ManagedFile findManagedFile(Long spaceId, Long fileId) {
        ManagedFile managedFile = managedFileMapper.selectOne(Wrappers.<ManagedFile>lambdaQuery()
                .eq(ManagedFile::getId, fileId)
                .eq(ManagedFile::getSpaceId, spaceId));
        if (managedFile == null) {
            throw new ResourceNotFoundException("文件不存在");
        }
        return managedFile;
    }

    private FileFolder findFolder(Long spaceId, Long folderId) {
        FileFolder folder = fileFolderMapper.selectOne(Wrappers.<FileFolder>lambdaQuery()
                .eq(FileFolder::getId, folderId)
                .eq(FileFolder::getSpaceId, spaceId));
        if (folder == null) {
            throw new ResourceNotFoundException("文件夹不存在");
        }
        return folder;
    }

    private void ensureFolderExists(Long spaceId, Long folderId) {
        if (folderId == null) {
            return;
        }
        if (folderId == 0) {
            return;
        }
        findFolder(spaceId, folderId);
    }

    private void ensureFolderNameUnique(Long spaceId, Long parentId, String name, Long excludeId) {
        Long count = fileFolderMapper.selectCount(Wrappers.<FileFolder>lambdaQuery()
                .eq(FileFolder::getSpaceId, spaceId)
                .eq(FileFolder::getName, name)
                .eq(FileFolder::getParentId, normalizeFolderId(parentId))
                .ne(excludeId != null, FileFolder::getId, excludeId));
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "同级目录下已存在同名文件夹");
        }
    }

    private Long normalizeFolderId(Long folderId) {
        return folderId == null || folderId <= 0 ? 0L : folderId;
    }

    private String normalizeFolderName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件夹名称不能为空");
        }
        return name.trim();
    }

    private void ensureFileManager(Long spaceId) {
        Long currentUserId = LoginUserUtil.currentUserId();
        Space space = spaceMapper.selectById(spaceId);
        if (space == null) {
            throw new ResourceNotFoundException("空间不存在");
        }
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
    }

    private void ensureUploadConfigManager(Long spaceId) {
        Long currentUserId = LoginUserUtil.currentUserId();
        Space space = spaceMapper.selectById(spaceId);
        if (space == null) {
            throw new ResourceNotFoundException("空间不存在");
        }
        if (SpaceType.TEAM.equals(space.getType())) {
            permissionValidator.ensureSpaceAdminOrOwner(spaceId, currentUserId);
            return;
        }
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
    }

    private void ensureEditorUploader(Long spaceId) {
        Long currentUserId = LoginUserUtil.currentUserId();
        Space space = spaceMapper.selectById(spaceId);
        if (space == null) {
            throw new ResourceNotFoundException("空间不存在");
        }
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
    }

    private long safeLogPageNum(long pageNum) {
        return pageNum <= 0 ? 1L : pageNum;
    }

    private long safeLogPageSize(long pageSize) {
        if (pageSize <= 0) {
            return DEFAULT_LOG_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_LOG_PAGE_SIZE);
    }

    private FileUploadConfigVO defaultUploadConfig() {
        return normalizeUploadConfig(new FileUploadConfigVO(
                minioStorageService.maxFileSize(),
                fileManagementProperties.getMultipartThresholdSize(),
                fileManagementProperties.getChunkSize(),
                minioStorageService.allowedMimeTypes()
        ));
    }

    private FileUploadConfigVO effectiveUploadConfig(Long spaceId) {
        FileUploadConfigVO cachedConfig = redisUtil.getObject(
                RedisKeyConstants.spaceFileUploadConfig(spaceId),
                FileUploadConfigVO.class
        );
        return cachedConfig == null ? defaultUploadConfig() : normalizeUploadConfig(cachedConfig);
    }

    private FileUploadConfigVO normalizeUploadConfig(FileUploadConfigVO config) {
        Long globalMaxFileSize = minioStorageService.maxFileSize();
        Long maxFileSize = config.getMaxFileSize() == null || config.getMaxFileSize() <= 0
                ? globalMaxFileSize
                : Math.min(config.getMaxFileSize(), globalMaxFileSize);
        Long multipartThresholdSize = config.getMultipartThresholdSize() == null
                || config.getMultipartThresholdSize() <= 0
                ? 52428800L
                : config.getMultipartThresholdSize();
        Long chunkSize = config.getChunkSize() == null || config.getChunkSize() <= 0
                ? 5242880L
                : config.getChunkSize();
        Long normalizedThresholdSize = Math.min(multipartThresholdSize, maxFileSize);
        Long normalizedChunkSize = Math.min(chunkSize, normalizedThresholdSize);
        return new FileUploadConfigVO(
                maxFileSize,
                Math.max(1L, normalizedThresholdSize),
                Math.max(1L, normalizedChunkSize),
                safeAllowedMimeTypes(config.getAllowedMimeTypes())
        );
    }

    private List<String> safeAllowedMimeTypes(List<String> allowedMimeTypes) {
        List<String> source = allowedMimeTypes == null || allowedMimeTypes.isEmpty()
                ? minioStorageService.allowedMimeTypes()
                : allowedMimeTypes;
        return source == null ? Collections.emptyList() : source.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private void validateMultipartFile(Long spaceId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        validateUploadMetadata(spaceId, file.getOriginalFilename(), file.getSize(), file.getContentType());
    }

    private void validateUploadMetadata(Long spaceId, String fileName, Long fileSize, String contentType) {
        FileUploadConfigVO config = effectiveUploadConfig(spaceId);
        minioStorageService.validateUploadMetadata(fileName, fileSize, contentType);
        if (fileSize > config.getMaxFileSize()) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
        if (!isAllowedContentType(contentType, config.getAllowedMimeTypes())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件类型不允许");
        }
    }

    private boolean isAllowedContentType(String contentType, List<String> allowedMimeTypes) {
        if (allowedMimeTypes == null || allowedMimeTypes.isEmpty()) {
            return true;
        }
        if (!StringUtils.hasText(contentType)) {
            return false;
        }
        String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
        return allowedMimeTypes.stream().anyMatch(allowedMimeType -> {
            String normalizedAllowedMimeType = allowedMimeType.toLowerCase(Locale.ROOT);
            if (normalizedAllowedMimeType.endsWith("/*")) {
                String prefix = normalizedAllowedMimeType.substring(0, normalizedAllowedMimeType.length() - 1);
                return normalizedContentType.startsWith(prefix);
            }
            return normalizedAllowedMimeType.equals(normalizedContentType);
        });
    }

    private Long safeChunkSize(Long spaceId) {
        return effectiveUploadConfig(spaceId).getChunkSize();
    }

    private Integer chunkUploadExpiresIn() {
        return Math.toIntExact(safeChunkUploadSessionMinutes() * 60L);
    }

    private Integer safeChunkUploadSessionMinutes() {
        Integer sessionMinutes = fileManagementProperties.getChunkUploadSessionMinutes();
        return sessionMinutes == null || sessionMinutes <= 0 ? 30 : sessionMinutes;
    }

    private Path chunkSessionDirectory(String uploadToken) {
        String configuredTempDir = fileManagementProperties.getChunkTempDir();
        Path rootDirectory = StringUtils.hasText(configuredTempDir)
                ? Path.of(configuredTempDir)
                : Path.of(System.getProperty("java.io.tmpdir"), "notask-flow", "file-chunks");
        return rootDirectory.resolve(uploadToken).normalize();
    }

    private ChunkUploadSession findChunkUploadSession(String uploadToken) {
        String sessionValue = redisUtil.getString(RedisKeyConstants.fileUploadSession(uploadToken));
        if (!StringUtils.hasText(sessionValue)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传会话已过期，请重新上传");
        }
        return decodeChunkUploadSession(sessionValue);
    }

    private void ensureChunkSessionOwner(Long spaceId, ChunkUploadSession session) {
        if (!spaceId.equals(session.spaceId()) || !LoginUserUtil.currentUserId().equals(session.uploaderId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该上传会话");
        }
    }

    private Path chunkPath(ChunkUploadSession session, Integer chunkIndex) {
        return Path.of(session.tempDirectory()).resolve(CHUNK_FILE_PREFIX + chunkIndex).normalize();
    }

    private Path combineChunks(ChunkUploadSession session) {
        Path combinedPath = Path.of(session.tempDirectory()).resolve(COMBINED_FILE_NAME).normalize();
        try (OutputStream outputStream = Files.newOutputStream(combinedPath)) {
            for (int index = 0; index < session.totalChunks(); index++) {
                Path chunkPath = chunkPath(session, index);
                if (!Files.exists(chunkPath)) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "文件分片未上传完成");
                }
                try (InputStream inputStream = Files.newInputStream(chunkPath)) {
                    StreamUtils.copy(inputStream, outputStream);
                }
            }
            return combinedPath;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "合并分片文件失败");
        }
    }

    private void cleanupChunkDirectory(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> pathStream = Files.walk(directory)) {
            pathStream
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            log.warn("删除分片临时文件失败，path={}", path, exception);
                        }
                    });
        } catch (IOException exception) {
            log.warn("清理分片临时目录失败，directory={}", directory, exception);
        }
    }

    private String encodeUploadSession(UploadSession session) {
        return String.join(UPLOAD_SESSION_DELIMITER,
                String.valueOf(session.spaceId()),
                String.valueOf(session.folderId()),
                String.valueOf(session.uploaderId()),
                encodeText(session.storagePath()),
                encodeText(session.fileName()),
                String.valueOf(session.fileSize()),
                encodeText(session.mimeType()));
    }

    private UploadSession decodeUploadSession(String value) {
        String[] parts = value.split(UPLOAD_SESSION_DELIMITER, -1);
        if (parts.length != 7) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传会话数据无效");
        }
        try {
            return new UploadSession(
                    Long.valueOf(parts[0]),
                    Long.valueOf(parts[1]),
                    Long.valueOf(parts[2]),
                    decodeText(parts[3]),
                    decodeText(parts[4]),
                    Long.valueOf(parts[5]),
                    decodeText(parts[6])
            );
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传会话数据无效");
        }
    }

    private String encodeChunkUploadSession(ChunkUploadSession session) {
        return String.join(UPLOAD_SESSION_DELIMITER,
                String.valueOf(session.spaceId()),
                String.valueOf(session.folderId()),
                String.valueOf(session.uploaderId()),
                encodeText(session.fileName()),
                String.valueOf(session.fileSize()),
                encodeText(session.mimeType()),
                String.valueOf(session.chunkSize()),
                String.valueOf(session.totalChunks()),
                encodeText(session.tempDirectory()));
    }

    private ChunkUploadSession decodeChunkUploadSession(String value) {
        String[] parts = value.split(UPLOAD_SESSION_DELIMITER, -1);
        if (parts.length != 9) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传会话数据无效");
        }
        try {
            return new ChunkUploadSession(
                    Long.valueOf(parts[0]),
                    Long.valueOf(parts[1]),
                    Long.valueOf(parts[2]),
                    decodeText(parts[3]),
                    Long.valueOf(parts[4]),
                    decodeText(parts[5]),
                    Long.valueOf(parts[6]),
                    Integer.valueOf(parts[7]),
                    decodeText(parts[8])
            );
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传会话数据无效");
        }
    }

    private String encodeText(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
    }

    private String decodeText(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private String resolveDisplayName(String displayName, String fallback) {
        return StringUtils.hasText(displayName) ? displayName.trim() : fallback;
    }

    private String safeFileName(String fileName) {
        return StringUtils.cleanPath(fileName);
    }

    private void publishFileProcess(ManagedFile managedFile, FileProcessOperation operation) {
        applicationEventPublisher.publishEvent(new FileProcessRequestedEvent(
                managedFile.getSpaceId(),
                managedFile.getId(),
                managedFile.getAttachmentId(),
                operation
        ));
    }

    private void publishFileStatsRefresh(Long spaceId) {
        applicationEventPublisher.publishEvent(new StatsRefreshRequestedEvent(spaceId, StatsRefreshScope.FILE));
    }

    private void publishFolderRealtimeEvent(Long spaceId, FileFolder folder, String action) {
        spaceRealtimeEventService.publish(spaceId, SpaceRealtimeEventType.FILE_TREE_CHANGED,
                Map.of("folderId", folder.getId(), "parentId", normalizeFolderId(folder.getParentId()),
                        "name", folder.getName(), "action", action));
    }

    private void publishManagedFileRealtimeEvent(ManagedFile managedFile,
                                                 SpaceRealtimeEventType type,
                                                 String action) {
        spaceRealtimeEventService.publish(managedFile.getSpaceId(), type,
                Map.of("fileId", managedFile.getId(), "attachmentId", managedFile.getAttachmentId(),
                        "folderId", normalizeFolderId(managedFile.getFolderId()), "action", action));
    }

    private void recordOperation(Long fileId, Long spaceId, String operationType, String detail) {
        FileOperationLog operationLog = new FileOperationLog();
        operationLog.setFileId(fileId);
        operationLog.setSpaceId(spaceId);
        operationLog.setOperatorId(LoginUserUtil.currentUserId());
        operationLog.setOperationType(operationType);
        operationLog.setDetail(detail);
        fileOperationLogMapper.insert(operationLog);
        log.info("文件操作已记录，fileId={}, spaceId={}, operationType={}", fileId, spaceId, operationType);
    }

    /**
     * 文件直传会话。
     *
     * @param spaceId 空间标识
     * @param folderId 文件夹标识
     * @param uploaderId 上传用户标识
     * @param storagePath 对象存储路径
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @param mimeType 文件类型
     */
    private record UploadSession(Long spaceId, Long folderId, Long uploaderId, String storagePath, String fileName,
                                 Long fileSize, String mimeType) {
    }

    /**
     * 文件分片上传会话。
     *
     * @param spaceId 空间标识
     * @param folderId 文件夹标识
     * @param uploaderId 上传用户标识
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @param mimeType 文件类型
     * @param chunkSize 分片大小
     * @param totalChunks 分片总数
     * @param tempDirectory 临时目录
     */
    private record ChunkUploadSession(Long spaceId, Long folderId, Long uploaderId, String fileName, Long fileSize,
                                      String mimeType, Long chunkSize, Integer totalChunks, String tempDirectory) {
    }
}
