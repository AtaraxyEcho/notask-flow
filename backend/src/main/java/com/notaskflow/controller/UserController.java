package com.notaskflow.controller;

import com.notaskflow.common.ApiResponse;
import com.notaskflow.domain.dto.request.EmailChangeCodeRequest;
import com.notaskflow.domain.dto.request.EmailChangeConfirmRequest;
import com.notaskflow.domain.dto.request.NotificationSettingUpdateRequest;
import com.notaskflow.domain.dto.request.PasswordUpdateRequest;
import com.notaskflow.domain.dto.request.ProfileUpdateRequest;
import com.notaskflow.domain.vo.NotificationSettingVO;
import com.notaskflow.domain.vo.UserOptionVO;
import com.notaskflow.domain.vo.UserProfileVO;
import com.notaskflow.service.NotificationSettingService;
import com.notaskflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户资料控制器。
 *
 * @author LIN
 */
@Tag(name = "用户资料")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    private final NotificationSettingService notificationSettingService;

    /**
     * 获取当前用户资料。
     *
     * @return 用户资料
     */
    @Operation(summary = "获取当前用户资料")
    @GetMapping("/profile")
    public ApiResponse<UserProfileVO> profile() {
        return ApiResponse.success(userService.profile());
    }

    /**
     * 根据用户名或邮箱关键字搜索用户。
     *
     * @param keyword 用户名或邮箱关键字
     * @return 用户选择项列表
     */
    @Operation(summary = "搜索用户")
    @GetMapping("/search")
    public ApiResponse<List<UserOptionVO>> search(@RequestParam String keyword) {
        return ApiResponse.success(userService.search(keyword));
    }

    /**
     * 更新当前用户资料。
     *
     * @param request 更新请求
     * @return 用户资料
     */
    @Operation(summary = "更新当前用户资料")
    @PutMapping("/profile")
    public ApiResponse<UserProfileVO> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.success(userService.updateProfile(request));
    }

    /**
     * 发送邮箱修改验证码到当前旧邮箱。
     *
     * @param request 验证码发送请求
     * @return 空响应
     */
    @Operation(summary = "发送邮箱修改验证码")
    @PostMapping("/email/code")
    public ApiResponse<Void> sendEmailChangeCode(@Valid @RequestBody EmailChangeCodeRequest request) {
        userService.sendEmailChangeCode(request);
        return ApiResponse.success();
    }

    /**
     * 校验旧邮箱验证码并修改邮箱。
     *
     * @param request 邮箱修改确认请求
     * @return 用户资料
     */
    @Operation(summary = "修改邮箱")
    @PutMapping("/email")
    public ApiResponse<UserProfileVO> changeEmail(@Valid @RequestBody EmailChangeConfirmRequest request) {
        return ApiResponse.success(userService.changeEmail(request));
    }

    /**
     * 上传当前用户头像。
     *
     * @param file 头像文件
     * @return 用户资料
     */
    @Operation(summary = "上传用户头像")
    @PostMapping("/avatar")
    public ApiResponse<UserProfileVO> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(userService.uploadAvatar(file));
    }

    /**
     * 获取当前用户通知偏好。
     *
     * @return 通知偏好
     */
    @Operation(summary = "获取当前用户通知偏好")
    @GetMapping("/notification-settings")
    public ApiResponse<NotificationSettingVO> notificationSettings() {
        return ApiResponse.success(notificationSettingService.getCurrentSetting());
    }

    /**
     * 更新当前用户通知偏好。
     *
     * @param request 通知偏好更新请求
     * @return 通知偏好
     */
    @Operation(summary = "更新当前用户通知偏好")
    @PutMapping("/notification-settings")
    public ApiResponse<NotificationSettingVO> updateNotificationSettings(
            @Valid @RequestBody NotificationSettingUpdateRequest request) {
        return ApiResponse.success(notificationSettingService.updateCurrentSetting(request));
    }

    /**
     * 修改当前用户密码。
     *
     * @param request 密码修改请求
     * @return 空响应
     */
    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordUpdateRequest request) {
        userService.changePassword(request);
        return ApiResponse.success();
    }
}
