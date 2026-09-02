package com.notaskflow.repository;

import com.notaskflow.domain.document.NoteSearchDocument;
import java.util.List;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 笔记 Elasticsearch 仓储。
 *
 * @author LIN
 */
@Repository
public interface NoteSearchRepository extends ElasticsearchRepository<NoteSearchDocument, Long> {

    /**
     * 根据空间标识查询笔记搜索文档。
     *
     * @param spaceId 空间标识
     * @return 笔记搜索文档列表
     */
    List<NoteSearchDocument> findBySpaceId(Long spaceId);
}
