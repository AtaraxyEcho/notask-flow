package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 任务评论创建请求。
 *
 * @author LIN
 */
@Data
public class TaskCommentCreateRequest {

    private Long parentCommentId;

    @NotBlank(message = "评论内容不能为空")
    private String content;

    private List<Long> mentionUserIds = new ArrayList<>();
}
