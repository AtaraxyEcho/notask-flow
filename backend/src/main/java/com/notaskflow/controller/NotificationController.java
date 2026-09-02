package com.notaskflow.controller;

import com.notaskflow.common.ApiResponse;
import com.notaskflow.common.PageResponse;
import com.notaskflow.domain.query.NotificationQuery;
import com.notaskflow.domain.vo.NotificationVO;
import com.notaskflow.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知控制器。
 *
 * @author LIN
 */
@Validated
@Tag(name = "消息通知")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 分页查询通知。
     *
     * @param query 查询条件
     * @return 分页通知
     */
    @Operation(summary = "分页查询通知")
    @GetMapping
    public ApiResponse<PageResponse<NotificationVO>> page(@Valid @ModelAttribute NotificationQuery query) {
        return ApiResponse.success(notificationService.page(query));
    }

    /**
     * 查询未读数量。
     *
     * @return 未读数量
     */
    @Operation(summary = "查询未读通知数量")
    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount() {
        return ApiResponse.success(notificationService.unreadCount());
    }

    /**
     * 标记单条通知已读。
     *
     * @param id 通知标识
     * @return 通知详情
     */
    @Operation(summary = "标记通知已读")
    @PutMapping("/{id}/read")
    public ApiResponse<NotificationVO> markRead(@PathVariable Long id) {
        return ApiResponse.success(notificationService.markRead(id));
    }

    /**
     * 标记全部通知已读。
     *
     * @return 空响应
     */
    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public ApiResponse<Void> readAll() {
        notificationService.readAll();
        return ApiResponse.success();
    }

    /**
     * 清空全部已读通知。
     *
     * @return 空响应
     */
    @Operation(summary = "清空已读通知")
    @DeleteMapping("/read")
    public ApiResponse<Void> clearRead() {
        notificationService.clearRead();
        return ApiResponse.success();
    }

    /**
     * 删除通知。
     *
     * @param id 通知标识
     * @return 空响应
     */
    @Operation(summary = "删除通知")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        notificationService.delete(id);
        return ApiResponse.success();
    }
}
