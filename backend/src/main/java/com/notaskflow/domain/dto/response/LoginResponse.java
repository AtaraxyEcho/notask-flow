package com.notaskflow.domain.dto.response;

import com.notaskflow.common.enums.ClientType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private Long userId;

    private String tokenName;

    private String tokenValue;

    private Long expireTime;

    private String sessionId;

    private ClientType clientType;

    private String refreshToken;

    public LoginResponse(Long userId, String tokenName, String tokenValue, Long expireTime) {
        this.userId = userId;
        this.tokenName = tokenName;
        this.tokenValue = tokenValue;
        this.expireTime = expireTime;
    }
}
