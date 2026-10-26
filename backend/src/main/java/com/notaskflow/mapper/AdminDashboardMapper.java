package com.notaskflow.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 管理端数据大盘聚合查询 Mapper。
 *
 * <p>趋势统计全部下推到数据库按日聚合，避免逐日逐表执行 {@code COUNT} 造成上百次 SQL 往返。</p>
 *
 * @author LIN
 */
@Mapper
public interface AdminDashboardMapper {

    /**
     * 按日统计新增用户数。
     *
     * @param start 起始时间
     * @return 每日数量
     */
    @Select("""
            SELECT DATE(gmt_create) AS day, COUNT(*) AS total
            FROM nt_user
            WHERE is_deleted = 0 AND gmt_create >= #{start}
            GROUP BY DATE(gmt_create)
            """)
    List<Map<String, Object>> countUsersByDay(@Param("start") LocalDateTime start);

    /**
     * 按日统计新增团队空间数。
     *
     * @param start 起始时间
     * @return 每日数量
     */
    @Select("""
            SELECT DATE(gmt_create) AS day, COUNT(*) AS total
            FROM nt_space
            WHERE is_deleted = 0 AND type = 'TEAM' AND gmt_create >= #{start}
            GROUP BY DATE(gmt_create)
            """)
    List<Map<String, Object>> countTeamSpacesByDay(@Param("start") LocalDateTime start);

    /**
     * 按日统计新增笔记数。
     *
     * @param start 起始时间
     * @return 每日数量
     */
    @Select("""
            SELECT DATE(gmt_create) AS day, COUNT(*) AS total
            FROM nt_note
            WHERE is_deleted = 0 AND gmt_create >= #{start}
            GROUP BY DATE(gmt_create)
            """)
    List<Map<String, Object>> countNotesByDay(@Param("start") LocalDateTime start);

    /**
     * 按日统计新增任务数。
     *
     * @param start 起始时间
     * @return 每日数量
     */
    @Select("""
            SELECT DATE(gmt_create) AS day, COUNT(*) AS total
            FROM nt_task
            WHERE is_deleted = 0 AND gmt_create >= #{start}
            GROUP BY DATE(gmt_create)
            """)
    List<Map<String, Object>> countTasksByDay(@Param("start") LocalDateTime start);

    /**
     * 按日统计新增待办数。
     *
     * @param start 起始时间
     * @return 每日数量
     */
    @Select("""
            SELECT DATE(gmt_create) AS day, COUNT(*) AS total
            FROM nt_todo
            WHERE is_deleted = 0 AND gmt_create >= #{start}
            GROUP BY DATE(gmt_create)
            """)
    List<Map<String, Object>> countTodosByDay(@Param("start") LocalDateTime start);

    /**
     * 按日统计上传文件占用字节数。
     *
     * @param start 起始时间
     * @return 每日字节数
     */
    @Select("""
            SELECT DATE(gmt_create) AS day, COALESCE(SUM(file_size), 0) AS total
            FROM nt_attachment
            WHERE is_deleted = 0 AND gmt_create >= #{start}
            GROUP BY DATE(gmt_create)
            """)
    List<Map<String, Object>> sumAttachmentBytesByDay(@Param("start") LocalDateTime start);
}
