package com.notaskflow.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端数据趋势点。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardTrendPointVO {

    private String date;

    private Long newUsers;

    private Long newNotes;

    private Long newTasks;

    private Long newTodos;

    private Long newTeamSpaces;

    private Long uploadedBytes;
}
