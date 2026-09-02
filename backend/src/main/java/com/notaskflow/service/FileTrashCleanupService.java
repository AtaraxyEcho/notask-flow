package com.notaskflow.service;

/**
 * 文件回收站清理服务。
 *
 * @author LIN
 */
public interface FileTrashCleanupService {

    /**
     * 清理超过保留期且未被引用的回收站文件。
     *
     * @return 清理的文件数量
     */
    int cleanupExpiredTrashFiles();
}
