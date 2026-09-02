package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.notaskflow.common.enums.PushPlatform;
import com.notaskflow.common.enums.PushProvider;
import com.notaskflow.domain.dto.request.DeviceTokenRegisterRequest;
import com.notaskflow.domain.entity.UserDeviceToken;
import com.notaskflow.domain.vo.UserDeviceTokenVO;
import com.notaskflow.mapper.UserDeviceTokenMapper;
import com.notaskflow.service.UserDeviceTokenService;
import com.notaskflow.utils.LoginUserUtil;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户移动端推送设备令牌服务实现。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeviceTokenServiceImpl implements UserDeviceTokenService {

    private final UserDeviceTokenMapper userDeviceTokenMapper;

    /**
     * 注册或刷新当前用户的设备推送令牌。
     *
     * @param request 注册请求
     * @return 设备推送令牌信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDeviceTokenVO registerCurrent(DeviceTokenRegisterRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        LocalDateTime now = LocalDateTime.now();
        String deviceId = request.getDeviceId().trim();
        String deviceToken = request.getDeviceToken().trim();
        PushProvider provider = resolveProvider(request);
        disableSameTokenOnOtherDevices(currentUserId, request.getPlatform(), deviceId, deviceToken);

        UserDeviceToken token = findByCurrentDevice(currentUserId, request.getPlatform(), deviceId);
        if (token == null) {
            token = new UserDeviceToken();
            token.setUserId(currentUserId);
            token.setPlatform(request.getPlatform());
            token.setDeviceId(deviceId);
            token.setProvider(provider);
            token.setDeviceName(trimToNull(request.getDeviceName()));
            token.setDeviceToken(deviceToken);
            token.setAppVersion(trimToNull(request.getAppVersion()));
            token.setEnabled(true);
            token.setLastActiveAt(now);
            userDeviceTokenMapper.insert(token);
        } else {
            token.setProvider(provider);
            token.setDeviceName(trimToNull(request.getDeviceName()));
            token.setDeviceToken(deviceToken);
            token.setAppVersion(trimToNull(request.getAppVersion()));
            token.setEnabled(true);
            token.setLastActiveAt(now);
            userDeviceTokenMapper.updateById(token);
        }

        log.info("用户设备推送令牌已注册，userId={}, platform={}, deviceId={}",
                currentUserId, request.getPlatform(), deviceId);
        return toVO(token);
    }

    /**
     * 解绑当前用户的设备推送令牌。
     *
     * @param platform 推送平台
     * @param deviceId 设备标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindCurrent(PushPlatform platform, String deviceId) {
        Long currentUserId = LoginUserUtil.currentUserId();
        String normalizedDeviceId = deviceId.trim();
        UserDeviceToken token = findByCurrentDevice(currentUserId, platform, normalizedDeviceId);
        if (token == null) {
            log.info("用户设备推送令牌不存在，跳过解绑，userId={}, platform={}, deviceId={}",
                    currentUserId, platform, normalizedDeviceId);
            return;
        }
        token.setEnabled(false);
        token.setLastActiveAt(LocalDateTime.now());
        userDeviceTokenMapper.updateById(token);
        log.info("用户设备推送令牌已解绑，userId={}, platform={}, deviceId={}",
                currentUserId, platform, normalizedDeviceId);
    }

    /**
     * 查询当前用户指定设备的推送令牌状态。
     *
     * @param platform 推送平台
     * @param deviceId 设备标识
     * @return 设备推送令牌信息
     */
    @Override
    public UserDeviceTokenVO current(PushPlatform platform, String deviceId) {
        UserDeviceToken token = findByCurrentDevice(LoginUserUtil.currentUserId(), platform, deviceId.trim());
        return token == null ? null : toVO(token);
    }

    private void disableSameTokenOnOtherDevices(Long currentUserId, PushPlatform platform, String deviceId,
                                                String deviceToken) {
        userDeviceTokenMapper.update(null, Wrappers.<UserDeviceToken>lambdaUpdate()
                .eq(UserDeviceToken::getPlatform, platform)
                .eq(UserDeviceToken::getDeviceToken, deviceToken)
                .and(wrapper -> wrapper.ne(UserDeviceToken::getUserId, currentUserId)
                        .or()
                        .ne(UserDeviceToken::getDeviceId, deviceId))
                .set(UserDeviceToken::getEnabled, false)
                .set(UserDeviceToken::getLastActiveAt, LocalDateTime.now()));
    }

    private UserDeviceToken findByCurrentDevice(Long userId, PushPlatform platform, String deviceId) {
        List<UserDeviceToken> tokens = userDeviceTokenMapper.selectList(Wrappers.<UserDeviceToken>lambdaQuery()
                .eq(UserDeviceToken::getUserId, userId)
                .eq(UserDeviceToken::getPlatform, platform)
                .eq(UserDeviceToken::getDeviceId, deviceId)
                .orderByDesc(UserDeviceToken::getGmtModified));
        return tokens.stream().findFirst().orElse(null);
    }

    private PushProvider resolveProvider(DeviceTokenRegisterRequest request) {
        if (request.getProvider() != null) {
            return request.getProvider();
        }
        if (PushPlatform.IOS.equals(request.getPlatform())) {
            return PushProvider.APNS;
        }
        return PushProvider.FCM;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private UserDeviceTokenVO toVO(UserDeviceToken token) {
        return new UserDeviceTokenVO(
                token.getId(),
                token.getPlatform(),
                token.getProvider(),
                token.getDeviceId(),
                token.getDeviceName(),
                token.getAppVersion(),
                token.getEnabled(),
                token.getLastActiveAt()
        );
    }
}
