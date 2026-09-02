package com.notaskflow.repository;

import com.notaskflow.domain.document.FileSearchDocument;
import java.util.List;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 文件 Elasticsearch 仓储。
 *
 * @author LIN
 */
@Repository
public interface FileSearchRepository extends ElasticsearchRepository<FileSearchDocument, Long> {

    /**
     * 根据空间标识查询文件搜索文档。
     *
     * @param spaceId 空间标识
     * @return 文件搜索文档列表
     */
    List<FileSearchDocument> findBySpaceId(Long spaceId);
}
