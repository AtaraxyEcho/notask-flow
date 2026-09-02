package com.notaskflow.domain.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端数据大盘视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardVO {

    private Long totalUsers;

    private Long totalTeamSpaces;

    private Long totalNotes;

    private Long totalTasks;

    private Long totalTodos;

    private Long totalFiles;

    private Long totalStorageBytes;

    private Long todayNewUsers;

    private Long todayNewNotes;

    private Long todayNewTasks;

    private Long todayNewTodos;

    private Long todayNewTeamSpaces;

    private List<AdminDashboardTrendPointVO> trends;
}
