package com.notaskflow.service;

import com.notaskflow.common.PageResponse;
import com.notaskflow.domain.query.EventFailLogQuery;
import com.notaskflow.domain.vo.EventFailLogVO;

/**
 * 事件失败补偿服务接口。
 *
 * @author LIN
 */
public interface EventFailLogService {

    /**
     * 分页查询事件失败日志。
     *
     * @param query 查询条件
     * @return 事件失败日志分页
     */
    PageResponse<EventFailLogVO> page(EventFailLogQuery query);

    /**
     * 查询事件失败日志详情。
     *
     * @param id 事件失败日志标识
     * @return 事件失败日志
     */
    EventFailLogVO get(Long id);

    /**
     * 记录事件处理失败信息。
     *
     * @param eventType 事件类型
     * @param eventData 事件数据
     * @param failReason 失败原因
     */
    void recordFailure(String eventType, Object eventData, String failReason);

    /**
     * 手动重试单条失败事件。
     *
     * @param id 事件失败日志标识
     * @param maxRetryCount 最大重试次数
     * @return 重试后的事件失败日志
     */
    EventFailLogVO retryFailure(Long id, int maxRetryCount);

    /**
     * 批量重试待补偿的失败事件。
     *
     * @param batchSize 批处理数量
     * @param maxRetryCount 最大重试次数
     * @return 本次成功重新投递的事件数量
     */
    int retryPendingFailures(int batchSize, int maxRetryCount);
}
