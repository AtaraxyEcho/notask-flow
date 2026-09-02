package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 笔记本保存请求。
 *
 * @author LIN
 */
@Data
public class NotebookSaveRequest {

    private Long parentId = 0L;

    @NotBlank(message = "笔记本名称不能为空")
    @Size(max = 100, message = "笔记本名称长度不能超过100")
    private String name;

    private Integer sortOrder = 0;
}
