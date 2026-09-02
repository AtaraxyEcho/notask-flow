package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 空间实时事件 Ticket 消费请求。
 *
 * @author LIN
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceEventTicketConsumeRequest {

    @NotBlank(message = "空间实时事件 Ticket 不能为空")
    private String ticket;
}
