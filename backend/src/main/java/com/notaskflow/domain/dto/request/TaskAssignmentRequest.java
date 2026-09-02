package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 任务职责分配请求。
 *
 * @author LIN
 */
@Data
public class TaskAssignmentRequest {

    @NotNull(message = "责任人ID不能为空")
    private Long userId;

    @NotBlank(message = "职责描述不能为空")
    @Size(max = 500, message = "职责描述长度不能超过500")
    private String responsibility;

    private Boolean isRequired = true;
}
