package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传配置更新请求。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadConfigUpdateRequest {

    @Min(value = 1, message = "最大文件大小必须大于0")
    private Long maxFileSize;

    @Min(value = 1, message = "分片上传阈值必须大于0")
    private Long multipartThresholdSize;

    @Min(value = 1, message = "分片大小必须大于0")
    private Long chunkSize;

    private List<String> allowedMimeTypes;
}
