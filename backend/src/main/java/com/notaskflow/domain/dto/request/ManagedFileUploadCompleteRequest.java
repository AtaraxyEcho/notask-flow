package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件直传完成登记请求。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagedFileUploadCompleteRequest {

    @NotBlank(message = "上传令牌不能为空")
    private String uploadToken;

    private String displayName;
}
