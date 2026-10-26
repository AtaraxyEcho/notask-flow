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
            long count = redisUtil.takeCounter(key);
            if (count <= 0) {
                return;
            }
            int updated = noteMapper.incrementViewCount(noteId, count);
            if (updated <= 0) {
                // 笔记已不存在或已删除，重新累加避免并发访问的浏览量被丢弃
                redisUtil.increment(key, count);
                log.debug("笔记浏览量回写目标不存在，已回滚计数，noteId={}", noteId);
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
