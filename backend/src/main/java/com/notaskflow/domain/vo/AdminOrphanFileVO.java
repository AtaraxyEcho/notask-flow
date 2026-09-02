package com.notaskflow.domain.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端孤立文件视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrphanFileVO {

    private Long id;

    private String fileName;

    private Long fileSize;

    private String mimeType;

    private Long uploaderId;

    private String uploaderName;

    private String uploaderEmail;

    private Long spaceId;

    private String spaceName;

    private LocalDateTime gmtCreate;
}
