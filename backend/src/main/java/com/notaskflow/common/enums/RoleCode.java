package com.notaskflow.common.enums;

import lombok.Getter;

/**
 * 系统预置角色编码。
 *
 * @author LIN
 */
@Getter
public enum RoleCode {

    SPACE_OWNER("SPACE_OWNER"),
    SPACE_ADMIN("SPACE_ADMIN"),
    SPACE_MEMBER("SPACE_MEMBER"),
    SPACE_GUEST("SPACE_GUEST");

    private final String code;

    RoleCode(String code) {
        this.code = code;
    }
}
