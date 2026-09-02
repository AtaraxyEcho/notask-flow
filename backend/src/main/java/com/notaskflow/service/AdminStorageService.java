package com.notaskflow.service;

import com.notaskflow.common.PageResponse;
import com.notaskflow.domain.vo.AdminOrphanCleanResultVO;
import com.notaskflow.domain.vo.AdminOrphanFileVO;
import com.notaskflow.domain.vo.AdminStorageRankVO;
import com.notaskflow.domain.vo.AdminStorageSummaryVO;
import java.util.List;

/**
 * 管理端存储管理服务。
 *
 * @author LIN
 */
public interface AdminStorageService {

    /**
     * 查询存储汇总。
     *
     * @return 存储汇总
     */
    AdminStorageSummaryVO summary();

    /**
     * 查询用户存储占用排行。
     *
     * @return 用户存储占用排行
     */
    List<AdminStorageRankVO> topUsers();

    /**
     * 查询空间存储占用排行。
     *
     * @return 空间存储占用排行
     */
    List<AdminStorageRankVO> topSpaces();

    /**
     * 分页查询孤立文件。
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 孤立文件分页
     */
    PageResponse<AdminOrphanFileVO> orphanFiles(long pageNum, long pageSize);

    /**
     * 清理孤立文件。
     *
     * @return 清理结果
     */
    AdminOrphanCleanResultVO cleanOrphanFiles();
}
