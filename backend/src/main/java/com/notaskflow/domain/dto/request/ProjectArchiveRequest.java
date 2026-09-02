package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 项目归档请求。
 *
 * @author LIN
 */
@Data
public class ProjectArchiveRequest {

    @NotNull(message = "归档状态不能为空")
    private Boolean archived;
}
