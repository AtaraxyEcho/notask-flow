package com.notaskflow.controller;

import com.notaskflow.common.ApiResponse;
import com.notaskflow.domain.vo.AdminSystemMonitorVO;
import com.notaskflow.service.AdminMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端性能监控控制器。
 *
 * @author LIN
 */
@Tag(name = "管理端性能监控")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/monitor")
public class AdminMonitorController {

    private final AdminMonitorService adminMonitorService;

    /**
     * 查询系统运行快照。
     *
     * @return 系统运行快照
     */
    @Operation(summary = "查询系统运行快照")
    @GetMapping
    public ApiResponse<AdminSystemMonitorVO> snapshot() {
        return ApiResponse.success(adminMonitorService.snapshot());
    }
}
