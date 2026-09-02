package com.notaskflow.controller;

import com.notaskflow.common.ApiResponse;
import com.notaskflow.domain.dto.request.SpaceJoinApplyRequest;
import com.notaskflow.domain.dto.request.SpaceJoinApproveRequest;
import com.notaskflow.domain.dto.request.SpaceJoinRejectRequest;
import com.notaskflow.domain.vo.SpaceJoinRequestVO;
import com.notaskflow.service.SpaceJoinRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 团队加入申请控制器。
 *
 * @author LIN
 */
@Tag(name = "团队加入申请")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/team-applications")
public class SpaceJoinRequestController {

    private final SpaceJoinRequestService spaceJoinRequestService;

    /**
     * 创建团队加入申请。
     *
     * @param request 创建请求
     * @return 申请信息
     */
    @Operation(summary = "创建团队加入申请")
    @PostMapping
    public ApiResponse<SpaceJoinRequestVO> apply(@Valid @RequestBody SpaceJoinApplyRequest request) {
        return ApiResponse.success(spaceJoinRequestService.apply(request));
    }

    /**
     * 查询当前用户提交的申请。
     *
     * @return 申请列表
     */
    @Operation(summary = "查询我的团队加入申请")
    @GetMapping("/mine")
    public ApiResponse<List<SpaceJoinRequestVO>> listMine() {
        return ApiResponse.success(spaceJoinRequestService.listMine());
    }

    /**
     * 查询当前用户待审批的申请。
     *
     * @return 申请列表
     */
    @Operation(summary = "查询待我审批的团队加入申请")
    @GetMapping("/pending")
    public ApiResponse<List<SpaceJoinRequestVO>> listPendingForMe() {
        return ApiResponse.success(spaceJoinRequestService.listPendingForMe());
    }

    /**
     * 审批通过团队加入申请。
     *
     * @param requestId 申请标识
     * @param request 审批请求
     * @return 申请信息
     */
    @Operation(summary = "审批通过团队加入申请")
    @PostMapping("/{requestId}/approve")
    public ApiResponse<SpaceJoinRequestVO> approve(@PathVariable Long requestId,
                                                   @Valid @RequestBody SpaceJoinApproveRequest request) {
        return ApiResponse.success(spaceJoinRequestService.approve(requestId, request));
    }

    /**
     * 拒绝团队加入申请。
     *
     * @param requestId 申请标识
     * @param request 拒绝请求
     * @return 申请信息
     */
    @Operation(summary = "拒绝团队加入申请")
    @PostMapping("/{requestId}/reject")
    public ApiResponse<SpaceJoinRequestVO> reject(@PathVariable Long requestId,
                                                  @Valid @RequestBody SpaceJoinRejectRequest request) {
        return ApiResponse.success(spaceJoinRequestService.reject(requestId, request));
    }
}
