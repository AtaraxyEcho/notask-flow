package com.notaskflow.domain.vo;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务完成趋势视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskTrendVO {

    private LocalDate date;

    private Long createdCount;

    private Long completedCount;
}
