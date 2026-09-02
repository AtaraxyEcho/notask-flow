package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notaskflow.common.PageResponse;
import com.notaskflow.common.enums.ClientType;
import com.notaskflow.domain.entity.AdminOperationLog;
import com.notaskflow.domain.entity.LoginLog;
import com.notaskflow.domain.query.AdminLoginLogQuery;
import com.notaskflow.domain.query.AdminOperationLogQuery;
import com.notaskflow.domain.query.EventFailLogQuery;
import com.notaskflow.domain.vo.AdminLoginLogVO;
import com.notaskflow.domain.vo.AdminOperationLogVO;
import com.notaskflow.domain.vo.EventFailLogVO;
import com.notaskflow.mapper.AdminOperationLogMapper;
import com.notaskflow.mapper.LoginLogMapper;
import com.notaskflow.service.AdminLogService;
import com.notaskflow.service.EventFailLogService;
import com.notaskflow.utils.RequestClientUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 管理端日志服务实现。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLogServiceImpl implements AdminLogService {

    private static final int TEXT_MAX_LENGTH = 500;

    private final LoginLogMapper loginLogMapper;

    private final AdminOperationLogMapper adminOperationLogMapper;

    private final EventFailLogService eventFailLogService;

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
    @Override
    public void recordLogin(Long userId, String account, ClientType clientType, String deviceId, boolean success, String failReason) {
        try {
            LoginLog loginLog = new LoginLog();
            loginLog.setUserId(userId);
            loginLog.setAccount(account);
            loginLog.setClientType(clientType);
            loginLog.setDeviceId(deviceId);
            loginLog.setIpAddress(RequestClientUtil.currentIp());
            loginLog.setUserAgent(truncate(RequestClientUtil.currentUserAgent()));
            loginLog.setSuccess(success);
            loginLog.setFailReason(truncate(failReason));
            loginLogMapper.insert(loginLog);
        } catch (RuntimeException exception) {
            log.warn("登录日志记录失败，account={}", account, exception);
        }
    }

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
    @Override
    public void recordOperation(
            String operator,
            String method,
            String path,
            String operationName,
            boolean success,
            String errorMessage) {
        try {
            AdminOperationLog operationLog = new AdminOperationLog();
            operationLog.setOperator(operator);
            operationLog.setMethod(method);
            operationLog.setPath(path);
            operationLog.setOperationName(operationName);
            operationLog.setIpAddress(RequestClientUtil.currentIp());
            operationLog.setUserAgent(truncate(RequestClientUtil.currentUserAgent()));
            operationLog.setSuccess(success);
            operationLog.setErrorMessage(truncate(errorMessage));
            adminOperationLogMapper.insert(operationLog);
        } catch (RuntimeException exception) {
            log.warn("管理操作日志记录失败，path={}", path, exception);
        }
    }

    /**
     * 分页查询登录日志。
     *
     * @param query 查询条件
     * @return 登录日志分页
     */
    @Override
    public PageResponse<AdminLoginLogVO> loginLogs(AdminLoginLogQuery query) {
        Page<LoginLog> page = new Page<>(query.safePageNum(), query.safePageSize());
        LambdaQueryWrapper<LoginLog> wrapper = Wrappers.<LoginLog>lambdaQuery()
                .eq(query.getSuccess() != null, LoginLog::getSuccess, query.getSuccess())
                .and(StringUtils.hasText(query.getKeyword()), nested -> nested
                        .like(LoginLog::getAccount, query.getKeyword())
                        .or()
                        .like(LoginLog::getIpAddress, query.getKeyword()))
                .orderByDesc(LoginLog::getGmtCreate);
        Page<LoginLog> result = loginLogMapper.selectPage(page, wrapper);
        List<AdminLoginLogVO> list = result.getRecords().stream()
                .map(this::toLoginLogVO)
                .toList();
        return PageResponse.of(result, list);
    }

    /**
     * 分页查询管理操作日志。
     *
     * @param query 查询条件
     * @return 管理操作日志分页
     */
    @Override
    public PageResponse<AdminOperationLogVO> operationLogs(AdminOperationLogQuery query) {
        Page<AdminOperationLog> page = new Page<>(query.safePageNum(), query.safePageSize());
        LambdaQueryWrapper<AdminOperationLog> wrapper = Wrappers.<AdminOperationLog>lambdaQuery()
                .eq(query.getSuccess() != null, AdminOperationLog::getSuccess, query.getSuccess())
                .and(StringUtils.hasText(query.getKeyword()), nested -> nested
                        .like(AdminOperationLog::getOperator, query.getKeyword())
                        .or()
                        .like(AdminOperationLog::getPath, query.getKeyword())
                        .or()
                        .like(AdminOperationLog::getOperationName, query.getKeyword()))
                .orderByDesc(AdminOperationLog::getGmtCreate);
        Page<AdminOperationLog> result = adminOperationLogMapper.selectPage(page, wrapper);
        List<AdminOperationLogVO> list = result.getRecords().stream()
                .map(this::toOperationLogVO)
                .toList();
        return PageResponse.of(result, list);
    }

    /**
     * 分页查询系统事件失败日志。
     *
     * @param query 查询条件
     * @return 系统事件失败日志分页
     */
    @Override
    public PageResponse<EventFailLogVO> systemLogs(EventFailLogQuery query) {
        return eventFailLogService.page(query);
    }

    /**
     * 转换登录日志。
     *
     * @param loginLog 登录日志
     * @return 登录日志视图对象
     */
    private AdminLoginLogVO toLoginLogVO(LoginLog loginLog) {
        return new AdminLoginLogVO(
                loginLog.getId(),
                loginLog.getUserId(),
                loginLog.getAccount(),
                loginLog.getClientType(),
                loginLog.getDeviceId(),
                loginLog.getIpAddress(),
                loginLog.getUserAgent(),
                loginLog.getSuccess(),
                loginLog.getFailReason(),
                loginLog.getGmtCreate());
    }

    /**
     * 转换管理操作日志。
     *
     * @param operationLog 管理操作日志
     * @return 管理操作日志视图对象
     */
    private AdminOperationLogVO toOperationLogVO(AdminOperationLog operationLog) {
        return new AdminOperationLogVO(
                operationLog.getId(),
                operationLog.getOperator(),
                operationLog.getMethod(),
                operationLog.getPath(),
                operationLog.getOperationName(),
                operationLog.getIpAddress(),
                operationLog.getUserAgent(),
                operationLog.getSuccess(),
                operationLog.getErrorMessage(),
                operationLog.getGmtCreate());
    }

    /**
     * 截断文本。
     *
     * @param value 原始文本
     * @return 截断后文本
     */
    private String truncate(String value) {
        if (!StringUtils.hasText(value) || value.length() <= TEXT_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, TEXT_MAX_LENGTH);
    }
}
