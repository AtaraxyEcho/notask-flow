package com.notaskflow.controller;

import com.notaskflow.common.ApiResponse;
import com.notaskflow.domain.vo.AdminDashboardVO;
import com.notaskflow.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端数据大盘控制器。
 *
 * @author LIN
 */
@Tag(name = "管理端数据大盘")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * 查询数据大盘。
     *
     * @return 数据大盘
     */
    @Operation(summary = "查询数据大盘")
    @GetMapping
    public ApiResponse<AdminDashboardVO> overview() {
        return ApiResponse.success(adminDashboardService.overview());
    }
}
