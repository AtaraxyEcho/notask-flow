package com.notaskflow.controller;

import com.notaskflow.common.ApiResponse;
import com.notaskflow.common.enums.PushPlatform;
import com.notaskflow.domain.dto.request.DeviceTokenRegisterRequest;
import com.notaskflow.domain.vo.UserDeviceTokenVO;
import com.notaskflow.service.UserDeviceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户移动端推送设备令牌控制器。
 *
 * @author LIN
 */
@Validated
@Tag(name = "用户设备推送")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user/device-tokens")
public class UserDeviceTokenController {

    private final UserDeviceTokenService userDeviceTokenService;

    /**
     * 注册或刷新当前用户的设备推送令牌。
     *
     * @param request 注册请求
     * @return 设备推送令牌信息
     */
    @Operation(summary = "注册设备推送令牌")
    @PostMapping
    public ApiResponse<UserDeviceTokenVO> register(@Valid @RequestBody DeviceTokenRegisterRequest request) {
        return ApiResponse.success(userDeviceTokenService.registerCurrent(request));
    }

    /**
     * 查询当前用户指定设备的推送令牌状态。
     *
     * @param platform 推送平台
     * @param deviceId 设备标识
     * @return 设备推送令牌信息
     */
    @Operation(summary = "查询当前设备推送令牌")
    @GetMapping("/current")
    public ApiResponse<UserDeviceTokenVO> current(
            @NotNull(message = "推送平台不能为空") @RequestParam PushPlatform platform,
            @NotBlank(message = "设备标识不能为空")
            @Size(max = 100, message = "设备标识长度不能超过100")
            @RequestParam String deviceId) {
        return ApiResponse.success(userDeviceTokenService.current(platform, deviceId));
    }

    /**
     * 解绑当前用户的设备推送令牌。
     *
     * @param platform 推送平台
     * @param deviceId 设备标识
     * @return 空响应
     */
    @Operation(summary = "解绑设备推送令牌")
    @DeleteMapping
    public ApiResponse<Void> unbind(
            @NotNull(message = "推送平台不能为空") @RequestParam PushPlatform platform,
            @NotBlank(message = "设备标识不能为空")
            @Size(max = 100, message = "设备标识长度不能超过100")
            @RequestParam String deviceId) {
        userDeviceTokenService.unbindCurrent(platform, deviceId);
        return ApiResponse.success();
    }
}
