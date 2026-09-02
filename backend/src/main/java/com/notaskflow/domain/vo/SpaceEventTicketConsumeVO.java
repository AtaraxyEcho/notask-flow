package com.notaskflow.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 空间实时事件 Ticket 消费结果。
 *
 * @author LIN
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceEventTicketConsumeVO {

    private Boolean valid;

    private Long spaceId;

    private Long userId;

    private String nickname;

    private String avatarUrl;
}
