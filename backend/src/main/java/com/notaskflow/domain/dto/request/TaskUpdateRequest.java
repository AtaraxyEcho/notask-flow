package com.notaskflow.domain.dto.request;

import com.notaskflow.common.enums.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 任务基础信息更新请求。
 *
 * @author LIN
 */
@Data
public class TaskUpdateRequest {

    @NotBlank(message = "任务标题不能为空")
    @Size(max = 200, message = "任务标题长度不能超过200")
    private String title;

    private String description;

    private TaskPriority priority = TaskPriority.MEDIUM;

    private LocalDateTime deadline;

    private Long projectId;
}
