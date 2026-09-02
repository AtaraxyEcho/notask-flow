package com.notaskflow.controller;

import com.notaskflow.common.ApiResponse;
import com.notaskflow.config.CollaborationProperties;
import com.notaskflow.domain.dto.request.CollabTicketConsumeRequest;
import com.notaskflow.domain.dto.request.SpaceEventTicketConsumeRequest;
import com.notaskflow.domain.vo.CollabTicketConsumeVO;
import com.notaskflow.domain.vo.SpaceEventTicketConsumeVO;
import com.notaskflow.exception.AccessDeniedException;
import com.notaskflow.service.NoteService;
import com.notaskflow.service.SpaceService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 协作内部控制器。
 *
 * @author LIN
 */
@Hidden
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/collab")
public class CollaborationInternalController {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final NoteService noteService;

    private final SpaceService spaceService;

    private final CollaborationProperties collaborationProperties;

    /**
     * 消费一次性协作 Ticket。
     *
     * @param internalToken 内部访问令牌
     * @param request Ticket 消费请求
     * @return Ticket 消费结果
     */
    @PostMapping("/tickets/consume")
    public ApiResponse<CollabTicketConsumeVO> consumeTicket(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String internalToken,
            @Valid @RequestBody CollabTicketConsumeRequest request) {
        ensureInternalToken(internalToken);
        return ApiResponse.success(noteService.consumeCollabTicket(request.getTicket()));
    }

    /**
     * 消费空间实时事件 Ticket。
     *
     * @param internalToken 内部访问令牌
     * @param request Ticket 消费请求
     * @return Ticket 消费结果
     */
    @PostMapping("/space-events/tickets/consume")
    public ApiResponse<SpaceEventTicketConsumeVO> consumeSpaceEventTicket(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String internalToken,
            @Valid @RequestBody SpaceEventTicketConsumeRequest request) {
        ensureInternalToken(internalToken);
        return ApiResponse.success(spaceService.consumeSpaceEventTicket(request.getTicket()));
    }

    /**
     * 校验内部访问令牌。
     *
     * @param internalToken 请求头中的令牌
     */
    private void ensureInternalToken(String internalToken) {
        if (!StringUtils.hasText(internalToken)
                || !internalToken.equals(collaborationProperties.getInternalToken())) {
            throw new AccessDeniedException("协作内部调用令牌无效");
        }
    }
}
