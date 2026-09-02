package com.notaskflow.controller;

import com.notaskflow.common.ApiResponse;
import com.notaskflow.common.PageResponse;
import com.notaskflow.config.CollaborationProperties;
import com.notaskflow.domain.query.EventFailLogQuery;
import com.notaskflow.domain.vo.EventFailLogVO;
import com.notaskflow.exception.AccessDeniedException;
import com.notaskflow.service.EventFailLogService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 失败事件内部管理控制器。
 *
 * @author LIN
 */
@Hidden
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/event-fail-logs")
public class EventFailLogInternalController {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final EventFailLogService eventFailLogService;

    private final CollaborationProperties collaborationProperties;

    /**
     * 分页查询失败事件日志。
     *
     * @param internalToken 内部访问令牌
     * @param query 查询条件
     * @return 失败事件日志分页
     */
    @GetMapping
    public ApiResponse<PageResponse<EventFailLogVO>> page(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String internalToken,
            @Valid @ModelAttribute EventFailLogQuery query) {
        ensureInternalToken(internalToken);
        return ApiResponse.success(eventFailLogService.page(query));
    }

    /**
     * 查询失败事件日志详情。
     *
     * @param internalToken 内部访问令牌
     * @param id 失败事件日志标识
     * @return 失败事件日志
     */
    @GetMapping("/{id}")
    public ApiResponse<EventFailLogVO> get(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String internalToken,
            @PathVariable Long id) {
        ensureInternalToken(internalToken);
        return ApiResponse.success(eventFailLogService.get(id));
    }

    /**
     * 手动重试单条失败事件。
     *
     * @param internalToken 内部访问令牌
     * @param id 失败事件日志标识
     * @param maxRetryCount 最大重试次数
     * @return 重试后的失败事件日志
     */
    @PostMapping("/{id}/retry")
    public ApiResponse<EventFailLogVO> retry(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String internalToken,
            @PathVariable Long id,
            @RequestParam(defaultValue = "5") int maxRetryCount) {
        ensureInternalToken(internalToken);
        return ApiResponse.success(eventFailLogService.retryFailure(id, maxRetryCount));
    }

    /**
     * 批量重试待补偿失败事件。
     *
     * @param internalToken 内部访问令牌
     * @param batchSize 批处理数量
     * @param maxRetryCount 最大重试次数
     * @return 成功重新投递数量
     */
    @PostMapping("/retry-pending")
    public ApiResponse<Integer> retryPending(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String internalToken,
            @RequestParam(defaultValue = "20") int batchSize,
            @RequestParam(defaultValue = "5") int maxRetryCount) {
        ensureInternalToken(internalToken);
        return ApiResponse.success(eventFailLogService.retryPendingFailures(batchSize, maxRetryCount));
    }

    private void ensureInternalToken(String internalToken) {
        if (!StringUtils.hasText(internalToken)
                || !internalToken.equals(collaborationProperties.getInternalToken())) {
            throw new AccessDeniedException("事件失败管理内部调用令牌无效");
        }
    }
}
