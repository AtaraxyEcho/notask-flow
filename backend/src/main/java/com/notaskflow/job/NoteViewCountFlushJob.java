package com.notaskflow.job;

import com.notaskflow.common.constant.RedisKeyConstants;
import com.notaskflow.mapper.NoteMapper;
import com.notaskflow.utils.RedisUtil;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 笔记浏览量 Redis 计数回写任务。
 *
 * @author LIN
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoteViewCountFlushJob {

    private static final long SCAN_COUNT = 200L;

    private final RedisUtil redisUtil;

    private final NoteMapper noteMapper;

    /**
     * 定时将 Redis 中的笔记浏览量增量回写到 MySQL。
     */
    @Scheduled(fixedDelayString = "${notask-flow.note.view-count-flush-delay-ms:60000}")
    public void flushViewCounts() {
        Set<String> keys;
        try {
            keys = redisUtil.scanKeys(RedisKeyConstants.noteViewCountPattern(), SCAN_COUNT);
        } catch (RuntimeException exception) {
            log.warn("扫描笔记浏览量 Redis 键失败", exception);
            return;
        }
        for (String key : keys) {
            flushSingleKey(key);
        }
    }

    private void flushSingleKey(String key) {
        Long noteId = parseNoteId(key);
        if (noteId == null) {
            return;
        }
        try {
            Long count = redisUtil.getLong(key);
            if (count == null || count <= 0) {
                redisUtil.delete(key);
                return;
            }
            int updated = noteMapper.incrementViewCount(noteId, count);
            if (updated <= 0) {
                redisUtil.delete(key);
                return;
            }
            Long remaining = redisUtil.increment(key, -count);
            if (remaining == null || remaining <= 0) {
                redisUtil.delete(key);
            }
        } catch (RuntimeException exception) {
            log.warn("笔记浏览量回写失败，key={}", key, exception);
        }
    }

    private Long parseNoteId(String key) {
        if (key == null || !key.startsWith(RedisKeyConstants.NOTE_VIEW_COUNT_PREFIX)) {
            return null;
        }
        String noteIdValue = key.substring(RedisKeyConstants.NOTE_VIEW_COUNT_PREFIX.length());
        try {
            return Long.valueOf(noteIdValue);
        } catch (NumberFormatException exception) {
            log.warn("笔记浏览量 Redis 键格式异常，key={}", key, exception);
            return null;
        }
    }
}
