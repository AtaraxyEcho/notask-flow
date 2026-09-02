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
import java.util.List;
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
     * 构造近 30 天趋势。
     *
     * @param today 当前日期
     * @return 趋势点列表
     */
    private List<AdminDashboardTrendPointVO> buildTrends(LocalDate today) {
        LocalDate startDate = today.minusDays(TREND_DAYS - 1L);
        List<Attachment> recentAttachments = attachmentMapper.selectList(new LambdaQueryWrapper<Attachment>()
                .select(Attachment::getFileSize, Attachment::getGmtCreate)
                .ge(Attachment::getGmtCreate, startDate.atStartOfDay()));
        List<AdminDashboardTrendPointVO> trends = new ArrayList<>();
        for (int index = 0; index < TREND_DAYS; index++) {
            LocalDate date = startDate.plusDays(index);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1L).atStartOfDay();
            trends.add(new AdminDashboardTrendPointVO(
                    date.toString(),
                    countUsersBetween(start, end),
                    countNotesBetween(start, end),
                    countTasksBetween(start, end),
                    countTodosBetween(start, end),
                    countTeamSpacesBetween(start, end),
                    sumAttachmentBytesByDate(recentAttachments, date)
            ));
        }
        return trends;
    }

    /**
     * 统计指定日期的附件上传大小。
     *
     * @param attachments 附件列表
     * @param date 日期
     * @return 上传大小
     */
    private Long sumAttachmentBytesByDate(List<Attachment> attachments, LocalDate date) {
        return attachments.stream()
                .filter(attachment -> attachment.getGmtCreate() != null)
                .filter(attachment -> date.equals(attachment.getGmtCreate().toLocalDate()))
                .map(Attachment::getFileSize)
                .filter(fileSize -> fileSize != null)
                .reduce(0L, Long::sum);
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
        return attachmentMapper.selectList(new LambdaQueryWrapper<Attachment>().select(Attachment::getFileSize))
                .stream()
                .map(Attachment::getFileSize)
                .filter(fileSize -> fileSize != null)
                .reduce(0L, Long::sum);
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

    /**
     * 统计指定时间范围的用户数。
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 用户数
     */
    private Long countUsersBetween(LocalDateTime start, LocalDateTime end) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .ge(User::getGmtCreate, start)
                .lt(User::getGmtCreate, end));
    }

    /**
     * 统计指定时间范围的笔记数。
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 笔记数
     */
    private Long countNotesBetween(LocalDateTime start, LocalDateTime end) {
        return noteMapper.selectCount(new LambdaQueryWrapper<Note>()
                .ge(Note::getGmtCreate, start)
                .lt(Note::getGmtCreate, end));
    }

    /**
     * 统计指定时间范围的任务数。
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 任务数
     */
    private Long countTasksBetween(LocalDateTime start, LocalDateTime end) {
        return taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .ge(Task::getGmtCreate, start)
                .lt(Task::getGmtCreate, end));
    }

    /**
     * 统计指定时间范围的待办数。
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 待办数
     */
    private Long countTodosBetween(LocalDateTime start, LocalDateTime end) {
        return todoMapper.selectCount(new LambdaQueryWrapper<Todo>()
                .ge(Todo::getGmtCreate, start)
                .lt(Todo::getGmtCreate, end));
    }

    /**
     * 统计指定时间范围的团队空间数。
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 团队空间数
     */
    private Long countTeamSpacesBetween(LocalDateTime start, LocalDateTime end) {
        return spaceMapper.selectCount(new LambdaQueryWrapper<Space>()
                .eq(Space::getType, SpaceType.TEAM)
                .ge(Space::getGmtCreate, start)
                .lt(Space::getGmtCreate, end));
    }
}
