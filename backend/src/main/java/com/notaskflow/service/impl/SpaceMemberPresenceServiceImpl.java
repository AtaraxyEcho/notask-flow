package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.notaskflow.domain.entity.SpaceMember;
import com.notaskflow.mapper.SpaceMemberMapper;
import com.notaskflow.service.SpaceMemberPresenceService;
import com.notaskflow.utils.RedisUtil;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 基于 Redis 心跳的空间成员在线状态服务实现。
 *
 * @author LIN
 */
@Service
@RequiredArgsConstructor
public class SpaceMemberPresenceServiceImpl implements SpaceMemberPresenceService {

    private static final String MEMBER_ONLINE_KEY_PREFIX = "notask:space:member:online-clients:";

    private static final String LEGACY_MEMBER_ONLINE_KEY_PREFIX = "notask:space:member:online:";

    private static final Duration MEMBER_ONLINE_TTL = Duration.ofSeconds(45);

    private static final String DEFAULT_CLIENT_ID = "default";

    private final RedisUtil redisUtil;

    private final SpaceMemberMapper spaceMemberMapper;

    /**
     * 标记空间成员在线。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     * @param clientId 客户端标识
     * @return true 表示成员从离线变为在线
     */
    @Override
    public boolean markOnline(Long spaceId, Long userId, String clientId) {
        String key = memberOnlineKey(spaceId, userId);
        boolean alreadyOnline = Boolean.TRUE.equals(redisUtil.hasKey(key));
        redisUtil.hashPut(key, normalizeClientId(clientId), String.valueOf(System.currentTimeMillis()));
        redisUtil.expire(key, MEMBER_ONLINE_TTL);
        return !alreadyOnline;
    }

    /**
     * 标记空间成员离线。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     * @param clientId 客户端标识
     * @return true 表示成员从在线变为离线
     */
    @Override
    public boolean markOffline(Long spaceId, Long userId, String clientId) {
        String key = memberOnlineKey(spaceId, userId);
        boolean wasOnline = Boolean.TRUE.equals(redisUtil.hasKey(key));
        redisUtil.hashDelete(key, normalizeClientId(clientId));
        Long clientCount = redisUtil.hashSize(key);
        if (clientCount == null || clientCount <= 0) {
            redisUtil.delete(key);
            return wasOnline;
        }
        return false;
    }

    /**
     * 清理用户在指定空间中的全部在线状态。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     */
    @Override
    public void clearMemberOnline(Long spaceId, Long userId) {
        redisUtil.delete(List.of(
                memberOnlineKey(spaceId, userId),
                legacyMemberOnlineKey(spaceId, userId)
        ));
    }

    /**
     * 清理用户在所有空间中的在线状态。
     *
     * @param userId 用户标识
     */
    @Override
    public void clearUserOnline(Long userId) {
        List<String> keys = spaceMemberMapper.selectList(Wrappers.<SpaceMember>lambdaQuery()
                        .eq(SpaceMember::getUserId, userId))
                .stream()
                .flatMap(member -> List.of(
                        memberOnlineKey(member.getSpaceId(), userId),
                        legacyMemberOnlineKey(member.getSpaceId(), userId)
                ).stream())
                .toList();
        if (!keys.isEmpty()) {
            redisUtil.delete(keys);
        }
    }

    /**
     * 判断空间成员是否在线。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     * @return true 表示在线
     */
    @Override
    public Boolean isOnline(Long spaceId, Long userId) {
        return Boolean.TRUE.equals(redisUtil.hasKey(memberOnlineKey(spaceId, userId)));
    }

    /**
     * 规范化客户端标识。
     *
     * @param clientId 客户端标识
     * @return 可用于 Redis Hash 字段的客户端标识
     */
    private String normalizeClientId(String clientId) {
        return StringUtils.hasText(clientId) ? clientId.trim() : DEFAULT_CLIENT_ID;
    }

    /**
     * 构造空间成员在线状态缓存键。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     * @return Redis 缓存键
     */
    private String memberOnlineKey(Long spaceId, Long userId) {
        return MEMBER_ONLINE_KEY_PREFIX + spaceId + ":" + userId;
    }

    /**
     * 构造旧版空间成员在线状态缓存键，用于登出时清理历史残留。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     * @return Redis 缓存键
     */
    private String legacyMemberOnlineKey(Long spaceId, Long userId) {
        return LEGACY_MEMBER_ONLINE_KEY_PREFIX + spaceId + ":" + userId;
    }
}
