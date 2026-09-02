package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notaskflow.common.enums.SpaceType;
import com.notaskflow.common.enums.TaskMemberStatus;
import com.notaskflow.common.enums.TaskStatus;
import com.notaskflow.domain.entity.Note;
import com.notaskflow.domain.entity.Role;
import com.notaskflow.domain.entity.Space;
import com.notaskflow.domain.entity.SpaceMember;
import com.notaskflow.domain.entity.Task;
import com.notaskflow.domain.entity.TaskMember;
import com.notaskflow.domain.entity.User;
import com.notaskflow.domain.vo.MemberTaskLoadVO;
import com.notaskflow.domain.vo.PersonalNoteTrendVO;
import com.notaskflow.domain.vo.PersonalStatsVO;
import com.notaskflow.domain.vo.RoleCompletionVO;
import com.notaskflow.domain.vo.StatsActivityVO;
import com.notaskflow.domain.vo.TaskTrendVO;
import com.notaskflow.exception.BusinessException;
import com.notaskflow.exception.ErrorCode;
import com.notaskflow.mapper.NoteMapper;
import com.notaskflow.mapper.RoleMapper;
import com.notaskflow.mapper.SpaceMapper;
import com.notaskflow.mapper.SpaceMemberMapper;
import com.notaskflow.mapper.TaskMapper;
import com.notaskflow.mapper.TaskMemberMapper;
import com.notaskflow.mapper.UserMapper;
import com.notaskflow.security.PermissionValidator;
import com.notaskflow.service.StatsService;
import com.notaskflow.utils.LoginUserUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 统计服务实现，提供个人和空间维度的任务统计数据。
 *
 * @author LIN
 */
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final SpaceMapper spaceMapper;

    private final SpaceMemberMapper spaceMemberMapper;

    private final NoteMapper noteMapper;

    private final TaskMapper taskMapper;

    private final TaskMemberMapper taskMemberMapper;

    private final UserMapper userMapper;

    private final RoleMapper roleMapper;

    private final PermissionValidator permissionValidator;

    /**
     * 查询当前用户的个人统计数据。
     *
     * @return 个人统计数据
     */
    @Override
    public PersonalStatsVO personal() {
        Long currentUserId = LoginUserUtil.currentUserId();
        Space personalSpace = spaceMapper.selectOne(Wrappers.<Space>lambdaQuery()
                .eq(Space::getOwnerUserId, currentUserId)
                .eq(Space::getType, SpaceType.PERSONAL));
        if (personalSpace == null) {
            return new PersonalStatsVO(0L, 0L, 0L);
        }
        Long noteCount = noteMapper.selectCount(Wrappers.<Note>lambdaQuery()
                .eq(Note::getSpaceId, personalSpace.getId()));
        List<Long> personalTaskIds = findTaskIdsBySpace(personalSpace.getId());
        Long unfinishedCount = countUnfinishedMembers(currentUserId, personalTaskIds);
        LocalDateTime monthStart = YearMonth.now().atDay(1).atStartOfDay();
        Long completedTaskCount = taskMapper.selectCount(Wrappers.<Task>lambdaQuery()
                .eq(Task::getSpaceId, personalSpace.getId())
                .eq(Task::getStatus, TaskStatus.COMPLETED)
                .ge(Task::getCompletedAt, monthStart));
        return new PersonalStatsVO(noteCount, unfinishedCount, completedTaskCount);
    }

    /**
     * 查询个人笔记创建与编辑趋势。
     *
     * @param days 天数
     * @return 笔记趋势
     */
    @Override
    public List<PersonalNoteTrendVO> personalNoteTrend(Integer days) {
        int safeDays = safeDays(days);
        Space personalSpace = findCurrentPersonalSpace();
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(safeDays - 1L);
        if (personalSpace == null) {
            return buildEmptyPersonalNoteTrend(startDate, safeDays);
        }
        List<Note> notes = noteMapper.selectList(Wrappers.<Note>lambdaQuery()
                .eq(Note::getSpaceId, personalSpace.getId())
                .ge(Note::getGmtModified, startDate.atStartOfDay()));
        Map<LocalDate, Long> createdMap = notes.stream()
                .filter(note -> note.getGmtCreate() != null)
                .filter(note -> !note.getGmtCreate().toLocalDate().isBefore(startDate))
                .collect(Collectors.groupingBy(note -> note.getGmtCreate().toLocalDate(), LinkedHashMap::new,
                        Collectors.counting()));
        Map<LocalDate, Long> updatedMap = notes.stream()
                .filter(note -> note.getGmtModified() != null)
                .filter(note -> !note.getGmtModified().toLocalDate().isBefore(startDate))
                .collect(Collectors.groupingBy(note -> note.getGmtModified().toLocalDate(), LinkedHashMap::new,
                        Collectors.counting()));
        List<PersonalNoteTrendVO> result = new ArrayList<>();
        for (int offset = 0; offset < safeDays; offset++) {
            LocalDate date = startDate.plusDays(offset);
            result.add(new PersonalNoteTrendVO(date, createdMap.getOrDefault(date, 0L),
                    updatedMap.getOrDefault(date, 0L)));
        }
        return result;
    }

    /**
     * 查询空间成员的未完成任务负载。
     *
     * @param spaceId 空间标识
     * @return 成员任务负载列表
     */
    @Override
    public List<MemberTaskLoadVO> load(Long spaceId) {
        permissionValidator.ensureSpaceMember(spaceId, LoginUserUtil.currentUserId());
        List<SpaceMember> members = spaceMemberMapper.selectList(Wrappers.<SpaceMember>lambdaQuery()
                .eq(SpaceMember::getSpaceId, spaceId));
        List<Long> taskIds = findTaskIdsBySpace(spaceId);
        Map<Long, Long> loadMap = new LinkedHashMap<>();
        Map<Long, Long> completedMap = new LinkedHashMap<>();
        if (!taskIds.isEmpty()) {
            taskMemberMapper.selectList(Wrappers.<TaskMember>lambdaQuery()
                            .in(TaskMember::getTaskId, taskIds))
                    .forEach(member -> {
                        if (TaskMemberStatus.COMPLETED.equals(member.getStatus())) {
                            completedMap.merge(member.getUserId(), 1L, Long::sum);
                            return;
                        }
                        loadMap.merge(member.getUserId(), 1L, Long::sum);
                    });
        }
        return members.stream()
                .map(member -> new MemberTaskLoadVO(member.getUserId(), username(member.getUserId()),
                        loadMap.getOrDefault(member.getUserId(), 0L),
                        completedMap.getOrDefault(member.getUserId(), 0L)))
                .toList();
    }

    /**
     * 查询空间任务完成趋势。
     *
     * @param spaceId 空间标识
     * @param days 统计天数
     * @return 任务趋势列表
     */
    @Override
    public List<TaskTrendVO> trend(Long spaceId, Integer days) {
        permissionValidator.ensureSpaceMember(spaceId, LoginUserUtil.currentUserId());
        int safeDays = safeDays(days);
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(safeDays - 1L);
        List<Task> tasks = taskMapper.selectList(Wrappers.<Task>lambdaQuery()
                .eq(Task::getSpaceId, spaceId));
        Map<LocalDate, Long> createdMap = tasks.stream()
                .filter(task -> task.getGmtCreate() != null)
                .filter(task -> !task.getGmtCreate().toLocalDate().isBefore(startDate))
                .collect(Collectors.groupingBy(task -> task.getGmtCreate().toLocalDate(), LinkedHashMap::new,
                        Collectors.counting()));
        Map<LocalDate, Long> completedMap = tasks.stream()
                .map(task -> resolveCompletedDate(task, startDate))
                .filter(date -> date != null && !date.isBefore(startDate))
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        List<TaskTrendVO> result = new ArrayList<>();
        for (int offset = 0; offset < safeDays; offset++) {
            LocalDate date = startDate.plusDays(offset);
            result.add(new TaskTrendVO(date, createdMap.getOrDefault(date, 0L),
                    completedMap.getOrDefault(date, 0L)));
        }
        return result;
    }

    /**
     * 查询空间内不同角色的任务完成数量。
     *
     * @param spaceId 空间标识
     * @return 角色完成统计列表
     */
    @Override
    public List<RoleCompletionVO> roleCompletion(Long spaceId) {
        permissionValidator.ensureSpaceMember(spaceId, LoginUserUtil.currentUserId());
        List<SpaceMember> members = spaceMemberMapper.selectList(Wrappers.<SpaceMember>lambdaQuery()
                .eq(SpaceMember::getSpaceId, spaceId));
        Map<Long, SpaceMember> memberMap = members.stream()
                .collect(Collectors.toMap(SpaceMember::getUserId, Function.identity(), (first, second) -> first));
        Map<Long, Long> roleCountMap = new LinkedHashMap<>();
        List<Long> taskIds = findTaskIdsBySpace(spaceId);
        if (!taskIds.isEmpty()) {
            taskMemberMapper.selectList(Wrappers.<TaskMember>lambdaQuery()
                            .in(TaskMember::getTaskId, taskIds)
                            .eq(TaskMember::getStatus, TaskMemberStatus.COMPLETED))
                    .forEach(taskMember -> {
                        SpaceMember member = memberMap.get(taskMember.getUserId());
                        if (member != null) {
                            roleCountMap.merge(member.getRoleId(), 1L, Long::sum);
                        }
                    });
        }
        return members.stream()
                .map(SpaceMember::getRoleId)
                .distinct()
                .map(roleId -> toRoleCompletionVO(roleId, roleCountMap.getOrDefault(roleId, 0L)))
                .toList();
    }

    /**
     * 查询空间近期任务动态。
     *
     * @param spaceId 空间标识
     * @param limit 数量限制
     * @return 近期动态列表
     */
    @Override
    public List<StatsActivityVO> recentActivities(Long spaceId, Integer limit) {
        permissionValidator.ensureSpaceMember(spaceId, LoginUserUtil.currentUserId());
        List<Long> taskIds = findTaskIdsBySpace(spaceId);
        if (taskIds.isEmpty()) {
            return Collections.emptyList();
        }
        Page<TaskMember> page = new Page<>(1, safeLimit(limit), false);
        return taskMemberMapper.selectPage(page, Wrappers.<TaskMember>lambdaQuery()
                        .in(TaskMember::getTaskId, taskIds)
                        .orderByDesc(TaskMember::getGmtModified))
                .getRecords()
                .stream()
                .map(this::toStatsActivityVO)
                .toList();
    }

    /**
     * 查询空间任务标识列表。
     *
     * @param spaceId 空间标识
     * @return 任务标识列表
     */
    private List<Long> findTaskIdsBySpace(Long spaceId) {
        List<Long> taskIds = taskMapper.selectList(Wrappers.<Task>lambdaQuery().eq(Task::getSpaceId, spaceId))
                .stream()
                .map(Task::getId)
                .toList();
        return taskIds.isEmpty() ? Collections.emptyList() : taskIds;
    }

    /**
     * 统计未完成的任务成员数量。
     *
     * @param userId 用户标识
     * @param taskIds 任务标识列表
     * @return 未完成数量
     */
    private Long countUnfinishedMembers(Long userId, List<Long> taskIds) {
        if (taskIds.isEmpty()) {
            return 0L;
        }
        return taskMemberMapper.selectCount(Wrappers.<TaskMember>lambdaQuery()
                .in(TaskMember::getTaskId, taskIds)
                .eq(TaskMember::getUserId, userId)
                .ne(TaskMember::getStatus, TaskMemberStatus.COMPLETED));
    }

    /**
     * 解析任务完成日期，兼容历史数据中 completedAt 为空但状态已完成的记录。
     *
     * @param task 任务实体
     * @param startDate 统计开始日期
     * @return 完成日期
     */
    private LocalDate resolveCompletedDate(Task task, LocalDate startDate) {
        if (task.getCompletedAt() != null) {
            return task.getCompletedAt().toLocalDate();
        }
        if (!TaskStatus.COMPLETED.equals(task.getStatus()) || task.getGmtModified() == null) {
            return null;
        }
        LocalDate modifiedDate = task.getGmtModified().toLocalDate();
        return modifiedDate.isBefore(startDate) ? null : modifiedDate;
    }

    /**
     * 查询当前用户的个人空间。
     *
     * @return 个人空间实体
     */
    private Space findCurrentPersonalSpace() {
        Long currentUserId = LoginUserUtil.currentUserId();
        return spaceMapper.selectOne(Wrappers.<Space>lambdaQuery()
                .eq(Space::getOwnerUserId, currentUserId)
                .eq(Space::getType, SpaceType.PERSONAL));
    }

    /**
     * 构造空的个人笔记趋势数据。
     *
     * @param startDate 开始日期
     * @param days 天数
     * @return 趋势列表
     */
    private List<PersonalNoteTrendVO> buildEmptyPersonalNoteTrend(LocalDate startDate, int days) {
        List<PersonalNoteTrendVO> result = new ArrayList<>();
        for (int offset = 0; offset < days; offset++) {
            result.add(new PersonalNoteTrendVO(startDate.plusDays(offset), 0L, 0L));
        }
        return result;
    }

    /**
     * 获取用户名。
     *
     * @param userId 用户标识
     * @return 用户名
     */
    private String username(Long userId) {
        User user = userMapper.selectById(userId);
        return user == null ? "" : user.getUsername();
    }

    /**
     * 转换角色完成统计视图。
     *
     * @param roleId 角色标识
     * @param completedCount 完成数量
     * @return 角色完成统计
     */
    private RoleCompletionVO toRoleCompletionVO(Long roleId, Long completedCount) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            return new RoleCompletionVO(roleId, "", "", completedCount);
        }
        return new RoleCompletionVO(role.getId(), role.getCode(), role.getName(), completedCount);
    }

    /**
     * 校验统计天数。
     *
     * @param days 天数
     * @return 安全天数
     */
    private int safeDays(Integer days) {
        if (days == null) {
            return 7;
        }
        if (days < 1 || days > 30) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "统计天数必须在1到30之间");
        }
        return days;
    }

    /**
     * 校验动态数量限制。
     *
     * @param limit 数量限制
     * @return 安全数量
     */
    private int safeLimit(Integer limit) {
        if (limit == null) {
            return 10;
        }
        if (limit < 1 || limit > 50) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "动态数量必须在1到50之间");
        }
        return limit;
    }

    /**
     * 转换近期动态视图。
     *
     * @param member 任务成员工作项
     * @return 近期动态
     */
    private StatsActivityVO toStatsActivityVO(TaskMember member) {
        Task task = taskMapper.selectById(member.getTaskId());
        String taskTitle = task == null ? "未知任务" : task.getTitle();
        String type = TaskMemberStatus.COMPLETED.equals(member.getStatus()) ? "完成任务" : "更新任务";
        String impact = TaskMemberStatus.COMPLETED.equals(member.getStatus()) ? "+1 完成职责" : "状态更新";
        String content = type + "「" + taskTitle + " - " + member.getResponsibility() + "」";
        return new StatsActivityVO(member.getGmtModified(), member.getUserId(), username(member.getUserId()), type,
                content, impact);
    }
}
