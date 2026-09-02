package com.notaskflow.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.notaskflow.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器，统一以 HTTP 200 返回业务响应。
 *
 * @author LIN
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常。
     *
     * @param e 业务异常
     * @return 统一错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    /**
     * 处理未登录异常。
     *
     * @param e 未登录异常
     * @param request HTTP 请求
     * @return 统一错误响应
     */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotLogin(NotLoginException e, HttpServletRequest request) {
        log.warn("认证异常: path={}, message={}", request.getRequestURI(), e.getMessage());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.error(ErrorCode.UNAUTHORIZED.getCode(), resolveNotLoginMessage(e)));
    }

    private String resolveNotLoginMessage(NotLoginException e) {
        String type = e.getType();
        if (NotLoginException.BE_REPLACED.equals(type) || NotLoginException.KICK_OUT.equals(type)) {
            return "当前账号已在其他设备登录，本端已自动下线";
        }
        if (NotLoginException.TOKEN_TIMEOUT.equals(type)) {
            return "登录状态已过期，请重新登录";
        }
        return ErrorCode.UNAUTHORIZED.getMessage();
    }

    /**
     * 处理权限不足异常。
     *
     * @param e 权限不足异常
     * @return 统一错误响应
     */
    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotPermission(NotPermissionException e) {
        log.warn("权限异常: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.error(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMessage()));
    }

    /**
     * 处理参数绑定异常。
     *
     * @param e 参数绑定异常
     * @return 统一错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ":" + error.getDefaultMessage())
                .collect(Collectors.joining(";"));
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.error(ErrorCode.PARAM_ERROR.getCode(), message));
    }

    /**
     * 处理查询参数绑定异常。
     *
     * @param e 参数绑定异常
     * @return 统一错误响应
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ":" + error.getDefaultMessage())
                .collect(Collectors.joining(";"));
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.error(ErrorCode.PARAM_ERROR.getCode(), message));
    }

    /**
     * 处理参数约束异常。
     *
     * @param e 参数约束异常
     * @return 统一错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.error(ErrorCode.PARAM_ERROR.getCode(), e.getMessage()));
    }

    /**
     * 处理查询参数类型转换异常。
     *
     * @param e 查询参数类型转换异常
     * @return 统一错误响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "参数" + e.getName() + "格式不正确";
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.error(ErrorCode.PARAM_ERROR.getCode(), message));
    }

    /**
     * 处理请求体解析异常。
     *
     * @param e 请求体解析异常
     * @return 统一错误响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析异常: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.error(ErrorCode.PARAM_ERROR.getCode(), "请求体格式不正确或枚举值不受支持"));
    }

    /**
     * 处理未知异常。
     *
     * @param e 未知异常
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknownException(Exception e) {
        log.error("未知系统异常", e);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.getCode(), "系统繁忙，请稍后重试"));
    }
}
