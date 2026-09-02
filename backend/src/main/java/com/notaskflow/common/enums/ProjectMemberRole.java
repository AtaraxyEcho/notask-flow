package com.notaskflow.common.enums;

import java.util.Arrays;
import lombok.Getter;

/**
 * 项目成员角色枚举。
 *
 * @author LIN
 */
@Getter
public enum ProjectMemberRole {

    OWNER("OWNER", "项目负责人"),
    MEMBER("MEMBER", "项目成员");

    private final String code;

    private final String description;

    ProjectMemberRole(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据编码解析角色枚举。
     *
     * @param code 角色编码
     * @return 项目成员角色
     */
    public static ProjectMemberRole fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(MEMBER);
    }
}
