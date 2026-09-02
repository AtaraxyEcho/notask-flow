package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文件更新请求。
 *
 * @author LIN
 */
@Data
public class ManagedFileUpdateRequest {

    private Long folderId;

    @Size(max = 255, message = "文件名称不能超过255个字符")
    private String displayName;
}
