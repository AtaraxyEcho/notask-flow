package com.notaskflow.common;

/**
 * 请求级上下文，保存当前请求的空间标识。
 *
 * @author LIN
 */
public final class RequestContext {

    private static final ThreadLocal<Long> CURRENT_SPACE_ID = new ThreadLocal<>();

    private RequestContext() {
    }

    /**
     * 设置当前空间标识。
     *
     * @param spaceId 空间标识
     */
    public static void setCurrentSpaceId(Long spaceId) {
        CURRENT_SPACE_ID.set(spaceId);
    }

    /**
     * 获取当前空间标识。
     *
     * @return 空间标识
     */
    public static Long getCurrentSpaceId() {
        return CURRENT_SPACE_ID.get();
    }

    /**
     * 清理当前线程上下文。
     */
    public static void clear() {
        CURRENT_SPACE_ID.remove();
    }
}
