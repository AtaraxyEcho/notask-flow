package com.notaskflow.domain.vo;

import com.notaskflow.common.enums.ClientType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端当前登录信息。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminMeVO {

    private String username;

    private ClientType clientType;

    private String sessionId;
}
