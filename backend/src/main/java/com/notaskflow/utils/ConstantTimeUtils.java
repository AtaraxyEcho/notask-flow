package com.notaskflow.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 常量时间字符串比较工具，用于令牌、口令等敏感值的等值判断。
 *
 * <p>直接使用 {@link String#equals(Object)} 会因提前返回而泄露长度与前缀信息，
 * 存在时序侧信道风险，敏感值统一通过本工具比较。</p>
 *
 * @author LIN
 */
public final class ConstantTimeUtils {

    private ConstantTimeUtils() {
    }

    /**
     * 以常量时间比较两个字符串是否相等。
     *
     * @param expected 期望值
     * @param actual 实际值
     * @return true 表示两个字符串完全相同
     */
    public static boolean equals(String expected, String actual) {
        if (expected == null || actual == null) {
            return expected == null && actual == null;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
