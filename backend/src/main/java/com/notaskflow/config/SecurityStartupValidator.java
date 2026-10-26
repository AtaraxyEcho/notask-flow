package com.notaskflow.config;

import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 启动期安全配置校验。
 *
 * <p>生产环境（{@code prod} / {@code docker}）下拒绝使用示例或占位密钥启动，
 * 避免运维未覆盖环境变量导致弱口令上线。</p>
 *
 * @author LIN
 */
@Slf4j
@Component
public class SecurityStartupValidator implements InitializingBean {

    private static final List<String> PRODUCTION_PROFILES = List.of("prod", "docker");

    private static final List<String> WEAK_SECRET_MARKERS = List.of(
            "change-me",
            "change_me",
            "changeme",
            "your-jwt-secret",
            "your-secret",
            "minioadmin",
            "admin123456");

    private static final int MIN_SECRET_LENGTH = 16;

    private final Environment environment;

    private final AdminProperties adminProperties;

    private final CollaborationProperties collaborationProperties;

    /**
     * 创建校验器。
     *
     * @param environment 环境配置
     * @param adminProperties 管理端配置
     * @param collaborationProperties 协作配置
     */
    public SecurityStartupValidator(Environment environment,
                                    AdminProperties adminProperties,
                                    CollaborationProperties collaborationProperties) {
        this.environment = environment;
        this.adminProperties = adminProperties;
        this.collaborationProperties = collaborationProperties;
    }

    @Override
    public void afterPropertiesSet() {
        if (!isProductionProfile()) {
            return;
        }
        validateSecret("notask-flow.admin.password", adminProperties.getPassword());
        validateSecret("notask-flow.collab.internal-token", collaborationProperties.getInternalToken());
        validateSecret("sa-token.jwt-secret-key", environment.getProperty("sa-token.jwt-secret-key"));
    }

    /**
     * 判断当前是否处于生产类环境。
     *
     * @return true 表示生产环境
     */
    private boolean isProductionProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if (PRODUCTION_PROFILES.contains(profile.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验密钥强度。
     *
     * @param propertyName 配置项名称
     * @param value 密钥值
     */
    private void validateSecret(String propertyName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("生产环境必须配置 " + propertyName);
        }
        if (value.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("生产环境 " + propertyName + " 长度不能少于 " + MIN_SECRET_LENGTH + " 位");
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String marker : WEAK_SECRET_MARKERS) {
            if (normalized.contains(marker)) {
                throw new IllegalStateException("生产环境 " + propertyName + " 仍为示例弱密钥，请通过环境变量覆盖");
            }
        }
        log.info("安全配置校验通过：{}", propertyName);
    }
}
