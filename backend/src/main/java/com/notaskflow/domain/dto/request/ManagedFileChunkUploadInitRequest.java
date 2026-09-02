package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件分片上传初始化请求。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagedFileChunkUploadInitRequest {

    private Long folderId;

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotNull(message = "文件大小不能为空")
    @Positive(message = "文件大小必须大于0")
    private Long fileSize;

    @NotBlank(message = "文件类型不能为空")
    private String mimeType;
}
