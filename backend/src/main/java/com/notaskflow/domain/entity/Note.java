package com.notaskflow.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 笔记实体。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("nt_note")
public class Note extends BaseEntity {

    private Long spaceId;

    private Long notebookId;

    private Long projectId;

    private Long userId;

    private String title;

    private String content;

    private String contentHtml;

    @TableField("is_public")
    private Boolean isPublic;

    private String shareCode;

    private LocalDateTime shareExpire;

    private Integer viewCount;
}
