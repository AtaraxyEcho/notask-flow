package com.notaskflow.common.enums;

import com.notaskflow.exception.BusinessException;
import com.notaskflow.exception.ErrorCode;
import java.util.Locale;

/**
 * 笔记导出格式。
 *
 * @author LIN
 */
public enum NoteExportFormat {

    PDF("pdf", "application/pdf"),

    WORD("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),

    IMAGE("png", "image/png");

    private final String extension;

    private final String contentType;

    NoteExportFormat(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    /**
     * 获取文件扩展名。
     *
     * @return 文件扩展名
     */
    public String extension() {
        return extension;
    }

    /**
     * 获取响应内容类型。
     *
     * @return 内容类型
     */
    public String contentType() {
        return contentType;
    }

    /**
     * 解析导出格式。
     *
     * @param value 格式文本
     * @return 导出格式
     */
    public static NoteExportFormat from(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "导出格式不能为空");
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "pdf" -> PDF;
            case "word", "doc", "docx" -> WORD;
            case "image", "png" -> IMAGE;
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的导出格式");
        };
    }
}
