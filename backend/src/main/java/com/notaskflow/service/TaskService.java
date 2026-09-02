package com.notaskflow.service;

import com.notaskflow.common.PageResponse;
import com.notaskflow.domain.dto.request.TaskAssignmentRequest;
import com.notaskflow.domain.dto.request.TaskClaimRequest;
import com.notaskflow.domain.dto.request.TaskCommentCreateRequest;
import com.notaskflow.domain.dto.request.TaskCreateRequest;
import com.notaskflow.domain.dto.request.TaskMemberCompleteRequest;
import com.notaskflow.domain.dto.request.TaskStatusUpdateRequest;
import com.notaskflow.domain.dto.request.TaskUpdateRequest;
import com.notaskflow.domain.query.TaskQuery;
import com.notaskflow.domain.vo.TaskCommentVO;
import com.notaskflow.domain.vo.TaskMemberVO;
import com.notaskflow.domain.vo.TaskVO;
import java.util.List;

/**
 * 任务服务接口。
 *
 * @author LIN
 */
public interface TaskService {

    /**
     * 分页查询任务。
     *
     * @param spaceId 空间标识
     * @param query 查询条件
     * @return 分页任务
     */
    PageResponse<TaskVO> page(Long spaceId, TaskQuery query);

    /**
     * 创建任务。
     *
     * @param spaceId 空间标识
     * @param request 创建请求
     * @return 任务详情
     */
    TaskVO create(Long spaceId, TaskCreateRequest request);

    /**
     * 查询任务详情。
     *
     * @param spaceId 空间标识
     * @param id 任务标识
     * @return 任务详情
     */
    TaskVO get(Long spaceId, Long id);

    /**
     * 更新任务基础信息。
     *
     * @param spaceId 空间标识
     * @param id 任务标识
     * @param request 更新请求
     * @return 任务详情
     */
    TaskVO update(Long spaceId, Long id, TaskUpdateRequest request);

    /**
     * 更新任务整体状态。
     *
     * @param spaceId 空间标识
     * @param id 任务标识
     * @param request 状态更新请求
     * @return 任务详情
     */
    TaskVO changeStatus(Long spaceId, Long id, TaskStatusUpdateRequest request);

    /**
     * 删除任务并同步删除关联待办。
     *
     * @param spaceId 空间标识
     * @param id 任务标识
     */
    void delete(Long spaceId, Long id);

    /**
     * 开始处理任务成员职责。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     * @param memberId 成员工作项标识
     * @return 成员工作项
     */
    TaskMemberVO startMember(Long spaceId, Long taskId, Long memberId);

    /**
     * 完成任务成员职责。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     * @param memberId 成员工作项标识
     * @param request 完成提交请求
     * @return 成员工作项
     */
    TaskMemberVO completeMember(Long spaceId, Long taskId, Long memberId, TaskMemberCompleteRequest request);

    /**
     * 开放模式认领任务职责。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     * @param request 认领请求
     * @return 成员工作项
     */
    TaskMemberVO claim(Long spaceId, Long taskId, TaskClaimRequest request);

    /**
     * 指派模式添加职责。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     * @param request 分配请求
     * @return 成员工作项
     */
    TaskMemberVO assign(Long spaceId, Long taskId, TaskAssignmentRequest request);

    /**
     * 移除任务成员职责。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     * @param memberId 成员工作项标识
     */
    void removeMember(Long spaceId, Long taskId, Long memberId);

    /**
     * 查询任务评论。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     * @return 评论列表
     */
    List<TaskCommentVO> listComments(Long spaceId, Long taskId);

    /**
     * 添加任务评论。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     * @param request 评论请求
     * @return 评论详情
     */
    TaskCommentVO addComment(Long spaceId, Long taskId, TaskCommentCreateRequest request);

    /**
     * 删除任务评论。
     *
     * @param commentId 评论标识
     */
    void deleteComment(Long commentId);
}
