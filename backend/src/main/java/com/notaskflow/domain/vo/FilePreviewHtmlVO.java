package com.notaskflow.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件 HTML 预览视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilePreviewHtmlVO {

    private String fileName;

    private String mimeType;

    private String htmlContent;
}
