package com.notaskflow.service;

/**
 * 空间成员在线状态服务。
 *
 * @author LIN
 */
public interface SpaceMemberPresenceService {

    /**
     * 标记空间成员在线。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     * @param clientId 客户端标识
     * @return true 表示成员从离线变为在线
     */
    boolean markOnline(Long spaceId, Long userId, String clientId);

    /**
     * 标记空间成员离线。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     * @param clientId 客户端标识
     * @return true 表示成员从在线变为离线
     */
    boolean markOffline(Long spaceId, Long userId, String clientId);

    /**
     * 清理用户在指定空间中的全部在线状态。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     */
    void clearMemberOnline(Long spaceId, Long userId);

    /**
     * 清理用户在所有空间中的在线状态。
     *
     * @param userId 用户标识
     */
    void clearUserOnline(Long userId);

    /**
     * 判断空间成员是否在线。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     * @return true 表示在线
     */
    Boolean isOnline(Long spaceId, Long userId);
}
