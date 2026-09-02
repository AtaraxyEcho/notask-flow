package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 协作文档正文保存请求。
 *
 * @author LIN
 */
@Data
public class CollabContentSaveRequest {

    @NotNull(message = "正文内容不能为空")
    private String content;

    private String contentHtml;
}

