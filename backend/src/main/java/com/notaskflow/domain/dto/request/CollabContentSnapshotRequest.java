package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 协作内容快照请求，供协同服务在房间销毁前兜底落盘。
 *
 * @author LIN
 */
@Data
public class CollabContentSnapshotRequest {

    /** 纯文本正文。 */
    @Size(max = 2000000, message = "正文长度超出限制")
    private String content;

    /** 富文本 HTML 内容。 */
    @Size(max = 4000000, message = "富文本长度超出限制")
    private String contentHtml;
}
