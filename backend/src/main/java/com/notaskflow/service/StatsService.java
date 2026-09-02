package com.notaskflow.service;

import com.notaskflow.domain.vo.MemberTaskLoadVO;
import com.notaskflow.domain.vo.PersonalNoteTrendVO;
import com.notaskflow.domain.vo.PersonalStatsVO;
import com.notaskflow.domain.vo.RoleCompletionVO;
import com.notaskflow.domain.vo.StatsActivityVO;
import com.notaskflow.domain.vo.TaskTrendVO;
import java.util.List;

/**
 * 统计服务接口。
 *
 * @author LIN
 */
public interface StatsService {

    /**
     * 查询当前用户个人统计。
     *
     * @return 个人统计
     */
    PersonalStatsVO personal();

    /**
     * 查询个人笔记创建与编辑趋势。
     *
     * @param days 天数
     * @return 趋势列表
     */
    List<PersonalNoteTrendVO> personalNoteTrend(Integer days);

    /**
     * 查询空间成员任务负载。
     *
     * @param spaceId 空间标识
     * @return 成员任务负载
     */
    List<MemberTaskLoadVO> load(Long spaceId);

    /**
     * 查询任务完成趋势。
     *
     * @param spaceId 空间标识
     * @param days 天数
     * @return 完成趋势
     */
    List<TaskTrendVO> trend(Long spaceId, Integer days);

    /**
     * 查询按角色统计的完成数。
     *
     * @param spaceId 空间标识
     * @return 角色完成统计
     */
    List<RoleCompletionVO> roleCompletion(Long spaceId);

    /**
     * 查询空间近期动态。
     *
     * @param spaceId 空间标识
     * @param limit 数量限制
     * @return 近期动态列表
     */
    List<StatsActivityVO> recentActivities(Long spaceId, Integer limit);
}
