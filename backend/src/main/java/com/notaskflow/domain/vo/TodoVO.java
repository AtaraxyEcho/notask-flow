package com.notaskflow.domain.vo;

import com.notaskflow.common.enums.TaskMemberStatus;
import com.notaskflow.common.enums.TaskMode;
import com.notaskflow.common.enums.TaskStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 待办视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TodoVO {

    private Long id;

    private Long spaceId;

    private Long userId;

    private Long taskMemberId;

    private Long taskId;

    private TaskMode taskMode;

    private TaskStatus taskStatus;

    private TaskMemberStatus taskMemberStatus;

    private String title;

    private Boolean isCompleted;

    private LocalDateTime deadline;

    private LocalDateTime completedAt;

    private LocalDateTime gmtCreate;
}
