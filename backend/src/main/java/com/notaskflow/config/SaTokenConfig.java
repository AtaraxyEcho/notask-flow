package com.notaskflow.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.notaskflow.security.AdminOperationLogInterceptor;
import com.notaskflow.security.SpaceContextInterceptor;
import com.notaskflow.service.LoginSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 登录校验配置。
 *
 * @author LIN
 */
@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    private final SpaceContextInterceptor spaceContextInterceptor;

    private final AdminOperationLogInterceptor adminOperationLogInterceptor;

    private final LoginSessionService loginSessionService;

    /**
     * 注册登录校验拦截器。
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(spaceContextInterceptor)
                .addPathPatterns("/api/v1/**")
                .order(Ordered.HIGHEST_PRECEDENCE);
        registry.addInterceptor(new SaInterceptor(handle -> SaRouter.match("/api/v1/**")
                        .notMatch("/api/v1/auth/register/send-email-code")
                        .notMatch("/api/v1/auth/register")
                        .notMatch("/api/v1/auth/login")
                        .notMatch("/api/v1/auth/forgot-password")
                        .notMatch("/api/v1/auth/verify-reset-code")
                        .notMatch("/api/v1/auth/reset-password")
                        .notMatch("/api/v1/auth/settings")
                        .notMatch("/api/v1/admin/auth/login")
                        .notMatch("/api/v1/internal/collab/**")
                        .notMatch("/api/v1/public/**")
                        .notMatch("/api/v1/public/notes/**")
                        .check(router -> {
                            if (!isSwaggerRequest()) {
                                StpUtil.checkLogin();
                                loginSessionService.validateCurrentSession(isAdminRequest());
                            }
                        })))
                .addPathPatterns("/**")
                .order(Ordered.HIGHEST_PRECEDENCE + 1);
        registry.addInterceptor(adminOperationLogInterceptor)
                .addPathPatterns("/api/v1/admin/**")
                .order(Ordered.HIGHEST_PRECEDENCE + 2);
    }

    /**
     * 判断当前请求是否为 Swagger 相关资源。
     *
     * @return 是否为 Swagger 请求
     */
    private boolean isSwaggerRequest() {
        String path = SaHolder.getRequest().getRequestPath();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/doc.html")
                || path.startsWith("/webjars");
    }

    /**
     * 判断当前请求是否为管理端接口。
     *
     * @return 是否为管理端接口
     */
    private boolean isAdminRequest() {
        return SaHolder.getRequest().getRequestPath().startsWith("/api/v1/admin/");
    }
}
