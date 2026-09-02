package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.notaskflow.common.constant.RedisKeyConstants;
import com.notaskflow.domain.entity.Attachment;
import com.notaskflow.domain.entity.ManagedFile;
import com.notaskflow.domain.vo.FileStatsVO;
import com.notaskflow.mapper.AttachmentMapper;
import com.notaskflow.mapper.ManagedFileMapper;
import com.notaskflow.service.FileStatsCacheService;
import com.notaskflow.utils.RedisUtil;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 文件统计缓存服务实现。
 *
 * @author LIN
 */
@Service
@RequiredArgsConstructor
public class FileStatsCacheServiceImpl implements FileStatsCacheService {

    private static final Duration FILE_STATS_CACHE_TTL = Duration.ofMinutes(30);

    private final ManagedFileMapper managedFileMapper;

    private final AttachmentMapper attachmentMapper;

    private final RedisUtil redisUtil;

    /**
     * 读取缓存中的文件统计。
     *
     * @param spaceId 空间标识
     * @return 文件统计
     */
    @Override
    public FileStatsVO getCached(Long spaceId) {
        if (spaceId == null) {
            return null;
        }
        return redisUtil.getObject(RedisKeyConstants.spaceFileStats(spaceId), FileStatsVO.class);
    }

    /**
     * 刷新空间文件统计缓存。
     *
     * @param spaceId 空间标识
     * @return 文件统计
     */
    @Override
    public FileStatsVO refresh(Long spaceId) {
        FileStatsVO stats = calculate(spaceId);
        redisUtil.set(RedisKeyConstants.spaceFileStats(spaceId), stats, FILE_STATS_CACHE_TTL);
        return stats;
    }

    private FileStatsVO calculate(Long spaceId) {
        List<ManagedFile> files = managedFileMapper.selectList(Wrappers.<ManagedFile>lambdaQuery()
                .eq(ManagedFile::getSpaceId, spaceId));
        long trashCount = files.stream().filter(file -> Boolean.TRUE.equals(file.getTrashed())).count();
        List<Long> activeAttachmentIds = files.stream()
                .filter(file -> !Boolean.TRUE.equals(file.getTrashed()))
                .map(ManagedFile::getAttachmentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Attachment> attachments = activeAttachmentIds.isEmpty()
                ? Collections.emptyList()
                : attachmentMapper.selectBatchIds(activeAttachmentIds);
        long totalSize = attachments.stream()
                .map(Attachment::getFileSize)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
        long activeCount = files.size() - trashCount;
        return new FileStatsVO(activeCount, totalSize, trashCount);
    }
}
