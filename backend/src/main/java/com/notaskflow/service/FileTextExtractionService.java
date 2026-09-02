package com.notaskflow.service;

import com.notaskflow.domain.entity.Attachment;

/**
 * 文件文本内容提取服务。
 *
 * @author LIN
 */
public interface FileTextExtractionService {

    /**
     * 从附件对象中提取可搜索文本。
     *
     * @param attachment 附件元数据
     * @return 可搜索文本，无法提取时返回空字符串
     */
    String extract(Attachment attachment);

    /**
     * 从附件对象中提取 HTML 预览内容。
     *
     * @param attachment 附件元数据
     * @return HTML 预览内容，无法提取时返回空字符串
     */
    String extractHtml(Attachment attachment);
}
