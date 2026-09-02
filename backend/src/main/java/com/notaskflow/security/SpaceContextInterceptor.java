package com.notaskflow.security;

import com.notaskflow.common.RequestContext;
import com.notaskflow.exception.BusinessException;
import com.notaskflow.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 空间上下文拦截器，读取 Spring MVC 已匹配的路径变量。
 *
 * @author LIN
 */
@Component
public class SpaceContextInterceptor implements HandlerInterceptor {

    private static final String SPACE_ID_VARIABLE = "spaceId";

    /**
     * 在控制器执行前写入当前空间上下文。
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param handler 处理器对象
     * @return 是否继续执行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(attribute instanceof Map<?, ?> variables)) {
            return true;
        }
        Object value = variables.get(SPACE_ID_VARIABLE);
        if (value == null) {
            return true;
        }
        try {
            RequestContext.setCurrentSpaceId(Long.valueOf(String.valueOf(value)));
            return true;
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "空间ID格式不正确");
        }
    }

    /**
     * 请求完成后清理当前线程上下文。
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param handler 处理器对象
     * @param ex 执行异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        RequestContext.clear();
    }
}
