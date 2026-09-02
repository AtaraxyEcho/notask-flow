package com.notaskflow.service;

import com.notaskflow.domain.vo.FileStatsVO;

/**
 * 文件统计缓存服务。
 *
 * @author LIN
 */
public interface FileStatsCacheService {

    /**
     * 读取缓存中的文件统计。
     *
     * @param spaceId 空间标识
     * @return 文件统计
     */
    FileStatsVO getCached(Long spaceId);

    /**
     * 刷新空间文件统计缓存。
     *
     * @param spaceId 空间标识
     * @return 文件统计
     */
    FileStatsVO refresh(Long spaceId);
}
