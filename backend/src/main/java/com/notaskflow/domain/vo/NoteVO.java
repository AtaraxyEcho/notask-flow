package com.notaskflow.domain.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 笔记视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteVO {

    private Long id;

    private Long spaceId;

    private Long notebookId;

    private Long projectId;

    private String projectName;

    private Long userId;

    private String title;

    private String content;

    private String contentHtml;

    private Boolean canEdit;

    private Boolean collabEnabled;

    private Boolean isPublic;

    private String shareCode;

    private LocalDateTime shareExpire;

    private Integer viewCount;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    private List<TagVO> tags = new ArrayList<>();
}
