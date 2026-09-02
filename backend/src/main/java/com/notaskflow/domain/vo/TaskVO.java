package com.notaskflow.domain.vo;

import com.notaskflow.common.enums.TaskMode;
import com.notaskflow.common.enums.TaskPriority;
import com.notaskflow.common.enums.TaskStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskVO {

    private Long id;

    private Long spaceId;

    private Long projectId;

    private String projectName;

    private String title;

    private String description;

    private Long creatorId;

    private TaskMode mode;

    private TaskStatus status;

    private TaskPriority priority;

    private LocalDateTime deadline;

    private LocalDateTime completedAt;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    private List<TaskMemberVO> members = new ArrayList<>();
}
