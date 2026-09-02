package com.notaskflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.notaskflow.common.ApiResponse;
import com.notaskflow.domain.vo.MemberTaskLoadVO;
import com.notaskflow.domain.vo.PersonalNoteTrendVO;
import com.notaskflow.domain.vo.PersonalStatsVO;
import com.notaskflow.domain.vo.RoleCompletionVO;
import com.notaskflow.domain.vo.StatsActivityVO;
import com.notaskflow.domain.vo.TaskTrendVO;
import com.notaskflow.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计控制器。
 *
 * @author LIN
 */
@Tag(name = "统计仪表盘")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class StatsController {

    private final StatsService statsService;

    /**
     * 查询个人统计。
     *
     * @return 个人统计
     */
    @Operation(summary = "查询个人统计")
    @GetMapping("/stats/personal")
    public ApiResponse<PersonalStatsVO> personal() {
        return ApiResponse.success(statsService.personal());
    }

    /**
     * 查询个人笔记趋势。
     *
     * @param days 天数
     * @return 笔记趋势
     */
    @Operation(summary = "查询个人笔记趋势")
    @GetMapping("/stats/personal/note-trend")
    public ApiResponse<List<PersonalNoteTrendVO>> personalNoteTrend(
            @RequestParam(defaultValue = "7") Integer days) {
        return ApiResponse.success(statsService.personalNoteTrend(days));
    }

    /**
     * 查询空间成员任务负载。
     *
     * @param spaceId 空间标识
     * @return 成员任务负载
     */
    @SaCheckPermission("space:stats:view")
    @Operation(summary = "查询成员任务负载")
    @GetMapping("/spaces/{spaceId}/stats/load")
    public ApiResponse<List<MemberTaskLoadVO>> load(@PathVariable Long spaceId) {
        return ApiResponse.success(statsService.load(spaceId));
    }

    /**
     * 查询任务完成趋势。
     *
     * @param spaceId 空间标识
     * @param days 天数
     * @return 完成趋势
     */
    @SaCheckPermission("space:stats:view")
    @Operation(summary = "查询任务完成趋势")
    @GetMapping("/spaces/{spaceId}/stats/trend")
    public ApiResponse<List<TaskTrendVO>> trend(@PathVariable Long spaceId,
                                                @RequestParam(defaultValue = "7") Integer days) {
        return ApiResponse.success(statsService.trend(spaceId, days));
    }

    /**
     * 查询按角色统计的完成数。
     *
     * @param spaceId 空间标识
     * @return 角色完成统计
     */
    @SaCheckPermission("space:stats:view")
    @Operation(summary = "按角色统计完成数")
    @GetMapping("/spaces/{spaceId}/stats/role-completion")
    public ApiResponse<List<RoleCompletionVO>> roleCompletion(@PathVariable Long spaceId) {
        return ApiResponse.success(statsService.roleCompletion(spaceId));
    }

    /**
     * 查询空间近期动态。
     *
     * @param spaceId 空间标识
     * @param limit 数量限制
     * @return 近期动态
     */
    @SaCheckPermission("space:stats:view")
    @Operation(summary = "查询近期动态")
    @GetMapping("/spaces/{spaceId}/stats/activities")
    public ApiResponse<List<StatsActivityVO>> recentActivities(@PathVariable Long spaceId,
                                                               @RequestParam(defaultValue = "10") Integer limit) {
        return ApiResponse.success(statsService.recentActivities(spaceId, limit));
    }
}
