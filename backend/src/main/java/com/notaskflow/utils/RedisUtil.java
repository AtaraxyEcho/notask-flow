package com.notaskflow.utils;

import com.notaskflow.exception.BusinessException;
import com.notaskflow.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Redis 操作工具类，封装常用缓存、限流和分布式锁能力。
 *
 * @author LIN
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private static final String RELEASE_LOCK_SCRIPT_TEXT = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """;

    private static final RedisScript<Long> RELEASE_LOCK_SCRIPT =
            new DefaultRedisScript<>(RELEASE_LOCK_SCRIPT_TEXT, Long.class);

    private final RedisTemplate<Object, Object> redisTemplate;

    private final StringRedisTemplate stringRedisTemplate;

    private final DefaultRedisScript<Long> limitScript;

    /**
     * 写入带过期时间的缓存。
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 过期时间
     */
    public void set(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    /**
     * 写入永久缓存。
     *
     * @param key 缓存键
     * @param value 缓存值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 仅当键不存在时写入缓存。
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 过期时间
     * @return true 表示写入成功
     */
    public boolean setIfAbsent(String key, Object value, Duration ttl) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 读取字符串缓存。
     *
     * @param key 缓存键
     * @return 字符串值
     */
    public String getString(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 读取指定类型的对象缓存。
     *
     * @param key 缓存键
     * @param type 目标类型
     * @param <T> 目标类型
     * @return 对象值
     */
    public <T> T getObject(String key, Class<T> type) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null || !type.isInstance(value)) {
            return null;
        }
        return type.cast(value);
    }

    /**
     * 读取并删除字符串缓存。
     *
     * @param key 缓存键
     * @return 字符串值
     */
    public String getAndDeleteString(String key) {
        Object value = redisTemplate.opsForValue().getAndDelete(key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 删除单个缓存。
     *
     * @param key 缓存键
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 批量删除缓存。
     *
     * @param keys 缓存键集合
     */
    public void delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        redisTemplate.delete(keys.stream().map(key -> (Object) key).toList());
    }

    /**
     * 设置缓存过期时间。
     *
     * @param key 缓存键
     * @param ttl 过期时间
     */
    public void expire(String key, Duration ttl) {
        redisTemplate.expire(key, ttl);
    }

    /**
     * 获取缓存剩余过期秒数。
     *
     * @param key 缓存键
     * @return 剩余秒数
     */
    public Long getExpireSeconds(String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 自增计数。
     *
     * @param key 缓存键
     * @return 自增后的值
     */
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 按指定步长自增计数。
     *
     * @param key 缓存键
     * @param delta 增量
     * @return 自增后的值
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 读取长整型计数。
     *
     * @param key 缓存键
     * @return 长整型值
     */
    public Long getLong(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return parseCounter(String.valueOf(value));
    }

    /**
     * 使用 SCAN 按模式查找键，避免阻塞 Redis。
     *
     * @param pattern 键匹配表达式
     * @param count 单次扫描建议数量
     * @return 匹配键集合
     */
    public Set<String> scanKeys(String pattern, long count) {
        Set<String> keys = new LinkedHashSet<>();
        redisTemplate.execute((RedisConnection connection) -> {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(count)
                    .build();
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                cursor.forEachRemaining(bytes -> keys.add(new String(bytes, StandardCharsets.UTF_8)));
            }
            return null;
        });
        return keys;
    }

    /**
     * 写入 Hash 字段。
     *
     * @param key 缓存键
     * @param hashKey Hash 字段
     * @param value 字段值
     */
    public void hashPut(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 删除 Hash 字段。
     *
     * @param key 缓存键
     * @param hashKey Hash 字段
     */
    public void hashDelete(String key, String hashKey) {
        redisTemplate.opsForHash().delete(key, hashKey);
    }

    /**
     * 向 Set 写入成员。
     *
     * @param key 缓存键
     * @param value 成员值
     */
    public void setAdd(String key, String value) {
        redisTemplate.opsForSet().add(key, value);
    }

    /**
     * 从 Set 移除成员。
     *
     * @param key 缓存键
     * @param value 成员值
     */
    public void setRemove(String key, String value) {
        redisTemplate.opsForSet().remove(key, value);
    }

    /**
     * 读取 Set 全量成员。
     *
     * @param key 缓存键
     * @return 成员集合
     */
    public Set<String> setMembers(String key) {
        Set<Object> members = redisTemplate.opsForSet().members(key);
        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> values = new LinkedHashSet<>();
        for (Object member : members) {
            values.add(String.valueOf(member));
        }
        return values;
    }

    /**
     * 获取 Hash 字段数量。
     *
     * @param key 缓存键
     * @return 字段数量
     */
    public Long hashSize(String key) {
        return redisTemplate.opsForHash().size(key);
    }

    /**
     * 判断键是否存在。
     *
     * @param key 缓存键
     * @return true 表示存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 执行限流检查。
     *
     * @param key 限流键
     * @param maxAttempts 最大请求次数
     * @param window 统计窗口
     * @param message 超限提示
     */
    public void limit(String key, int maxAttempts, Duration window, String message) {
        Long current = stringRedisTemplate.execute(
                limitScript,
                Collections.singletonList(key),
                String.valueOf(maxAttempts),
                String.valueOf(window.toSeconds())
        );
        if (current != null && current > maxAttempts) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, message);
        }
    }

    /**
     * 检查失败计数是否已经达到限制，不增加计数。
     *
     * @param key 计数键
     * @param maxAttempts 最大允许失败次数
     * @param message 超限提示
     */
    public void checkFailureLimit(String key, int maxAttempts, String message) {
        Long current = parseCounter(stringRedisTemplate.opsForValue().get(key));
        if (current != null && current >= maxAttempts) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, message);
        }
    }

    /**
     * 检查失败计数是否已经达到限制，保留统计窗口参数以兼容限流调用。
     *
     * @param key 计数键
     * @param maxAttempts 最大允许失败次数
     * @param window 统计窗口
     * @param message 超限提示
     */
    public void checkFailureLimit(String key, int maxAttempts, Duration window, String message) {
        checkFailureLimit(key, maxAttempts, message);
    }

    /**
     * 记录一次失败并在首次失败时设置统计窗口。
     *
     * @param key 计数键
     * @param maxAttempts 最大允许失败次数
     * @param window 统计窗口
     * @param message 超限提示
     */
    public void recordLimitedFailure(String key, int maxAttempts, Duration window, String message) {
        Long current = stringRedisTemplate.opsForValue().increment(key);
        if (current != null && current == 1L) {
            stringRedisTemplate.expire(key, window);
        }
        if (current != null && current > maxAttempts) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, message);
        }
    }
//    public void limit(String key, int maxAttempts, Duration window, String message) {
//        log.info("限流key: {}", key);
//        Long current = redisTemplate.execute(
//                limitScript,
//                singletonRedisKey(key),
//                String.valueOf(maxAttempts),
//                String.valueOf(window.toSeconds())
//        );
//        if (current > maxAttempts) {
//            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, message);
//        }
//    }

    /**
     * 尝试获取分布式锁。
     *
     * @param key 锁键
     * @param owner 锁持有者标识
     * @param ttl 锁过期时间
     * @return true 表示获取成功
     */
    public boolean tryLock(String key, String owner, Duration ttl) {
        return setIfAbsent(key, owner, ttl);
    }

    /**
     * 释放分布式锁，仅允许锁持有者释放。
     *
     * @param key 锁键
     * @param owner 锁持有者标识
     */
    public void unlock(String key, String owner) {
        try {
            redisTemplate.execute(RELEASE_LOCK_SCRIPT, singletonRedisKey(key), owner);
        } catch (RuntimeException exception) {
            log.warn("释放 Redis 锁失败，key={}", key, exception);
        }
    }

    private List<Object> singletonRedisKey(String key) {
        return Collections.singletonList(key);
    }

    private Long parseCounter(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            log.warn("Redis 计数值格式无效，value={}", value, exception);
            return null;
        }
    }
}
