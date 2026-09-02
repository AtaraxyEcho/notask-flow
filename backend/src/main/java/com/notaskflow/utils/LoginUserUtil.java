package com.notaskflow.utils;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 登录用户工具类。
 *
 * @author LIN
 */
public final class LoginUserUtil {

    private LoginUserUtil() {
    }

    /**
     * 获取当前登录用户标识。
     *
     * @return 当前登录用户标识
     */
    public static Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }
}
