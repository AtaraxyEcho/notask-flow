package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文件夹保存请求。
 *
 * @author LIN
 */
@Data
public class FileFolderSaveRequest {

    private Long parentId;

    @NotBlank(message = "文件夹名称不能为空")
    @Size(max = 80, message = "文件夹名称不能超过80个字符")
    private String name;
}
