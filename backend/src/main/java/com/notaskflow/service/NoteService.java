package com.notaskflow.service;

import com.notaskflow.common.PageResponse;
import com.notaskflow.domain.dto.request.CollabContentSaveRequest;
import com.notaskflow.domain.dto.request.NoteSaveRequest;
import com.notaskflow.domain.dto.request.NoteShareRequest;
import com.notaskflow.domain.query.NoteQuery;
import com.notaskflow.domain.vo.CollabTicketConsumeVO;
import com.notaskflow.domain.vo.CollabTicketVO;
import com.notaskflow.domain.vo.NoteHistoryVO;
import com.notaskflow.domain.vo.NoteVO;
import java.util.List;

/**
 * 笔记服务接口。
 *
 * @author LIN
 */
public interface NoteService {

    /**
     * 分页查询笔记。
     *
     * @param spaceId 空间标识
     * @param query 查询条件
     * @return 分页笔记
     */
    PageResponse<NoteVO> page(Long spaceId, NoteQuery query);

    /**
     * 查询笔记详情。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     * @return 笔记详情
     */
    NoteVO get(Long spaceId, Long id);

    /**
     * 创建笔记。
     *
     * @param spaceId 空间标识
     * @param request 保存请求
     * @return 笔记详情
     */
    NoteVO create(Long spaceId, NoteSaveRequest request);

    /**
     * 更新笔记并生成版本快照。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     * @param request 保存请求
     * @return 笔记详情
     */
    NoteVO update(Long spaceId, Long id, NoteSaveRequest request);

    /**
     * 签发协作文档一次性 Ticket。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     * @return 协作 Ticket
     */
    CollabTicketVO createCollabTicket(Long spaceId, Long id);

    /**
     * 消费协作文档一次性 Ticket。
     *
     * @param ticket 协作 Ticket
     * @return Ticket 消费结果
     */
    CollabTicketConsumeVO consumeCollabTicket(String ticket);

    /**
     * 保存协作文档正文，不生成历史版本。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     * @param request 协同正文保存请求
     * @return 笔记详情
     */
    NoteVO saveCollabContent(Long spaceId, Long id, CollabContentSaveRequest request);

    /**
     * 创建协作文档正文检查点并生成历史版本。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     * @param request 协同正文保存请求
     * @return 笔记详情
     */
    NoteVO createCheckpoint(Long spaceId, Long id, CollabContentSaveRequest request);

    /**
     * 删除笔记。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     */
    void delete(Long spaceId, Long id);

    /**
     * 查询笔记历史版本。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     * @return 历史版本列表
     */
    List<NoteHistoryVO> listHistory(Long spaceId, Long id);

    /**
     * 查询指定历史版本。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     * @param version 版本号
     * @return 历史版本
     */
    NoteHistoryVO getHistory(Long spaceId, Long id, Integer version);

    /**
     * 回滚到指定历史版本。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     * @param version 版本号
     * @return 笔记详情
     */
    NoteVO restore(Long spaceId, Long id, Integer version);

    /**
     * 生成或刷新分享码。
     *
     * @param spaceId 空间标识
     * @param id 笔记标识
     * @param request 分享请求
     * @return 笔记详情
     */
    NoteVO share(Long spaceId, Long id, NoteShareRequest request);

    /**
     * 公开访问笔记。
     *
     * @param shareCode 分享码
     * @return 笔记详情
     */
    NoteVO publicAccess(String shareCode);

    /**
     * 搜索笔记。
     *
     * @param spaceId 空间标识
     * @param keyword 搜索关键词
     * @return 笔记列表
     */
    List<NoteVO> search(Long spaceId, String keyword);
}
