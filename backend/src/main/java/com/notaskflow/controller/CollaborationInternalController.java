package com.notaskflow.controller;

import com.notaskflow.common.ApiResponse;
import com.notaskflow.config.CollaborationProperties;
import com.notaskflow.domain.dto.request.CollabContentSnapshotRequest;
import com.notaskflow.domain.dto.request.CollabTicketConsumeRequest;
import com.notaskflow.domain.dto.request.SpaceEventTicketConsumeRequest;
import com.notaskflow.domain.vo.CollabTicketConsumeVO;
import com.notaskflow.domain.vo.SpaceEventTicketConsumeVO;
import com.notaskflow.exception.AccessDeniedException;
import com.notaskflow.service.NoteService;
import com.notaskflow.service.SpaceService;
import com.notaskflow.utils.ConstantTimeUtils;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
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
     * 兜底保存协同文档内容快照。
     *
     * <p>由协同服务在房间销毁前调用，用于客户端崩溃后的数据恢复。</p>
     *
     * @param internalToken 内部访问令牌
     * @param noteId 笔记标识
     * @param request 内容快照请求
     * @return 保存结果
     */
    @PostMapping("/notes/{noteId}/content")
    public ApiResponse<Void> saveNoteContentSnapshot(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String internalToken,
            @PathVariable Long noteId,
            @Valid @RequestBody CollabContentSnapshotRequest request) {
        ensureInternalToken(internalToken);
        noteService.saveCollabContentSnapshot(noteId, request.getContent(), request.getContentHtml());
        return ApiResponse.success();
    }

    /**
     * 校验内部访问令牌。
     *
     * @param internalToken 请求头中的令牌
     */
    private void ensureInternalToken(String internalToken) {
        if (!StringUtils.hasText(internalToken)
                || !ConstantTimeUtils.equals(collaborationProperties.getInternalToken(), internalToken)) {
            throw new AccessDeniedException("协作内部调用令牌无效");
        }
    }
}
