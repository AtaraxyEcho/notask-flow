package com.notaskflow.service.impl;

import com.notaskflow.domain.document.NoteSearchDocument;
import com.notaskflow.domain.entity.Note;
import com.notaskflow.repository.NoteSearchRepository;
import com.notaskflow.service.NoteSearchService;
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
 * 基于 Spring Data Elasticsearch 的笔记搜索服务实现。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchNoteSearchServiceImpl implements NoteSearchService {

    private static final int MAX_RESULTS = 50;

    private static final Duration FAILURE_RETRY_INTERVAL = Duration.ofSeconds(30);

    private final NoteSearchRepository noteSearchRepository;

    private final ElasticsearchOperations elasticsearchOperations;

    private final AtomicBoolean indexChecked = new AtomicBoolean(false);

    private final AtomicLong nextRetryEpochMillis = new AtomicLong(0);

    /**
     * 写入或更新笔记搜索索引。
     *
     * @param note 笔记实体
     */
    @Override
    public void index(Note note) {
        if (note == null || note.getId() == null || !ensureIndex()) {
            return;
        }
        try {
            noteSearchRepository.save(toDocument(note));
        } catch (RuntimeException exception) {
            log.warn("笔记索引写入失败，noteId={}", note.getId(), exception);
            delayNextRetry();
        }
    }

    /**
     * 删除笔记搜索索引。
     *
     * @param noteId 笔记标识
     */
    @Override
    public void delete(Long noteId) {
        if (noteId == null || !ensureIndex()) {
            return;
        }
        try {
            noteSearchRepository.deleteById(noteId);
        } catch (RuntimeException exception) {
            log.warn("笔记索引删除失败，noteId={}", noteId, exception);
            delayNextRetry();
        }
    }

    /**
     * 搜索指定空间内匹配关键字的笔记标识。
     *
     * @param spaceId 空间标识
     * @param keyword 搜索关键字
     * @param limit 最大结果数
     * @return 搜索结果，空 Optional 表示搜索引擎不可用
     */
    @Override
    public Optional<List<Long>> searchNoteIds(Long spaceId, String keyword, int limit) {
        if (spaceId == null || !StringUtils.hasText(keyword) || !ensureIndex()) {
            return Optional.empty();
        }
        try {
            SearchHits<NoteSearchDocument> hits = elasticsearchOperations.search(
                    buildSearchQuery(spaceId, keyword, limit),
                    NoteSearchDocument.class);
            return Optional.of(hits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .map(NoteSearchDocument::getNoteId)
                    .filter(Objects::nonNull)
                    .toList());
        } catch (RuntimeException exception) {
            log.warn("笔记索引搜索失败，spaceId={}", spaceId, exception);
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
            IndexOperations indexOperations = elasticsearchOperations.indexOps(NoteSearchDocument.class);
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
                log.warn("笔记索引创建失败");
            }
            return created;
        } catch (RuntimeException exception) {
            log.warn("笔记索引检查或创建失败", exception);
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
                        .must(must -> must.multiMatch(multiMatch -> multiMatch
                                .query(keyword.trim())
                                .fields("title^3", "content", "contentHtml")))))
                .withPageable(PageRequest.of(0, safeLimit(limit)))
                .build();
    }

    private NoteSearchDocument toDocument(Note note) {
        return new NoteSearchDocument(
                note.getId(),
                note.getSpaceId(),
                note.getNotebookId(),
                note.getProjectId(),
                note.getTitle(),
                note.getContent(),
                note.getContentHtml(),
                note.getGmtModified()
        );
    }

    private int safeLimit(int limit) {
        return Math.max(1, Math.min(limit, MAX_RESULTS));
    }

    private void delayNextRetry() {
        nextRetryEpochMillis.set(System.currentTimeMillis() + FAILURE_RETRY_INTERVAL.toMillis());
    }
}
