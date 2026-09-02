package com.notaskflow.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.notaskflow.common.enums.TaskMode;
import com.notaskflow.common.enums.TaskPriority;
import com.notaskflow.common.enums.TaskStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 任务实体。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("nt_task")
public class Task extends BaseEntity {

    private Long spaceId;

    private Long projectId;

    private String title;

    private String description;

    private Long creatorId;

    private TaskMode mode;

    private TaskStatus status;

    private TaskPriority priority;

    private LocalDateTime deadline;

    private LocalDateTime completedAt;
}
