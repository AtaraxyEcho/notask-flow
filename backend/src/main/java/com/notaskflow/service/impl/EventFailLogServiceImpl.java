package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notaskflow.common.PageResponse;
import com.notaskflow.common.constant.RedisKeyConstants;
import com.notaskflow.common.enums.EventFailStatus;
import com.notaskflow.domain.entity.EventFailLog;
import com.notaskflow.domain.query.EventFailLogQuery;
import com.notaskflow.domain.vo.EventFailLogVO;
import com.notaskflow.exception.BusinessException;
import com.notaskflow.exception.ErrorCode;
import com.notaskflow.mapper.EventFailLogMapper;
import com.notaskflow.mq.producer.EventFailRetryPublisher;
import com.notaskflow.service.EventFailLogService;
import com.notaskflow.utils.RedisUtil;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 事件失败补偿服务实现，记录并重试事务后异步处理失败的事件。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventFailLogServiceImpl implements EventFailLogService {

    private static final Duration RETRY_LOCK_TTL = Duration.ofMinutes(5);

    private final EventFailLogMapper eventFailLogMapper;

    private final ObjectMapper objectMapper;

    private final EventFailRetryPublisher eventFailRetryPublisher;

    private final RedisUtil redisUtil;

    /**
     * 分页查询事件失败日志。
     *
     * @param query 查询条件
     * @return 事件失败日志分页
     */
    @Override
    public PageResponse<EventFailLogVO> page(EventFailLogQuery query) {
        Page<EventFailLog> page = new Page<>(query.safePageNum(), query.safePageSize());
        Page<EventFailLog> result = eventFailLogMapper.selectPage(page, Wrappers.<EventFailLog>lambdaQuery()
                .eq(query.getStatus() != null, EventFailLog::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getEventType()), EventFailLog::getEventType, query.getEventType())
                .orderByDesc(EventFailLog::getGmtCreate));
        List<EventFailLogVO> list = result.getRecords().stream()
                .map(this::toVO)
                .toList();
        return PageResponse.of(result, list);
    }

    /**
     * 查询事件失败日志详情。
     *
     * @param id 事件失败日志标识
     * @return 事件失败日志
     */
    @Override
    public EventFailLogVO get(Long id) {
        return toVO(requireFailLog(id));
    }

    /**
     * 记录事件处理失败信息。
     *
     * @param eventType 事件类型
     * @param eventData 事件数据
     * @param failReason 失败原因
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordFailure(String eventType, Object eventData, String failReason) {
        EventFailLog failLog = new EventFailLog();
        failLog.setEventType(eventType);
        failLog.setEventData(serialize(eventData));
        failLog.setFailReason(failReason);
        failLog.setRetryCount(0);
        failLog.setStatus(EventFailStatus.PENDING);
        eventFailLogMapper.insert(failLog);
        log.warn("事件失败日志已记录，eventType={}，failLogId={}，reason={}",
                eventType, failLog.getId(), failReason);
    }

    /**
     * 手动重试单条失败事件。
     *
     * @param id 事件失败日志标识
     * @param maxRetryCount 最大重试次数
     * @return 重试后的事件失败日志
     */
    @Override
    public EventFailLogVO retryFailure(Long id, int maxRetryCount) {
        EventFailLog failLog = requireFailLog(id);
        if (EventFailStatus.SUCCESS.equals(failLog.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "事件已处理成功，无需重试");
        }
        int safeMaxRetryCount = Math.max(1, maxRetryCount);
        if (resolveRetryCount(failLog) >= safeMaxRetryCount) {
            throw new BusinessException(ErrorCode.CONFLICT, "事件重试次数已达到上限");
        }
        retryFailureLog(failLog, safeMaxRetryCount);
        return get(id);
    }

    /**
     * 批量重试待补偿的失败事件。
     *
     * @param batchSize 批处理数量
     * @param maxRetryCount 最大重试次数
     * @return 本次成功重新投递的事件数量
     */
    @Override
    public int retryPendingFailures(int batchSize, int maxRetryCount) {
        int safeBatchSize = Math.max(1, batchSize);
        int safeMaxRetryCount = Math.max(1, maxRetryCount);
        String lockOwner = UUID.randomUUID().toString();
        boolean locked = redisUtil.tryLock(RedisKeyConstants.EVENT_FAIL_RETRY_LOCK, lockOwner, RETRY_LOCK_TTL);
        if (!locked) {
            log.info("事件失败补偿任务已在其他节点执行，本次跳过");
            return 0;
        }
        try {
            List<EventFailLog> failLogs = queryRetryableFailures(safeBatchSize, safeMaxRetryCount);
            int retried = 0;
            for (EventFailLog failLog : failLogs) {
                if (retryFailureLog(failLog, safeMaxRetryCount)) {
                    retried++;
                }
            }
            return retried;
        } finally {
            redisUtil.unlock(RedisKeyConstants.EVENT_FAIL_RETRY_LOCK, lockOwner);
        }
    }

    private List<EventFailLog> queryRetryableFailures(int batchSize, int maxRetryCount) {
        Page<EventFailLog> page = new Page<>(1, batchSize);
        return eventFailLogMapper.selectPage(page, Wrappers.<EventFailLog>lambdaQuery()
                        .in(EventFailLog::getStatus, EventFailStatus.PENDING, EventFailStatus.FAILED)
                        .lt(EventFailLog::getRetryCount, maxRetryCount)
                        .orderByAsc(EventFailLog::getGmtCreate))
                .getRecords();
    }

    private boolean retryFailureLog(EventFailLog failLog, int maxRetryCount) {
        int nextRetryCount = resolveRetryCount(failLog) + 1;
        if (!claimFailureLog(failLog, nextRetryCount, maxRetryCount)) {
            return false;
        }
        try {
            eventFailRetryPublisher.publish(failLog);
            markSuccess(failLog.getId());
            log.info("事件失败补偿已重新投递，failLogId={}, eventType={}, retryCount={}",
                    failLog.getId(), failLog.getEventType(), nextRetryCount);
            return true;
        } catch (RuntimeException exception) {
            markRetryFailed(failLog.getId(), nextRetryCount, exception);
            log.warn("事件失败补偿重新投递失败，failLogId={}, eventType={}, retryCount={}",
                    failLog.getId(), failLog.getEventType(), nextRetryCount, exception);
            return false;
        }
    }

    private boolean claimFailureLog(EventFailLog failLog, int nextRetryCount, int maxRetryCount) {
        LambdaUpdateWrapper<EventFailLog> updateWrapper = Wrappers.<EventFailLog>lambdaUpdate()
                .eq(EventFailLog::getId, failLog.getId())
                .eq(EventFailLog::getStatus, failLog.getStatus())
                .lt(EventFailLog::getRetryCount, maxRetryCount)
                .set(EventFailLog::getStatus, EventFailStatus.RETRYING)
                .set(EventFailLog::getRetryCount, nextRetryCount);
        return eventFailLogMapper.update(null, updateWrapper) > 0;
    }

    private void markSuccess(Long failLogId) {
        eventFailLogMapper.update(null, Wrappers.<EventFailLog>lambdaUpdate()
                .eq(EventFailLog::getId, failLogId)
                .set(EventFailLog::getStatus, EventFailStatus.SUCCESS));
    }

    private void markRetryFailed(Long failLogId, int retryCount, RuntimeException exception) {
        eventFailLogMapper.update(null, Wrappers.<EventFailLog>lambdaUpdate()
                .eq(EventFailLog::getId, failLogId)
                .set(EventFailLog::getStatus, EventFailStatus.FAILED)
                .set(EventFailLog::getRetryCount, retryCount)
                .set(EventFailLog::getFailReason, resolveFailReason(exception)));
    }

    private int resolveRetryCount(EventFailLog failLog) {
        Integer retryCount = failLog.getRetryCount();
        if (retryCount == null) {
            return 0;
        }
        return retryCount;
    }

    private String resolveFailReason(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }

    private EventFailLog requireFailLog(Long id) {
        EventFailLog failLog = eventFailLogMapper.selectById(id);
        if (failLog == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "事件失败日志不存在");
        }
        return failLog;
    }

    private EventFailLogVO toVO(EventFailLog failLog) {
        EventFailLogVO vo = new EventFailLogVO();
        vo.setId(failLog.getId());
        vo.setEventType(failLog.getEventType());
        vo.setEventData(failLog.getEventData());
        vo.setFailReason(failLog.getFailReason());
        vo.setRetryCount(failLog.getRetryCount());
        vo.setStatus(failLog.getStatus());
        vo.setGmtCreate(failLog.getGmtCreate());
        vo.setGmtModified(failLog.getGmtModified());
        return vo;
    }

    private String serialize(Object eventData) {
        try {
            return objectMapper.writeValueAsString(eventData);
        } catch (JsonProcessingException exception) {
            log.warn("事件数据序列化失败", exception);
            return "{\"serializeError\":\"" + exception.getMessage() + "\"}";
        }
    }
}
