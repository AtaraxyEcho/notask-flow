package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.notaskflow.common.enums.SpaceType;
import com.notaskflow.domain.entity.Attachment;
import com.notaskflow.domain.entity.Note;
import com.notaskflow.domain.entity.Space;
import com.notaskflow.domain.entity.Task;
import com.notaskflow.domain.entity.Todo;
import com.notaskflow.domain.entity.User;
import com.notaskflow.domain.vo.AdminDashboardTrendPointVO;
import com.notaskflow.domain.vo.AdminDashboardVO;
import com.notaskflow.mapper.AdminDashboardMapper;
import com.notaskflow.mapper.AttachmentMapper;
import com.notaskflow.mapper.NoteMapper;
import com.notaskflow.mapper.SpaceMapper;
import com.notaskflow.mapper.TaskMapper;
import com.notaskflow.mapper.TodoMapper;
import com.notaskflow.mapper.UserMapper;
import com.notaskflow.service.AdminDashboardService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 管理端数据大盘服务实现。
 *
 * @author LIN
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final int TREND_DAYS = 30;

    private final UserMapper userMapper;

    private final SpaceMapper spaceMapper;

    private final NoteMapper noteMapper;

    private final TaskMapper taskMapper;

    private final TodoMapper todoMapper;

    private final AttachmentMapper attachmentMapper;

    private final AdminDashboardMapper adminDashboardMapper;

    /**
     * 查询管理端数据大盘。
     *
     * @return 数据大盘
     */
    @Override
    public AdminDashboardVO overview() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        return new AdminDashboardVO(
                countUsers(),
                countTeamSpaces(),
                countNotes(),
                countTasks(),
                countTodos(),
                countFiles(),
                sumStorageBytes(),
                countUsersSince(todayStart),
                countNotesSince(todayStart),
                countTasksSince(todayStart),
                countTodosSince(todayStart),
                countTeamSpacesSince(todayStart),
                buildTrends(today)
        );
    }

    /**
     * 构造近 30 天趋势，按日期分组一次性聚合各业务表。
     *
     * @param today 当前日期
     * @return 趋势点列表
     */
    private List<AdminDashboardTrendPointVO> buildTrends(LocalDate today) {
        LocalDate startDate = today.minusDays(TREND_DAYS - 1L);
        LocalDateTime start = startDate.atStartOfDay();
        Map<LocalDate, Long> userCounts = toDailyCounts(adminDashboardMapper.countUsersByDay(start));
        Map<LocalDate, Long> spaceCounts = toDailyCounts(adminDashboardMapper.countTeamSpacesByDay(start));
        Map<LocalDate, Long> noteCounts = toDailyCounts(adminDashboardMapper.countNotesByDay(start));
        Map<LocalDate, Long> taskCounts = toDailyCounts(adminDashboardMapper.countTasksByDay(start));
        Map<LocalDate, Long> todoCounts = toDailyCounts(adminDashboardMapper.countTodosByDay(start));
        Map<LocalDate, Long> attachmentBytes = toDailyCounts(adminDashboardMapper.sumAttachmentBytesByDay(start));
        List<AdminDashboardTrendPointVO> trends = new ArrayList<>(TREND_DAYS);
        for (int index = 0; index < TREND_DAYS; index++) {
            LocalDate date = startDate.plusDays(index);
            trends.add(new AdminDashboardTrendPointVO(
                    date.toString(),
                    userCounts.getOrDefault(date, 0L),
                    noteCounts.getOrDefault(date, 0L),
                    taskCounts.getOrDefault(date, 0L),
                    todoCounts.getOrDefault(date, 0L),
                    spaceCounts.getOrDefault(date, 0L),
                    attachmentBytes.getOrDefault(date, 0L)
            ));
        }
        return trends;
    }

    /**
     * 将按日聚合结果转换为日期映射。
     *
     * @param rows 聚合查询结果
     * @return 日期到数量的映射
     */
    private Map<LocalDate, Long> toDailyCounts(List<Map<String, Object>> rows) {
        Map<LocalDate, Long> result = new HashMap<>();
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            LocalDate day = resolveDay(row.get("day"));
            if (day == null) {
                continue;
            }
            result.put(day, resolveLong(row.get("total")));
        }
        return result;
    }

    /**
     * 解析聚合结果中的日期字段。
     *
     * @param value 原始值
     * @return 日期，无法解析时返回 null
     */
    private LocalDate resolveDay(Object value) {
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.util.Date date) {
            return new java.sql.Date(date.getTime()).toLocalDate();
        }
        if (value instanceof CharSequence text) {
            return LocalDate.parse(text.toString());
        }
        return null;
    }

    /**
     * 解析聚合结果中的计数字段。
     *
     * @param value 原始值
     * @return 计数值
     */
    private long resolveLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof CharSequence text) {
            try {
                return Long.parseLong(text.toString());
            } catch (NumberFormatException exception) {
                return 0L;
            }
        }
        return 0L;
    }

    /**
     * 统计总用户数。
     *
     * @return 总用户数
     */
    private Long countUsers() {
        return userMapper.selectCount(new LambdaQueryWrapper<User>());
    }

    /**
     * 统计团队空间数。
     *
     * @return 团队空间数
     */
    private Long countTeamSpaces() {
        return spaceMapper.selectCount(new LambdaQueryWrapper<Space>().eq(Space::getType, SpaceType.TEAM));
    }

    /**
     * 统计笔记总数。
     *
     * @return 笔记总数
     */
    private Long countNotes() {
        return noteMapper.selectCount(new LambdaQueryWrapper<Note>());
    }

    /**
     * 统计任务总数。
     *
     * @return 任务总数
     */
    private Long countTasks() {
        return taskMapper.selectCount(new LambdaQueryWrapper<Task>());
    }

    /**
     * 统计待办总数。
     *
     * @return 待办总数
     */
    private Long countTodos() {
        return todoMapper.selectCount(new LambdaQueryWrapper<Todo>());
    }

    /**
     * 统计文件总数。
     *
     * @return 文件总数
     */
    private Long countFiles() {
        return attachmentMapper.selectCount(new LambdaQueryWrapper<Attachment>());
    }

    /**
     * 统计存储占用字节数。
     *
     * @return 存储占用字节数
     */
    private Long sumStorageBytes() {
        Long total = attachmentMapper.sumActiveFileSize();
        return total == null ? 0L : total;
    }

    /**
     * 统计指定时间后的用户数。
     *
     * @param start 开始时间
     * @return 用户数
     */
    private Long countUsersSince(LocalDateTime start) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>().ge(User::getGmtCreate, start));
    }

    /**
     * 统计指定时间后的笔记数。
     *
     * @param start 开始时间
     * @return 笔记数
     */
    private Long countNotesSince(LocalDateTime start) {
        return noteMapper.selectCount(new LambdaQueryWrapper<Note>().ge(Note::getGmtCreate, start));
    }

    /**
     * 统计指定时间后的任务数。
     *
     * @param start 开始时间
     * @return 任务数
     */
    private Long countTasksSince(LocalDateTime start) {
        return taskMapper.selectCount(new LambdaQueryWrapper<Task>().ge(Task::getGmtCreate, start));
    }

    /**
     * 统计指定时间后的待办数。
     *
     * @param start 开始时间
     * @return 待办数
     */
    private Long countTodosSince(LocalDateTime start) {
        return todoMapper.selectCount(new LambdaQueryWrapper<Todo>().ge(Todo::getGmtCreate, start));
    }

    /**
     * 统计指定时间后的团队空间数。
     *
     * @param start 开始时间
     * @return 团队空间数
     */
    private Long countTeamSpacesSince(LocalDateTime start) {
        return spaceMapper.selectCount(new LambdaQueryWrapper<Space>()
                .eq(Space::getType, SpaceType.TEAM)
                .ge(Space::getGmtCreate, start));
    }
}
