package com.notaskflow.utils;

import com.notaskflow.domain.entity.User;
import org.springframework.util.StringUtils;

/**
 * 用户头像代理地址工具。
 *
 * @author LIN
 */
public final class AvatarUrlUtil {

    private static final String AVATAR_PATH_PREFIX = "/api/v1/public/users/";

    private static final String AVATAR_PATH_SUFFIX = "/avatar";

    private AvatarUrlUtil() {
    }

    /**
     * 生成用户头像代理地址。
     *
     * @param user 用户实体
     * @return 头像代理地址
     */
    public static String proxyUrl(User user) {
        if (user == null || !StringUtils.hasText(user.getAvatarUrl())) {
            return null;
        }
        return AVATAR_PATH_PREFIX + user.getId() + AVATAR_PATH_SUFFIX + "?v=" + versionOf(user);
    }

    private static long versionOf(User user) {
        return Integer.toUnsignedLong(user.getAvatarUrl().hashCode());
    }
}
