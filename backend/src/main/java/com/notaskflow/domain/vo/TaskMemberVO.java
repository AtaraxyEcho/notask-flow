package com.notaskflow.domain.vo;

import com.notaskflow.common.enums.AssignmentType;
import com.notaskflow.common.enums.TaskMemberStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务成员工作项视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskMemberVO {

    private Long id;

    private Long taskId;

    private Long userId;

    private String username;

    private String responsibility;

    private AssignmentType assignmentType;

    private TaskMemberStatus status;

    private Boolean isRequired;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private String completionRemark;

    private Integer version;
}
