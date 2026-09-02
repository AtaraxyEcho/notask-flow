package com.notaskflow.service;

import com.notaskflow.domain.vo.AdminDashboardVO;

/**
 * 管理端数据大盘服务。
 *
 * @author LIN
 */
public interface AdminDashboardService {

    /**
     * 查询管理端数据大盘。
     *
     * @return 数据大盘
     */
    AdminDashboardVO overview();
}
