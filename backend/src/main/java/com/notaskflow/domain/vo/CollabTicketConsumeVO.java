package com.notaskflow.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 协作 Ticket 消费结果。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollabTicketConsumeVO {

    private Boolean valid;

    private Long spaceId;

    private Long noteId;

    private Long userId;

    private String username;

    private String nickname;

    private String avatarUrl;

    private Boolean canEdit;

    private String content;
}

