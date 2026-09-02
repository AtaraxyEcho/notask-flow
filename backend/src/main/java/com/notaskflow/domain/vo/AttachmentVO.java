package com.notaskflow.domain.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 附件视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentVO {

    private Long id;

    private String fileName;

    private Long fileSize;

    private String mimeType;

    private Long uploaderId;

    private Long spaceId;

    private String downloadUrl;

    private LocalDateTime gmtCreate;
}
