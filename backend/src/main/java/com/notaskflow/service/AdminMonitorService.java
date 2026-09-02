package com.notaskflow.service;

import com.notaskflow.domain.vo.AdminSystemMonitorVO;

/**
 * 管理端系统监控服务。
 *
 * @author LIN
 */
public interface AdminMonitorService {

    /**
     * 查询系统运行快照。
     *
     * @return 系统运行快照
     */
    AdminSystemMonitorVO snapshot();
}
