package com.notaskflow.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端用户统计视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserStatsVO {

    private Long totalUsers;

    private Long todayNewUsers;

    private Long disabledUsers;

    private Long onlineUsers;
}
