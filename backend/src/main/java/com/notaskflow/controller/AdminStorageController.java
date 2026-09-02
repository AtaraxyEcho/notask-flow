package com.notaskflow.controller;

import com.notaskflow.common.ApiResponse;
import com.notaskflow.common.PageResponse;
import com.notaskflow.domain.vo.AdminOrphanCleanResultVO;
import com.notaskflow.domain.vo.AdminOrphanFileVO;
import com.notaskflow.domain.vo.AdminStorageRankVO;
import com.notaskflow.domain.vo.AdminStorageSummaryVO;
import com.notaskflow.service.AdminStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端存储管理控制器。
 *
 * @author LIN
 */
@Tag(name = "管理端存储管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/storage")
public class AdminStorageController {

    private final AdminStorageService adminStorageService;

    /**
     * 查询存储汇总。
     *
     * @return 存储汇总
     */
    @Operation(summary = "查询存储汇总")
    @GetMapping("/summary")
    public ApiResponse<AdminStorageSummaryVO> summary() {
        return ApiResponse.success(adminStorageService.summary());
    }

    /**
     * 查询用户存储排行。
     *
     * @return 用户存储排行
     */
    @Operation(summary = "查询用户存储排行")
    @GetMapping("/top-users")
    public ApiResponse<List<AdminStorageRankVO>> topUsers() {
        return ApiResponse.success(adminStorageService.topUsers());
    }

    /**
     * 查询空间存储排行。
     *
     * @return 空间存储排行
     */
    @Operation(summary = "查询空间存储排行")
    @GetMapping("/top-spaces")
    public ApiResponse<List<AdminStorageRankVO>> topSpaces() {
        return ApiResponse.success(adminStorageService.topSpaces());
    }

    /**
     * 扫描孤立文件。
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 孤立文件分页
     */
    @Operation(summary = "扫描孤立文件")
    @PostMapping("/orphan-files/scan")
    public ApiResponse<PageResponse<AdminOrphanFileVO>> scanOrphanFiles(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.success(adminStorageService.orphanFiles(pageNum, pageSize));
    }

    /**
     * 查询孤立文件。
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 孤立文件分页
     */
    @Operation(summary = "查询孤立文件")
    @GetMapping("/orphan-files")
    public ApiResponse<PageResponse<AdminOrphanFileVO>> orphanFiles(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.success(adminStorageService.orphanFiles(pageNum, pageSize));
    }

    /**
     * 清理孤立文件。
     *
     * @return 清理结果
     */
    @Operation(summary = "清理孤立文件")
    @PostMapping("/orphan-files/clean")
    public ApiResponse<AdminOrphanCleanResultVO> cleanOrphanFiles() {
        return ApiResponse.success(adminStorageService.cleanOrphanFiles());
    }
}
