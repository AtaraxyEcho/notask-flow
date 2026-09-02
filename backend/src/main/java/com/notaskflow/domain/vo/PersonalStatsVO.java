package com.notaskflow.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 个人统计视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalStatsVO {

    private Long noteCount;

    private Long unfinishedTaskMemberCount;

    private Long completedTaskCountThisMonth;
}
