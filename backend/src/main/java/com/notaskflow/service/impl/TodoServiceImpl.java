package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notaskflow.common.PageResponse;
import com.notaskflow.common.enums.SpaceRealtimeEventType;
import com.notaskflow.common.enums.SpaceType;
import com.notaskflow.common.enums.TaskMemberStatus;
import com.notaskflow.common.enums.TaskMode;
import com.notaskflow.common.enums.TaskStatus;
import com.notaskflow.domain.dto.request.TodoSaveRequest;
import com.notaskflow.domain.entity.Space;
import com.notaskflow.domain.entity.Task;
import com.notaskflow.domain.entity.TaskMember;
import com.notaskflow.domain.entity.Todo;
import com.notaskflow.domain.query.TodoQuery;
import com.notaskflow.domain.vo.TodoVO;
import com.notaskflow.exception.ResourceNotFoundException;
import com.notaskflow.mapper.SpaceMapper;
import com.notaskflow.mapper.TaskMapper;
import com.notaskflow.mapper.TaskMemberMapper;
import com.notaskflow.mapper.TodoMapper;
import com.notaskflow.security.PermissionValidator;
import com.notaskflow.service.SpaceRealtimeEventService;
import com.notaskflow.service.TodoService;
import com.notaskflow.utils.LoginUserUtil;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 待办服务实现，处理当前用户在空间内的待办事项。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TodoServiceImpl implements TodoService {

    private final TodoMapper todoMapper;

    private final SpaceMapper spaceMapper;

    private final TaskMemberMapper taskMemberMapper;

    private final TaskMapper taskMapper;

    private final PermissionValidator permissionValidator;

    private final SpaceRealtimeEventService spaceRealtimeEventService;

    /**
     * 分页查询指定空间下的待办事项。
     *
     * @param spaceId 空间标识
     * @param query 查询条件
     * @return 待办分页结果
     */
    @Override
    public PageResponse<TodoVO> page(Long spaceId, TodoQuery query) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
        Page<Todo> page = new Page<>(query.safePageNum(), query.safePageSize());
        LambdaQueryWrapper<Todo> wrapper = Wrappers.<Todo>lambdaQuery()
                .eq(Todo::getSpaceId, spaceId)
                .like(StringUtils.hasText(query.getKeyword()), Todo::getTitle,
                        StringUtils.hasText(query.getKeyword()) ? query.getKeyword().trim() : null)
                .eq(query.getIsCompleted() != null, Todo::getIsCompleted, query.getIsCompleted())
                .orderByAsc(Todo::getIsCompleted)
                .orderByAsc(Todo::getDeadline)
                .orderByDesc(Todo::getGmtCreate);
        if (shouldRestrictToCurrentUser(spaceId)) {
            wrapper.eq(Todo::getUserId, currentUserId);
        } else {
            wrapper.eq(query.getAssigneeId() != null, Todo::getUserId, query.getAssigneeId());
        }
        IPage<Todo> result = todoMapper.selectPage(page, wrapper);
        List<TodoVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResponse.of(page, list);
    }

    /**
     * 查询当前用户的单个待办事项。
     *
     * @param spaceId 空间标识
     * @param id 待办标识
     * @return 待办信息
     */
    @Override
    public TodoVO get(Long spaceId, Long id) {
        return toVO(findCurrentTodo(spaceId, id));
    }

    /**
     * 创建当前用户的待办事项。
     *
     * @param spaceId 空间标识
     * @param request 待办保存请求
     * @return 待办信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TodoVO create(Long spaceId, TodoSaveRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
        Todo todo = new Todo();
        todo.setSpaceId(spaceId);
        todo.setUserId(currentUserId);
        todo.setTitle(request.getTitle());
        todo.setDeadline(request.getDeadline());
        todo.setIsCompleted(false);
        todoMapper.insert(todo);
        log.info("待办创建完成，spaceId={}，todoId={}，operatorId={}", spaceId, todo.getId(), currentUserId);
        publishTodoRealtimeEvent(spaceId, SpaceRealtimeEventType.TODO_CREATED, todo);
        return toVO(todo);
    }

    /**
     * 更新当前用户的待办事项。
     *
     * @param spaceId 空间标识
     * @param id 待办标识
     * @param request 待办保存请求
     * @return 待办信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TodoVO update(Long spaceId, Long id, TodoSaveRequest request) {
        Todo todo = findCurrentTodo(spaceId, id);
        todo.setTitle(request.getTitle());
        todo.setDeadline(request.getDeadline());
        todoMapper.updateById(todo);
        log.info("待办更新完成，spaceId={}，todoId={}，operatorId={}",
                spaceId, id, LoginUserUtil.currentUserId());
        publishTodoRealtimeEvent(spaceId, SpaceRealtimeEventType.TODO_UPDATED, todo);
        return toVO(todo);
    }

    /**
     * 标记当前用户的待办事项为已完成。
     *
     * @param spaceId 空间标识
     * @param id 待办标识
     * @return 待办信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TodoVO complete(Long spaceId, Long id) {
        Todo todo = findCurrentTodo(spaceId, id);
        todo.setIsCompleted(true);
        todoMapper.updateById(todo);
        log.info("待办标记完成，spaceId={}，todoId={}，operatorId={}",
                spaceId, id, LoginUserUtil.currentUserId());
        publishTodoRealtimeEvent(spaceId, SpaceRealtimeEventType.TODO_UPDATED, todo);
        return toVO(todo);
    }

    /**
     * 标记当前用户的待办事项为未完成。
     *
     * @param spaceId 空间标识
     * @param id 待办标识
     * @return 待办信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TodoVO uncomplete(Long spaceId, Long id) {
        Todo todo = findCurrentTodo(spaceId, id);
        todo.setIsCompleted(false);
        todoMapper.updateById(todo);
        log.info("待办标记未完成，spaceId={}，todoId={}，operatorId={}",
                spaceId, id, LoginUserUtil.currentUserId());
        publishTodoRealtimeEvent(spaceId, SpaceRealtimeEventType.TODO_UPDATED, todo);
        return toVO(todo);
    }

    /**
     * 删除当前用户的待办事项。
     *
     * @param spaceId 空间标识
     * @param id 待办标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long spaceId, Long id) {
        Todo todo = findCurrentTodo(spaceId, id);
        todoMapper.deleteById(todo.getId());
        log.info("待办删除完成，spaceId={}，todoId={}，operatorId={}",
                spaceId, id, LoginUserUtil.currentUserId());
        publishTodoRealtimeEvent(spaceId, SpaceRealtimeEventType.TODO_DELETED, todo);
    }

    /**
     * 发布待办实时事件。
     *
     * @param spaceId 空间标识
     * @param type 事件类型
     * @param todo 待办实体
     */
    private void publishTodoRealtimeEvent(Long spaceId, SpaceRealtimeEventType type, Todo todo) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("todoId", todo.getId());
        payload.put("userId", todo.getUserId());
        payload.put("title", todo.getTitle());
        payload.put("completed", todo.getIsCompleted());
        spaceRealtimeEventService.publish(spaceId, type, payload);
    }

    /**
     * 查询当前用户可访问的待办。
     *
     * @param spaceId 空间标识
     * @param id 待办标识
     * @return 待办实体
     */
    private Todo findCurrentTodo(Long spaceId, Long id) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
        Todo todo = todoMapper.selectOne(Wrappers.<Todo>lambdaQuery()
                .eq(Todo::getSpaceId, spaceId)
                .eq(Todo::getUserId, currentUserId)
                .eq(Todo::getId, id));
        if (todo == null) {
            throw new ResourceNotFoundException("待办不存在");
        }
        return todo;
    }

    /**
     * 转换待办视图对象。
     *
     *
     * @return 待办视图对象
     */
    private TodoVO toVO(Todo todo) {
        TaskMember member = taskMemberOf(todo);
        Task task = taskOf(member);
        return new TodoVO(todo.getId(), todo.getSpaceId(), todo.getUserId(), todo.getTaskMemberId(),
                member == null ? null : member.getTaskId(), taskModeOf(task), taskStatusOf(task),
                taskMemberStatusOf(member), todo.getTitle(), todo.getIsCompleted(), todo.getDeadline(),
                completedAtOf(todo), todo.getGmtCreate());
    }

    /**
     * 查询待办关联的任务成员。
     *
     *
     * @return 任务成员
     */
    private TaskMember taskMemberOf(Todo todo) {
        if (todo.getTaskMemberId() == null) {
            return null;
        }
        return taskMemberMapper.selectById(todo.getTaskMemberId());
    }

    /**
     * 查询任务成员关联的任务。
     *
     * @param member 任务成员
     * @return 任务实体
     */
    private Task taskOf(TaskMember member) {
        if (member == null) {
            return null;
        }
        return taskMapper.selectById(member.getTaskId());
    }

    /**
     * 获取任务模式。
     *
     * @param task 任务实体
     * @return 任务模式
     */
    private TaskMode taskModeOf(Task task) {
        return task == null ? null : task.getMode();
    }

    /**
     * 获取任务状态。
     *
     * @param task 任务实体
     * @return 任务状态
     */
    private TaskStatus taskStatusOf(Task task) {
        if (task == null) {
            return null;
        }
        if (TaskStatus.OPEN.equals(task.getStatus())) {
            return TaskStatus.PENDING;
        }
        return task.getStatus();
    }

    /**
     * 获取任务成员状态。
     *
     * @param member 任务成员
     * @return 任务成员状态
     */
    private TaskMemberStatus taskMemberStatusOf(TaskMember member) {
        return member == null ? null : member.getStatus();
    }

    /**
     * 判断当前空间是否需要限制为当前用户视角。
     *
     * @param spaceId 空间标识
     * @return 是否仅返回当前用户待办
     */
    private boolean shouldRestrictToCurrentUser(Long spaceId) {
        Space space = spaceMapper.selectById(spaceId);
        return space == null || SpaceType.PERSONAL.equals(space.getType());
    }

    /**
     * 获取待办完成时间。
     *
     *
     * @return 完成时间
     */
    private LocalDateTime completedAtOf(Todo todo) {
        if (todo == null || !Boolean.TRUE.equals(todo.getIsCompleted())) {
            return null;
        }
        return todo.getGmtModified();
    }
}
