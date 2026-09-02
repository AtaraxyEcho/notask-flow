package com.notaskflow.job;

import com.notaskflow.service.FileTrashCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 文件回收站清理定时任务。
 *
 * @author LIN
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileTrashCleanupJob {

    private final FileTrashCleanupService fileTrashCleanupService;

    /**
     * 每天凌晨清理超过保留期的回收站文件。
     */
    @Scheduled(cron = "${notask-flow.file.cleanup-cron:0 0 3 * * *}")
    public void cleanupExpiredTrashFiles() {
        int cleaned = fileTrashCleanupService.cleanupExpiredTrashFiles();
        if (cleaned > 0) {
            log.info("文件回收站定时清理完成，cleaned={}", cleaned);
        }
    }
}
