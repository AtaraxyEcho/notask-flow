package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.notaskflow.common.PageResponse;
import com.notaskflow.domain.entity.Attachment;
import com.notaskflow.domain.entity.BusinessAttachment;
import com.notaskflow.domain.entity.Space;
import com.notaskflow.domain.entity.User;
import com.notaskflow.domain.vo.AdminOrphanCleanResultVO;
import com.notaskflow.domain.vo.AdminOrphanFileVO;
import com.notaskflow.domain.vo.AdminStorageRankVO;
import com.notaskflow.domain.vo.AdminStorageSummaryVO;
import com.notaskflow.mapper.AttachmentMapper;
import com.notaskflow.mapper.BusinessAttachmentMapper;
import com.notaskflow.mapper.SpaceMapper;
import com.notaskflow.mapper.UserMapper;
import com.notaskflow.service.AdminStorageService;
import com.notaskflow.storage.MinioStorageService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端存储管理服务实现。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStorageServiceImpl implements AdminStorageService {

    private static final int TOP_LIMIT = 10;

    private static final long DEFAULT_PAGE_NUM = 1L;

    private static final long DEFAULT_PAGE_SIZE = 10L;

    private static final long MAX_PAGE_SIZE = 100L;

    private final AttachmentMapper attachmentMapper;

    private final BusinessAttachmentMapper businessAttachmentMapper;

    private final UserMapper userMapper;

    private final SpaceMapper spaceMapper;

    private final MinioStorageService minioStorageService;

    /**
     * 查询存储汇总。
     *
     * @return 存储汇总
     */
    @Override
    public AdminStorageSummaryVO summary() {
        List<Attachment> activeAttachments = listActiveAttachments();
        List<Attachment> deletedAttachments = listDeletedAttachments();
        List<Attachment> orphanAttachments = filterOrphanAttachments(activeAttachments);
        return new AdminStorageSummaryVO(
                (long) activeAttachments.size(),
                sumFileSize(activeAttachments),
                (long) orphanAttachments.size(),
                sumFileSize(orphanAttachments),
                (long) deletedAttachments.size(),
                sumFileSize(deletedAttachments));
    }

    /**
     * 查询用户存储占用排行。
     *
     * @return 用户存储占用排行
     */
    @Override
    public List<AdminStorageRankVO> topUsers() {
        List<Attachment> attachments = listActiveAttachments();
        Map<Long, StorageAggregate> aggregates = aggregateBy(attachments, Attachment::getUploaderId);
        Map<Long, User> users = loadUsers(aggregates.keySet());
        return toRankList(aggregates, userId -> {
            User user = users.get(userId);
            String name = user == null ? "未知用户" : displayUserName(user);
            String description = user == null ? "" : user.getEmail();
            return new RankTarget(name, description);
        });
    }

    /**
     * 查询空间存储占用排行。
     *
     * @return 空间存储占用排行
     */
    @Override
    public List<AdminStorageRankVO> topSpaces() {
        List<Attachment> attachments = listActiveAttachments();
        Map<Long, StorageAggregate> aggregates = aggregateBy(attachments, Attachment::getSpaceId);
        Map<Long, Space> spaces = loadSpaces(aggregates.keySet());
        return toRankList(aggregates, spaceId -> {
            Space space = spaces.get(spaceId);
            String name = space == null ? "未知空间" : space.getName();
            String description = space == null || space.getType() == null ? "" : space.getType().name();
            return new RankTarget(name, description);
        });
    }

    /**
     * 分页查询孤立文件。
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 孤立文件分页
     */
    @Override
    public PageResponse<AdminOrphanFileVO> orphanFiles(long pageNum, long pageSize) {
        List<Attachment> orphanAttachments = filterOrphanAttachments(listActiveAttachments());
        long safePageNum = safePageNum(pageNum);
        long safePageSize = safePageSize(pageSize);
        int fromIndex = (int) Math.min(orphanAttachments.size(), (safePageNum - 1) * safePageSize);
        int toIndex = (int) Math.min(orphanAttachments.size(), fromIndex + safePageSize);
        List<Attachment> pageAttachments = orphanAttachments.subList(fromIndex, toIndex);
        List<AdminOrphanFileVO> list = toOrphanFileList(pageAttachments);
        return new PageResponse<>((long) orphanAttachments.size(), safePageNum, safePageSize, list);
    }

    /**
     * 清理孤立文件。
     *
     * @return 清理结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminOrphanCleanResultVO cleanOrphanFiles() {
        List<Attachment> orphanAttachments = filterOrphanAttachments(listActiveAttachments());
        long cleanedCount = 0L;
        long cleanedBytes = 0L;
        long failedCount = 0L;
        for (Attachment attachment : orphanAttachments) {
            try {
                minioStorageService.delete(attachment.getStoragePath());
                attachmentMapper.deleteById(attachment.getId());
                cleanedCount += 1L;
                cleanedBytes += safeFileSize(attachment);
            } catch (RuntimeException exception) {
                failedCount += 1L;
                log.warn("孤立文件清理失败，attachmentId={}", attachment.getId(), exception);
            }
        }
        return new AdminOrphanCleanResultVO(cleanedCount, cleanedBytes, failedCount);
    }

    /**
     * 查询未删除附件。
     *
     * @return 附件列表
     */
    private List<Attachment> listActiveAttachments() {
        return attachmentMapper.selectList(Wrappers.<Attachment>lambdaQuery()
                .orderByDesc(Attachment::getGmtCreate));
    }

    /**
     * 查询已删除附件。
     *
     * @return 附件列表
     */
    private List<Attachment> listDeletedAttachments() {
        return attachmentMapper.selectDeletedStorageRows();
    }

    /**
     * 过滤孤立附件。
     *
     * @param attachments 附件列表
     * @return 孤立附件列表
     */
    private List<Attachment> filterOrphanAttachments(List<Attachment> attachments) {
        if (attachments.isEmpty()) {
            return List.of();
        }
        Set<Long> referencedAttachmentIds = businessAttachmentMapper.selectList(Wrappers.<BusinessAttachment>lambdaQuery()
                        .select(BusinessAttachment::getAttachmentId))
                .stream()
                .map(BusinessAttachment::getAttachmentId)
                .collect(Collectors.toCollection(HashSet::new));
        return attachments.stream()
                .filter(attachment -> !referencedAttachmentIds.contains(attachment.getId()))
                .toList();
    }

    /**
     * 聚合附件存储。
     *
     * @param attachments 附件列表
     * @param keyResolver 维度解析函数
     * @return 聚合结果
     */
    private Map<Long, StorageAggregate> aggregateBy(List<Attachment> attachments, Function<Attachment, Long> keyResolver) {
        Map<Long, StorageAggregate> aggregates = new HashMap<>();
        for (Attachment attachment : attachments) {
            Long key = keyResolver.apply(attachment);
            if (key == null) {
                continue;
            }
            StorageAggregate aggregate = aggregates.computeIfAbsent(key, current -> new StorageAggregate());
            aggregate.add(attachment);
        }
        return aggregates;
    }

    /**
     * 转换排行列表。
     *
     * @param aggregates 聚合结果
     * @param targetResolver 目标信息解析函数
     * @return 排行列表
     */
    private List<AdminStorageRankVO> toRankList(
            Map<Long, StorageAggregate> aggregates,
            Function<Long, RankTarget> targetResolver) {
        return aggregates.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<Long, StorageAggregate> entry) -> entry.getValue().storageBytes())
                        .reversed())
                .limit(TOP_LIMIT)
                .map(entry -> {
                    RankTarget target = targetResolver.apply(entry.getKey());
                    StorageAggregate aggregate = entry.getValue();
                    return new AdminStorageRankVO(
                            entry.getKey(),
                            target.name(),
                            target.description(),
                            aggregate.fileCount(),
                            aggregate.storageBytes());
                })
                .toList();
    }

    /**
     * 转换孤立文件列表。
     *
     * @param attachments 附件列表
     * @return 孤立文件视图列表
     */
    private List<AdminOrphanFileVO> toOrphanFileList(List<Attachment> attachments) {
        Map<Long, User> users = loadUsers(attachments.stream()
                .map(Attachment::getUploaderId)
                .collect(Collectors.toSet()));
        Map<Long, Space> spaces = loadSpaces(attachments.stream()
                .map(Attachment::getSpaceId)
                .collect(Collectors.toSet()));
        List<AdminOrphanFileVO> list = new ArrayList<>();
        for (Attachment attachment : attachments) {
            User user = users.get(attachment.getUploaderId());
            Space space = spaces.get(attachment.getSpaceId());
            list.add(new AdminOrphanFileVO(
                    attachment.getId(),
                    attachment.getFileName(),
                    safeFileSize(attachment),
                    attachment.getMimeType(),
                    attachment.getUploaderId(),
                    user == null ? "未知用户" : displayUserName(user),
                    user == null ? "" : user.getEmail(),
                    attachment.getSpaceId(),
                    space == null ? "未知空间" : space.getName(),
                    attachment.getGmtCreate()));
        }
        return list;
    }

    /**
     * 批量加载用户。
     *
     * @param userIds 用户标识集合
     * @return 用户映射
     */
    private Map<Long, User> loadUsers(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    /**
     * 批量加载空间。
     *
     * @param spaceIds 空间标识集合
     * @return 空间映射
     */
    private Map<Long, Space> loadSpaces(Set<Long> spaceIds) {
        if (spaceIds.isEmpty()) {
            return Map.of();
        }
        return spaceMapper.selectBatchIds(spaceIds).stream()
                .collect(Collectors.toMap(Space::getId, Function.identity()));
    }

    /**
     * 统计文件大小。
     *
     * @param attachments 附件列表
     * @return 文件大小
     */
    private Long sumFileSize(List<Attachment> attachments) {
        return attachments.stream()
                .mapToLong(this::safeFileSize)
                .sum();
    }

    /**
     * 读取安全文件大小。
     *
     * @param attachment 附件
     * @return 文件大小
     */
    private long safeFileSize(Attachment attachment) {
        return attachment.getFileSize() == null ? 0L : attachment.getFileSize();
    }

    /**
     * 读取用户展示名称。
     *
     * @param user 用户
     * @return 展示名称
     */
    private String displayUserName(User user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        return user.getUsername();
    }

    /**
     * 读取安全页码。
     *
     * @param pageNum 页码
     * @return 安全页码
     */
    private long safePageNum(long pageNum) {
        return pageNum <= 0 ? DEFAULT_PAGE_NUM : pageNum;
    }

    /**
     * 读取安全每页数量。
     *
     * @param pageSize 每页数量
     * @return 安全每页数量
     */
    private long safePageSize(long pageSize) {
        if (pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 存储聚合对象。
     */
    private static class StorageAggregate {

        private long fileCount;

        private long storageBytes;

        /**
         * 添加附件。
         *
         * @param attachment 附件
         */
        void add(Attachment attachment) {
            fileCount += 1L;
            storageBytes += attachment.getFileSize() == null ? 0L : attachment.getFileSize();
        }

        /**
         * 查询文件数量。
         *
         * @return 文件数量
         */
        long fileCount() {
            return fileCount;
        }

        /**
         * 查询存储字节数。
         *
         * @return 存储字节数
         */
        long storageBytes() {
            return storageBytes;
        }
    }

    /**
     * 排行目标信息。
     *
     * @param name 名称
     * @param description 描述
     */
    private record RankTarget(String name, String description) {
    }
}
