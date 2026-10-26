package com.notaskflow.utils;

import org.springframework.util.StringUtils;

/**
 * SQL 模糊查询关键字转义工具。
 *
 * <p>MyBatis-Plus 的 {@code like} 会自行拼接 {@code %keyword%}，若用户输入中包含
 * {@code %}、{@code _} 或 {@code \} 会被当作通配符，导致查询范围被恶意放大。
 * 所有用户输入在进入 {@code like} 前必须先转义。</p>
 *
 * @author LIN
 */
public final class SqlLikeUtils {

    private static final String ESCAPE_CHARACTER = "\\";

    private SqlLikeUtils() {
    }

    /**
     * 转义模糊查询关键字中的通配符。
     *
     * @param keyword 原始关键字
     * @return 转义后的关键字，空白输入返回原值
     */
    public static String escape(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return keyword;
        }
        String normalized = keyword.trim();
        StringBuilder builder = new StringBuilder(normalized.length() + 8);
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (current == '\\' || current == '%' || current == '_') {
                builder.append(ESCAPE_CHARACTER);
            }
            builder.append(current);
        }
        return builder.toString();
    }
}
