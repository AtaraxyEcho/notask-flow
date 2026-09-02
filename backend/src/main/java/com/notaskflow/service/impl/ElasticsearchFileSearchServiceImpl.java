package com.notaskflow.service.impl;

import com.notaskflow.domain.document.FileSearchDocument;
import com.notaskflow.domain.entity.Attachment;
import com.notaskflow.domain.entity.ManagedFile;
import com.notaskflow.repository.FileSearchRepository;
import com.notaskflow.service.FileSearchService;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 基于 Spring Data Elasticsearch 的文件搜索服务实现。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchFileSearchServiceImpl implements FileSearchService {

    private static final int MAX_RESULTS = 50;

    private static final Duration FAILURE_RETRY_INTERVAL = Duration.ofSeconds(30);

    private final FileSearchRepository fileSearchRepository;

    private final ElasticsearchOperations elasticsearchOperations;

    private final AtomicBoolean indexChecked = new AtomicBoolean(false);

    private final AtomicLong nextRetryEpochMillis = new AtomicLong(0);

    /**
     * 写入或更新文件搜索索引。
     *
     * @param managedFile 文件管理条目
     * @param attachment 附件元数据
     * @param extractedText 文件正文提取内容
     */
    @Override
    public void index(ManagedFile managedFile, Attachment attachment, String extractedText) {
        if (managedFile == null || managedFile.getId() == null || attachment == null || !ensureIndex()) {
            return;
        }
        try {
            fileSearchRepository.save(toDocument(managedFile, attachment, extractedText));
        } catch (RuntimeException exception) {
            log.warn("文件索引写入失败，fileId={}", managedFile.getId(), exception);
            delayNextRetry();
        }
    }

    /**
     * 删除文件搜索索引。
     *
     * @param fileId 文件管理条目标识
     */
    @Override
    public void delete(Long fileId) {
        if (fileId == null || !ensureIndex()) {
            return;
        }
        try {
            fileSearchRepository.deleteById(fileId);
        } catch (RuntimeException exception) {
            log.warn("文件索引删除失败，fileId={}", fileId, exception);
            delayNextRetry();
        }
    }

    /**
     * 搜索指定空间内匹配关键字的文件标识。
     *
     * @param spaceId 空间标识
     * @param keyword 搜索关键字
     * @param limit 最大结果数
     * @return 搜索结果，空 Optional 表示搜索引擎不可用
     */
    @Override
    public Optional<List<Long>> searchFileIds(Long spaceId, String keyword, int limit) {
        if (spaceId == null || !StringUtils.hasText(keyword) || !ensureIndex()) {
            return Optional.empty();
        }
        try {
            SearchHits<FileSearchDocument> hits = elasticsearchOperations.search(
                    buildSearchQuery(spaceId, keyword, limit),
                    FileSearchDocument.class);
            return Optional.of(hits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .map(FileSearchDocument::getFileId)
                    .filter(Objects::nonNull)
                    .toList());
        } catch (RuntimeException exception) {
            log.warn("文件索引搜索失败，spaceId={}", spaceId, exception);
            delayNextRetry();
            return Optional.empty();
        }
    }

    private boolean ensureIndex() {
        if (indexChecked.get()) {
            return true;
        }
        if (System.currentTimeMillis() < nextRetryEpochMillis.get()) {
            return false;
        }
        try {
            IndexOperations indexOperations = elasticsearchOperations.indexOps(FileSearchDocument.class);
            if (indexOperations.exists()) {
                indexChecked.set(true);
                nextRetryEpochMillis.set(0);
                return true;
            }
            boolean created = indexOperations.createWithMapping();
            indexChecked.set(created);
            if (created) {
                nextRetryEpochMillis.set(0);
            } else {
                delayNextRetry();
                log.warn("文件索引创建失败");
            }
            return created;
        } catch (RuntimeException exception) {
            log.warn("文件索引检查或创建失败", exception);
            delayNextRetry();
            return false;
        }
    }

    private NativeQuery buildSearchQuery(Long spaceId, String keyword, int limit) {
        return NativeQuery.builder()
                .withQuery(query -> query.bool(bool -> bool
                        .filter(filter -> filter.term(term -> term
                                .field("spaceId")
                                .value(spaceId)))
                        .filter(filter -> filter.term(term -> term
                                .field("trashed")
                                .value(false)))
                        .must(must -> must.multiMatch(multiMatch -> multiMatch
                                .query(keyword.trim())
                                .fields("displayName^3", "fileName^2", "mimeType", "extractedText")))))
                .withPageable(PageRequest.of(0, safeLimit(limit)))
                .build();
    }

    private FileSearchDocument toDocument(ManagedFile managedFile, Attachment attachment, String extractedText) {
        return new FileSearchDocument(
                managedFile.getId(),
                managedFile.getAttachmentId(),
                managedFile.getSpaceId(),
                managedFile.getFolderId(),
                managedFile.getDisplayName(),
                attachment.getFileName(),
                attachment.getMimeType(),
                attachment.getFileSize(),
                attachment.getUploaderId(),
                managedFile.getCreatedBy(),
                managedFile.getTrashed(),
                StringUtils.hasText(extractedText) ? extractedText : "",
                managedFile.getGmtCreate(),
                managedFile.getGmtModified()
        );
    }

    private int safeLimit(int limit) {
        return Math.max(1, Math.min(limit, MAX_RESULTS));
    }

    private void delayNextRetry() {
        nextRetryEpochMillis.set(System.currentTimeMillis() + FAILURE_RETRY_INTERVAL.toMillis());
    }
}
