package com.notaskflow.domain.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传配置视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadConfigVO {

    private Long maxFileSize;

    private Long multipartThresholdSize;

    private Long chunkSize;

    private List<String> allowedMimeTypes;
}
