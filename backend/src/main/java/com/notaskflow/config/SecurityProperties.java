package com.notaskflow.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Web安全防护配置属性。
 *
 * @author LIN
 */
@Data
@Component
@ConfigurationProperties(prefix = "notask-flow.security")
public class SecurityProperties {

    /** 是否启用CSRF防护。当前项目使用Bearer Token，默认关闭。 */
    private boolean csrfEnabled = false;

    /** CORS允许的前端来源。 */
    private List<String> allowedOrigins = new ArrayList<>(List.of(
            "http://localhost:3000",
            "http://127.0.0.1:3000",
            "http://192.168.1.20:3000"));

    /** CORS是否允许携带凭证。 */
    private boolean allowCredentials = false;

    /** 内容安全策略。 */
    private String contentSecurityPolicy =
            "default-src 'self'; "
                    + "script-src 'self' 'unsafe-inline' 'wasm-unsafe-eval'; "
                    + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                    + "font-src 'self' https://fonts.gstatic.com data:; "
                    + "img-src 'self' data: blob: http: https:; "
                    + "media-src 'self' blob: http: https:; "
                    + "connect-src 'self' http: https: ws: wss:; "
                    + "frame-src 'self' blob:; "
                    + "object-src 'none'; "
                    + "base-uri 'self'; "
                    + "frame-ancestors 'self'";

    /** 浏览器权限策略。 */
    private String permissionsPolicy = "camera=(), microphone=(), geolocation=(), payment=()";
}

