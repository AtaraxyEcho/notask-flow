package com.notaskflow.controller;

import com.notaskflow.common.ApiResponse;
import com.notaskflow.domain.dto.request.SystemSettingUpdateRequest;
import com.notaskflow.domain.vo.SystemSettingVO;
import com.notaskflow.service.SystemSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端系统配置控制器。
 *
 * @author LIN
 */
@Tag(name = "管理端系统配置")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/settings")
public class AdminSettingController {

    private final SystemSettingService systemSettingService;

    /**
     * 查询全部系统设置。
     *
     * @return 系统设置列表
     */
    @Operation(summary = "查询系统设置")
    @GetMapping
    public ApiResponse<List<SystemSettingVO>> list() {
        return ApiResponse.success(systemSettingService.listSettings());
    }

    /**
     * 更新系统设置。
     *
     * @param request 系统设置更新请求
     * @return 系统设置列表
     */
    @Operation(summary = "更新系统设置")
    @PutMapping
    public ApiResponse<List<SystemSettingVO>> update(@Valid @RequestBody SystemSettingUpdateRequest request) {
        return ApiResponse.success(systemSettingService.updateSettings(request));
    }
}
