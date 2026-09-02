package com.notaskflow.domain.dto.request;

import com.notaskflow.common.enums.TaskMode;
import com.notaskflow.common.enums.TaskPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 任务创建请求。
 *
 * @author LIN
 */
@Data
public class TaskCreateRequest {

    @NotBlank(message = "任务标题不能为空")
    @Size(max = 200, message = "任务标题长度不能超过200")
    private String title;

    private String description;

    @NotNull(message = "任务模式不能为空")
    private TaskMode mode;

    private TaskPriority priority = TaskPriority.MEDIUM;

    private LocalDateTime deadline;

    private Long projectId;

    @Valid
    private List<TaskAssignmentRequest> assignments = new ArrayList<>();
}
