package com.notaskflow.controller;

import com.notaskflow.common.ApiResponse;
import com.notaskflow.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论控制器。
 *
 * @author LIN
 */
@Tag(name = "任务评论")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/comments")
public class CommentController {

    private final TaskService taskService;

    /**
     * 删除评论。
     *
     * @param id 评论标识
     * @return 空响应
     */
    @Operation(summary = "删除评论")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteComment(@PathVariable Long id) {
        taskService.deleteComment(id);
        return ApiResponse.success();
    }
}
