package com.notaskflow.service;

import com.notaskflow.common.PageResponse;
import com.notaskflow.domain.dto.request.TodoSaveRequest;
import com.notaskflow.domain.query.TodoQuery;
import com.notaskflow.domain.vo.TodoVO;

/**
 * 待办服务接口。
 *
 * @author LIN
 */
public interface TodoService {

    /**
     * 分页查询当前用户待办。
     *
     * @param spaceId 空间标识
     * @param query 查询条件
     * @return 分页待办
     */
    PageResponse<TodoVO> page(Long spaceId, TodoQuery query);

    /**
     * 查询待办详情。
     *
     * @param spaceId 空间标识
     * @param id 待办标识
     * @return 待办详情
     */
    TodoVO get(Long spaceId, Long id);

    /**
     * 创建个人待办。
     *
     * @param spaceId 空间标识
     * @param request 保存请求
     * @return 待办详情
     */
    TodoVO create(Long spaceId, TodoSaveRequest request);

    /**
     * 更新待办。
     *
     * @param spaceId 空间标识
     * @param id 待办标识
     * @param request 保存请求
     * @return 待办详情
     */
    TodoVO update(Long spaceId, Long id, TodoSaveRequest request);

    /**
     * 标记待办完成。
     *
     * @param spaceId 空间标识
     * @param id 待办标识
     * @return 待办详情
     */
    TodoVO complete(Long spaceId, Long id);

    /**
     * 标记待办未完成。
     *
     * @param spaceId 空间标识
     * @param id 待办标识
     * @return 待办详情
     */
    TodoVO uncomplete(Long spaceId, Long id);

    /**
     * 删除待办。
     *
     * @param spaceId 空间标识
     * @param id 待办标识
     */
    void delete(Long spaceId, Long id);
}
