package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 协作 Ticket 消费请求。
 *
 * @author LIN
 */
@Data
public class CollabTicketConsumeRequest {

    @NotBlank(message = "协作Ticket不能为空")
    private String ticket;
}

