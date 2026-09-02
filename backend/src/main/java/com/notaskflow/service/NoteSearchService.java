package com.notaskflow.service;

import com.notaskflow.domain.entity.Note;
import java.util.List;
import java.util.Optional;

/**
 * 笔记搜索索引服务。
 *
 * @author LIN
 */
public interface NoteSearchService {

    /**
     * 写入或更新笔记搜索索引。
     *
     * @param note 笔记实体
     */
    void index(Note note);

    /**
     * 删除笔记搜索索引。
     *
     * @param noteId 笔记标识
     */
    void delete(Long noteId);

    /**
     * 搜索指定空间内匹配关键字的笔记标识。
     *
     * @param spaceId 空间标识
     * @param keyword 搜索关键字
     * @param limit 最大结果数
     * @return 搜索结果，空 Optional 表示搜索引擎不可用，应回退数据库搜索
     */
    Optional<List<Long>> searchNoteIds(Long spaceId, String keyword, int limit);
}
