package com.notaskflow.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.notaskflow.common.enums.AssignmentType;
import com.notaskflow.common.enums.TaskMemberStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 任务成员工作项实体。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("nt_task_member")
public class TaskMember extends BaseEntity {

    private Long taskId;

    private Long userId;

    private String responsibility;

    private AssignmentType assignmentType;

    private TaskMemberStatus status;

    @TableField("is_required")
    private Boolean isRequired;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private String completionRemark;

    @Version
    private Integer version;
}
