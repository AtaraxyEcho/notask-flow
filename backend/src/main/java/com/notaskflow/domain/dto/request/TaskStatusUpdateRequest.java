package com.notaskflow.domain.dto.request;

import com.notaskflow.common.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 任务状态更新请求。
 *
 * @author LIN
 */
@Data
public class TaskStatusUpdateRequest {

    @NotNull(message = "目标状态不能为空")
    private TaskStatus status;
}
