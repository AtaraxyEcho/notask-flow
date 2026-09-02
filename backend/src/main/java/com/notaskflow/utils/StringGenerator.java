package com.notaskflow.utils;

import java.security.SecureRandom;

/**
 * 随机字符串生成工具。
 *
 * @author LIN
 */
public final class StringGenerator {

    private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private static final SecureRandom RANDOM = new SecureRandom();

    private StringGenerator() {
    }

    /**
     * 生成指定长度的随机字符串。
     *
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String randomAlphanumeric(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return builder.toString();
    }

    /**
     * 生成指定长度的随机数字字符串。
     *
     * @param length 字符串长度
     * @return 随机数字字符串
     */
    public static String randomNumeric(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(RANDOM.nextInt(10));
        }
        return builder.toString();
    }
}
