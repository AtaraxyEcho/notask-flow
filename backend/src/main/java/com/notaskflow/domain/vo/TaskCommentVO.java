package com.notaskflow.domain.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务评论视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskCommentVO {

    private Long id;

    private Long taskId;

    private Long userId;

    private String username;

    private Long parentCommentId;

    private String content;

    private LocalDateTime gmtCreate;

    private List<Long> mentionUserIds = new ArrayList<>();
}
