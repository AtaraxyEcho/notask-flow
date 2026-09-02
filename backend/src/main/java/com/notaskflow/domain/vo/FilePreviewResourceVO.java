package com.notaskflow.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件预览资源视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilePreviewResourceVO {

    private String fileName;

    private String mimeType;

    private Long fileSize;
}
