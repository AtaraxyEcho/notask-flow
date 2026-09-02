package com.notaskflow.service;

import com.notaskflow.domain.vo.AuthSystemSettingVO;
import com.notaskflow.domain.dto.request.SystemSettingUpdateRequest;
import com.notaskflow.domain.vo.SystemSettingVO;
import java.time.Duration;
import java.util.List;

/**
 * 系统设置服务。
 *
 * @author LIN
 */
public interface SystemSettingService {

    /**
     * 判断是否开放新用户注册。
     *
     * @return 是否开放新用户注册
     */
    boolean isRegistrationEnabled();

    /**
     * 判断注册是否必须验证邮箱。
     *
     * @return 注册是否必须验证邮箱
     */
    boolean isRegisterEmailVerificationRequired();

    /**
     * 判断是否仅允许同一账号单设备登录。
     *
     * @return 是否仅允许单设备登录
     */
    boolean isSingleDeviceLoginOnly();

    /**
     * 判断是否启用系统邮件发送。
     *
     * @return 是否启用邮件发送
     */
    boolean isMailEnabled();

    /**
     * 获取登录失败锁定阈值。
     *
     * @return 登录失败锁定阈值
     */
    int getLoginFailureLimit();

    /**
     * 获取登录失败锁定窗口。
     *
     * @return 登录失败锁定窗口
     */
    Duration getLoginFailureWindow();

    /**
     * 获取 Web 访问令牌有效秒数。
     *
     * @return Web 访问令牌有效秒数
     */
    int getWebAccessTokenSeconds();

    /**
     * 获取管理端访问令牌有效秒数。
     *
     * @return 管理端访问令牌有效秒数
     */
    int getAdminAccessTokenSeconds();

    /**
     * 获取移动端访问令牌有效秒数。
     *
     * @return 移动端访问令牌有效秒数
     */
    int getMobileAccessTokenSeconds();

    /**
     * 判断是否允许公开分享笔记。
     *
     * @return 是否允许公开分享笔记
     */
    boolean isNoteShareEnabled();

    /**
     * 获取分享链接默认过期分钟数。
     *
     * @return 分享链接默认过期分钟数
     */
    int getNoteShareDefaultExpireMinutes();

    /**
     * 获取笔记历史最大版本数。
     *
     * @return 笔记历史最大版本数
     */
    int getNoteHistoryMaxVersions();

    /**
     * 获取协作 Ticket 过期秒数。
     *
     * @return 协作 Ticket 过期秒数
     */
    int getCollabTicketExpireSeconds();

    /**
     * 判断新团队默认是否需要审核加入。
     *
     * @return 新团队默认是否需要审核加入
     */
    boolean isNewTeamJoinApprovalRequired();

    /**
     * 获取认证相关系统设置。
     *
     * @return 认证系统设置
     */
    AuthSystemSettingVO getAuthSettings();

    /**
     * 查询全部系统设置。
     *
     * @return 系统设置列表
     */
    List<SystemSettingVO> listSettings();

    /**
     * 更新系统设置。
     *
     * @param request 系统设置更新请求
     * @return 系统设置列表
     */
    List<SystemSettingVO> updateSettings(SystemSettingUpdateRequest request);
}
