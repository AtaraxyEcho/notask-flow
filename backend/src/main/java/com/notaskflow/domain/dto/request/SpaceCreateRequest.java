package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 团队空间创建请求。
 *
 * @author LIN
 */
@Data
public class SpaceCreateRequest {

    @NotBlank(message = "空间名称不能为空")
    @Size(max = 100, message = "空间名称长度不能超过100")
    private String name;
}
