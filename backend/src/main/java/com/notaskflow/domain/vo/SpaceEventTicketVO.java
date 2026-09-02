package com.notaskflow.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 空间实时事件 Ticket 视图对象。
 *
 * @author LIN
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceEventTicketVO {

    private String ticket;

    private Integer expiresIn;
}
