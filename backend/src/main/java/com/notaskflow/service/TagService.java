package com.notaskflow.service;

import com.notaskflow.domain.dto.request.NoteTagBindRequest;
import com.notaskflow.domain.dto.request.TagSaveRequest;
import com.notaskflow.domain.vo.TagVO;
import java.util.List;

/**
 * 标签服务接口。
 *
 * @author LIN
 */
public interface TagService {

    /**
     * 查询空间标签列表。
     *
     * @param spaceId 空间标识
     * @return 标签列表
     */
    List<TagVO> list(Long spaceId);

    /**
     * 创建标签。
     *
     * @param spaceId 空间标识
     * @param request 保存请求
     * @return 标签详情
     */
    TagVO create(Long spaceId, TagSaveRequest request);

    /**
     * 更新标签。
     *
     * @param spaceId 空间标识
     * @param id 标签标识
     * @param request 保存请求
     * @return 标签详情
     */
    TagVO update(Long spaceId, Long id, TagSaveRequest request);

    /**
     * 删除标签。
     *
     * @param spaceId 空间标识
     * @param id 标签标识
     */
    void delete(Long spaceId, Long id);

    /**
     * 批量绑定笔记标签。
     *
     * @param spaceId 空间标识
     * @param noteId 笔记标识
     * @param request 绑定请求
     */
    void bindTags(Long spaceId, Long noteId, NoteTagBindRequest request);

    /**
     * 移除笔记标签。
     *
     * @param spaceId 空间标识
     * @param noteId 笔记标识
     * @param tagId 标签标识
     */
    void removeTag(Long spaceId, Long noteId, Long tagId);
}
