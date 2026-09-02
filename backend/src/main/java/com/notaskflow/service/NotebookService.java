package com.notaskflow.service;

import com.notaskflow.domain.dto.request.NotebookSaveRequest;
import com.notaskflow.domain.vo.NotebookVO;
import java.util.List;

/**
 * 笔记本服务接口。
 *
 * @author LIN
 */
public interface NotebookService {

    /**
     * 查询空间下的笔记本树。
     *
     * @param spaceId 空间标识
     * @return 笔记本树
     */
    List<NotebookVO> listTree(Long spaceId);

    /**
     * 查询笔记本详情。
     *
     * @param spaceId 空间标识
     * @param id 笔记本标识
     * @return 笔记本详情
     */
    NotebookVO get(Long spaceId, Long id);

    /**
     * 创建笔记本。
     *
     * @param spaceId 空间标识
     * @param request 保存请求
     * @return 笔记本详情
     */
    NotebookVO create(Long spaceId, NotebookSaveRequest request);

    /**
     * 更新笔记本。
     *
     * @param spaceId 空间标识
     * @param id 笔记本标识
     * @param request 保存请求
     * @return 笔记本详情
     */
    NotebookVO update(Long spaceId, Long id, NotebookSaveRequest request);

    /**
     * 删除笔记本。
     *
     * @param spaceId 空间标识
     * @param id 笔记本标识
     */
    void delete(Long spaceId, Long id);
}
