package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 任务成员完成提交请求。
 *
 * @author LIN
 */
@Data
public class TaskMemberCompleteRequest {

    @Size(max = 10000, message = "完成说明长度不能超过10000")
    private String completionRemark;
}
