package com.notaskflow.domain.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件管理条目视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagedFileVO {

    private Long id;

    private Long attachmentId;

    private Long spaceId;

    private Long folderId;

    private String displayName;

    private String fileName;

    private Long fileSize;

    private String mimeType;

    private Long uploaderId;

    private Long createdBy;

    private Boolean trashed;

    private LocalDateTime deletedAt;

    private String downloadUrl;

    private LocalDateTime gmtCreate;
}
