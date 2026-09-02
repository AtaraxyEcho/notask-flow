package com.notaskflow.service;

import com.notaskflow.domain.entity.Attachment;
import com.notaskflow.domain.entity.ManagedFile;
import java.util.List;
import java.util.Optional;

/**
 * 文件搜索服务。
 *
 * @author LIN
 */
public interface FileSearchService {

    /**
     * 写入或更新文件搜索索引。
     *
     * @param managedFile 文件管理条目
     * @param attachment 附件元数据
     */
    default void index(ManagedFile managedFile, Attachment attachment) {
        index(managedFile, attachment, "");
    }

    /**
     * 写入或更新文件搜索索引。
     *
     * @param managedFile 文件管理条目
     * @param attachment 附件元数据
     * @param extractedText 文件正文提取内容
     */
    void index(ManagedFile managedFile, Attachment attachment, String extractedText);

    /**
     * 删除文件搜索索引。
     *
     * @param fileId 文件管理条目标识
     */
    void delete(Long fileId);

    /**
     * 搜索指定空间内匹配关键字的文件标识。
     *
     * @param spaceId 空间标识
     * @param keyword 搜索关键字
     * @param limit 最大结果数
     * @return 搜索结果，空 Optional 表示搜索引擎不可用
     */
    Optional<List<Long>> searchFileIds(Long spaceId, String keyword, int limit);
}
