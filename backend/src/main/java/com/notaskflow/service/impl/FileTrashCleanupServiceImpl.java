package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notaskflow.common.constant.RedisKeyConstants;
import com.notaskflow.common.enums.FileProcessOperation;
import com.notaskflow.common.enums.StatsRefreshScope;
import com.notaskflow.config.FileManagementProperties;
import com.notaskflow.domain.entity.Attachment;
import com.notaskflow.domain.entity.BusinessAttachment;
import com.notaskflow.domain.entity.FileOperationLog;
import com.notaskflow.domain.entity.ManagedFile;
import com.notaskflow.event.FileProcessRequestedEvent;
import com.notaskflow.event.StatsRefreshRequestedEvent;
import com.notaskflow.mapper.AttachmentMapper;
import com.notaskflow.mapper.BusinessAttachmentMapper;
import com.notaskflow.mapper.FileOperationLogMapper;
import com.notaskflow.mapper.ManagedFileMapper;
import com.notaskflow.service.FileTrashCleanupService;
import com.notaskflow.storage.MinioStorageService;
import com.notaskflow.utils.RedisUtil;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文件回收站清理服务实现。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileTrashCleanupServiceImpl implements FileTrashCleanupService {

    private static final Duration CLEANUP_LOCK_TTL = Duration.ofMinutes(10);

    private static final String OPERATION_AUTO_PHYSICAL_DELETE = "AUTO_PHYSICAL_DELETE";

    private final ManagedFileMapper managedFileMapper;

    private final AttachmentMapper attachmentMapper;

    private final BusinessAttachmentMapper businessAttachmentMapper;

    private final FileOperationLogMapper fileOperationLogMapper;

    private final MinioStorageService minioStorageService;

    private final FileManagementProperties fileManagementProperties;

    private final RedisUtil redisUtil;

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 清理超过保留期且未被引用的回收站文件。
     *
     * @return 清理的文件数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpiredTrashFiles() {
        if (!Boolean.TRUE.equals(fileManagementProperties.getCleanupEnabled())) {
            return 0;
        }
        String lockOwner = UUID.randomUUID().toString();
        boolean locked = redisUtil.tryLock(RedisKeyConstants.FILE_TRASH_CLEANUP_LOCK, lockOwner, CLEANUP_LOCK_TTL);
        if (!locked) {
            log.info("文件回收站清理任务已在其他节点执行，本次跳过");
            return 0;
        }
        try {
            List<ManagedFile> files = queryExpiredTrashFiles();
            if (files.isEmpty()) {
                return 0;
            }
            Set<Long> affectedSpaceIds = new HashSet<>();
            int cleaned = 0;
            for (ManagedFile file : files) {
                if (cleanupFile(file)) {
                    affectedSpaceIds.add(file.getSpaceId());
                    cleaned++;
                }
            }
            affectedSpaceIds.forEach(this::publishFileStatsRefresh);
            log.info("文件回收站清理完成，cleaned={}", cleaned);
            return cleaned;
        } finally {
            redisUtil.unlock(RedisKeyConstants.FILE_TRASH_CLEANUP_LOCK, lockOwner);
        }
    }

    private List<ManagedFile> queryExpiredTrashFiles() {
        int retentionDays = Math.max(1, fileManagementProperties.getTrashRetentionDays());
        int batchSize = Math.max(1, fileManagementProperties.getCleanupBatchSize());
        LocalDateTime expiredBefore = LocalDateTime.now().minusDays(retentionDays);
        Page<ManagedFile> page = new Page<>(1, batchSize);
        return managedFileMapper.selectPage(page, Wrappers.<ManagedFile>lambdaQuery()
                        .eq(ManagedFile::getTrashed, true)
                        .le(ManagedFile::getDeletedAt, expiredBefore)
                        .orderByAsc(ManagedFile::getDeletedAt))
                .getRecords();
    }

    private boolean cleanupFile(ManagedFile managedFile) {
        if (managedFile.getAttachmentId() == null) {
            managedFileMapper.deleteById(managedFile.getId());
            return true;
        }
        Long referenceCount = businessAttachmentMapper.selectCount(Wrappers.<BusinessAttachment>lambdaQuery()
                .eq(BusinessAttachment::getAttachmentId, managedFile.getAttachmentId()));
        if (referenceCount > 0) {
            log.info("文件仍被业务对象引用，跳过自动清理，fileId={}, attachmentId={}",
                    managedFile.getId(), managedFile.getAttachmentId());
            return false;
        }
        Attachment attachment = attachmentMapper.selectById(managedFile.getAttachmentId());
        managedFileMapper.deleteById(managedFile.getId());
        if (attachment != null) {
            attachmentMapper.deleteById(attachment.getId());
            minioStorageService.delete(attachment.getStoragePath());
        }
        recordOperation(managedFile);
        publishFileProcess(managedFile);
        return true;
    }

    private void recordOperation(ManagedFile managedFile) {
        FileOperationLog operationLog = new FileOperationLog();
        operationLog.setFileId(managedFile.getId());
        operationLog.setSpaceId(managedFile.getSpaceId());
        operationLog.setOperatorId(resolveOperatorId(managedFile));
        operationLog.setOperationType(OPERATION_AUTO_PHYSICAL_DELETE);
        operationLog.setDetail("回收站过期自动物理删除文件");
        fileOperationLogMapper.insert(operationLog);
    }

    private Long resolveOperatorId(ManagedFile managedFile) {
        if (managedFile.getDeletedBy() != null) {
            return managedFile.getDeletedBy();
        }
        if (managedFile.getCreatedBy() != null) {
            return managedFile.getCreatedBy();
        }
        return 0L;
    }

    private void publishFileProcess(ManagedFile managedFile) {
        applicationEventPublisher.publishEvent(new FileProcessRequestedEvent(
                managedFile.getSpaceId(),
                managedFile.getId(),
                managedFile.getAttachmentId(),
                FileProcessOperation.PHYSICAL_DELETED
        ));
    }

    private void publishFileStatsRefresh(Long spaceId) {
        applicationEventPublisher.publishEvent(new StatsRefreshRequestedEvent(spaceId, StatsRefreshScope.FILE));
    }
}
