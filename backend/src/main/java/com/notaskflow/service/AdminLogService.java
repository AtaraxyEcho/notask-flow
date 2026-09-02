package com.notaskflow.service;

import com.notaskflow.common.PageResponse;
import com.notaskflow.common.enums.ClientType;
import com.notaskflow.domain.query.AdminLoginLogQuery;
import com.notaskflow.domain.query.AdminOperationLogQuery;
import com.notaskflow.domain.query.EventFailLogQuery;
import com.notaskflow.domain.vo.AdminLoginLogVO;
import com.notaskflow.domain.vo.AdminOperationLogVO;
import com.notaskflow.domain.vo.EventFailLogVO;

/**
 * 管理端日志服务。
 *
 * @author LIN
 */
public interface AdminLogService {

    /**
     * 记录登录日志。
     *
     * @param userId 用户标识
     * @param account 登录账号
     * @param clientType 客户端类型
     * @param deviceId 设备标识
     * @param success 是否成功
     * @param failReason 失败原因
     */
    void recordLogin(Long userId, String account, ClientType clientType, String deviceId, boolean success, String failReason);

    /**
     * 记录管理操作日志。
     *
     * @param operator 操作人
     * @param method 请求方法
     * @param path 请求路径
     * @param operationName 操作名称
     * @param success 是否成功
     * @param errorMessage 错误信息
     */
    void recordOperation(
            String operator,
            String method,
            String path,
            String operationName,
            boolean success,
            String errorMessage);

    /**
     * 分页查询登录日志。
     *
     * @param query 查询条件
     * @return 登录日志分页
     */
    PageResponse<AdminLoginLogVO> loginLogs(AdminLoginLogQuery query);

    /**
     * 分页查询管理操作日志。
     *
     * @param query 查询条件
     * @return 管理操作日志分页
     */
    PageResponse<AdminOperationLogVO> operationLogs(AdminOperationLogQuery query);

    /**
     * 分页查询系统事件失败日志。
     *
     * @param query 查询条件
     * @return 系统事件失败日志分页
     */
    PageResponse<EventFailLogVO> systemLogs(EventFailLogQuery query);
}
