package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 项目保存请求。
 *
 * @author LIN
 */
@Data
public class ProjectSaveRequest {

    @NotBlank(message = "项目名称不能为空")
    @Size(min = 2, max = 30, message = "项目名称长度必须在2到30之间")
    private String name;

    @Size(max = 1000, message = "项目描述长度不能超过1000")
    private String description;

    @Size(max = 20, message = "封面颜色长度不能超过20")
    private String coverColor;

    @Size(max = 255, message = "封面图片地址长度不能超过255")
    private String coverImageUrl;

    private Long ownerUserId;
}
