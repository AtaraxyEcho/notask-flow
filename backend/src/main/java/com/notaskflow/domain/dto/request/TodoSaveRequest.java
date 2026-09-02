package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 待办保存请求。
 *
 * @author LIN
 */
@Data
public class TodoSaveRequest {

    @NotBlank(message = "待办标题不能为空")
    @Size(max = 200, message = "待办标题长度不能超过200")
    private String title;

    private LocalDateTime deadline;
}
