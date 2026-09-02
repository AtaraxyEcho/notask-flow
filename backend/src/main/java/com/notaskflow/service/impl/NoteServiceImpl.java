package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notaskflow.common.PageResponse;
import com.notaskflow.common.constant.RedisKeyConstants;
import com.notaskflow.common.enums.NoteHistorySaveType;
import com.notaskflow.common.enums.SearchIndexOperation;
import com.notaskflow.common.enums.SpaceRealtimeEventType;
import com.notaskflow.common.enums.SpaceType;
import com.notaskflow.domain.dto.request.CollabContentSaveRequest;
import com.notaskflow.domain.dto.request.NoteSaveRequest;
import com.notaskflow.domain.dto.request.NoteShareRequest;
import com.notaskflow.domain.entity.Note;
import com.notaskflow.domain.entity.NoteHistory;
import com.notaskflow.domain.entity.NoteTag;
import com.notaskflow.domain.entity.Project;
import com.notaskflow.domain.entity.Space;
import com.notaskflow.domain.entity.Tag;
import com.notaskflow.domain.entity.User;
import com.notaskflow.domain.query.NoteQuery;
import com.notaskflow.domain.vo.CollabTicketConsumeVO;
import com.notaskflow.domain.vo.CollabTicketVO;
import com.notaskflow.domain.vo.NoteHistoryVO;
import com.notaskflow.domain.vo.NoteVO;
import com.notaskflow.domain.vo.TagVO;
import com.notaskflow.event.SearchIndexRequestedEvent;
import com.notaskflow.exception.AccessDeniedException;
import com.notaskflow.exception.BusinessException;
import com.notaskflow.exception.ErrorCode;
import com.notaskflow.exception.ResourceNotFoundException;
import com.notaskflow.mapper.NoteHistoryMapper;
import com.notaskflow.mapper.NoteMapper;
import com.notaskflow.mapper.NoteTagMapper;
import com.notaskflow.mapper.ProjectMapper;
import com.notaskflow.mapper.SpaceMapper;
import com.notaskflow.mapper.TagMapper;
import com.notaskflow.mapper.UserMapper;
import com.notaskflow.security.PermissionValidator;
import com.notaskflow.service.NoteService;
import com.notaskflow.service.NoteSearchService;
import com.notaskflow.service.SpaceRealtimeEventService;
import com.notaskflow.service.SystemSettingService;
import com.notaskflow.utils.HtmlSanitizerUtil;
import com.notaskflow.utils.LoginUserUtil;
import com.notaskflow.utils.RedisUtil;
import com.notaskflow.utils.StringGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 笔记服务实现类
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    /** 创建快照失败时最大重试次数 */
    private static final int MAX_SNAPSHOT_RETRY = 3;

    /** 更新时使用的FOR UPDATE锁 */
    private static final String LOCK_FOR_UPDATE_SQL = "FOR UPDATE";

    /** 分享码长度 */
    private static final int SHARE_CODE_LENGTH = 16;

    /** 生成分享码最大重试次数 */
    private static final int MAX_SHARE_CODE_RETRY = 5;

    /** 协作Ticket值分隔符 */
    private static final String COLLAB_TICKET_VALUE_DELIMITER = ":";

    /** 创建笔记历史快照时的分布式锁有效期。 */
    private static final Duration SNAPSHOT_LOCK_TTL = Duration.ofSeconds(10);

    private final NoteMapper noteMapper;
    private final NoteHistoryMapper noteHistoryMapper;
    private final NoteTagMapper noteTagMapper;
    private final ProjectMapper projectMapper;
    private final TagMapper tagMapper;
    private final UserMapper userMapper;
    private final SpaceMapper spaceMapper;
    private final PermissionValidator permissionValidator;
    private final RedisUtil redisUtil;
    private final NoteSearchService noteSearchService;
    private final SystemSettingService systemSettingService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final SpaceRealtimeEventService spaceRealtimeEventService;
    private final HtmlSanitizerUtil htmlSanitizerUtil;

    // ============================ 公开方法 ============================

    /**
     * 分页查询笔记列表
     *
     * @param spaceId 空间ID
     * @param query   查询条件
     * @return 分页结果
     */
    @Override
    public PageResponse<NoteVO> page(Long spaceId, NoteQuery query) {
        permissionValidator.ensureSpaceMember(spaceId, LoginUserUtil.currentUserId());
        Page<Note> page = new Page<>(query.safePageNum(), query.safePageSize());
        List<Long> noteIds = findNoteIdsByTag(query.getTagId());
        if (query.getTagId() != null && noteIds.isEmpty()) {
            return PageResponse.of(page, Collections.emptyList());
        }
        LambdaQueryWrapper<Note> wrapper = Wrappers.<Note>lambdaQuery()
                .eq(Note::getSpaceId, spaceId)
                .eq(query.getNotebookId() != null, Note::getNotebookId, query.getNotebookId())
                .eq(query.getProjectId() != null, Note::getProjectId, query.getProjectId())
                .in(!noteIds.isEmpty(), Note::getId, noteIds)
                .and(StringUtils.hasText(query.getKeyword()), condition -> condition
                        .like(Note::getTitle, query.getKeyword())
                        .or()
                        .like(Note::getContent, query.getKeyword()))
                .orderByDesc(Note::getGmtModified);
        IPage<Note> result = noteMapper.selectPage(page, wrapper);
        List<NoteVO> list = result.getRecords().stream()
                .map(note -> toVO(note, canCurrentUserEditNote(note), isTeamSpace(note.getSpaceId())))
                .toList();
        return PageResponse.of(page, list);
    }

    /**
     * 获取单个笔记详情
     *
     * @param spaceId 空间ID
     * @param id      笔记ID
     * @return 笔记VO
     */
    @Override
    public NoteVO get(Long spaceId, Long id) {
        permissionValidator.ensureSpaceMember(spaceId, LoginUserUtil.currentUserId());
        Note note = findNote(spaceId, id);
        increaseViewCount(note.getId());
        return toVO(note, canCurrentUserEditNote(note), isTeamSpace(spaceId));
    }

    /**
     * 创建笔记
     *
     * @param spaceId 空间ID
     * @param request 创建请求
     * @return 新建笔记VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteVO create(Long spaceId, NoteSaveRequest request) {
        permissionValidator.ensureSpaceMember(spaceId, LoginUserUtil.currentUserId());
        Note note = new Note();
        note.setSpaceId(spaceId);
        note.setNotebookId(request.getNotebookId());
        note.setProjectId(validateProjectInSpace(spaceId, request.getProjectId()));
        note.setUserId(LoginUserUtil.currentUserId());
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setContentHtml(sanitizeContentHtml(request.getContentHtml()));
        note.setIsPublic(Boolean.TRUE.equals(request.getIsPublic()));
        note.setViewCount(0);
        noteMapper.insert(note);
        syncTags(spaceId, note.getId(), request.getTagIds());
        publishSearchIndexUpsert(note.getId());
        publishDocumentRealtimeEvent(spaceId, SpaceRealtimeEventType.DOCUMENT_CREATED, note);
        log.info("创建笔记成功: spaceId={}, noteId={}, userId={}",
                spaceId, note.getId(), LoginUserUtil.currentUserId());
        return toVO(note, true, isTeamSpace(spaceId));
    }

    /**
     * 更新笔记
     *
     * @param spaceId 空间ID
     * @param id      笔记ID
     * @param request 更新请求
     * @return 更新后的笔记VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteVO update(Long spaceId, Long id, NoteSaveRequest request) {
        request.setContentHtml(sanitizeContentHtml(request.getContentHtml()));
        Note note = findNoteForUpdate(spaceId, id);
        ensureNoteWritePermission(note, LoginUserUtil.currentUserId());
        if (shouldCreateSnapshot(note, request)) {
            createSnapshot(note, resolveHistorySaveType(request.getSaveType()), request.getTitle(), request.getContent());
        }
        note.setNotebookId(request.getNotebookId());
        note.setProjectId(validateProjectInSpace(spaceId, request.getProjectId()));
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setContentHtml(request.getContentHtml());
        note.setIsPublic(Boolean.TRUE.equals(request.getIsPublic()));
        noteMapper.updateById(note);
        syncTags(spaceId, note.getId(), request.getTagIds());
        publishSearchIndexUpsert(note.getId());
        publishDocumentRealtimeEvent(spaceId, SpaceRealtimeEventType.DOCUMENT_UPDATED, note);
        cleanOldHistories(note.getId());
        log.info("更新笔记成功: spaceId={}, noteId={}, userId={}",
                spaceId, id, LoginUserUtil.currentUserId());
        return toVO(note, true, isTeamSpace(spaceId));
    }

    /**
     * 创建协作Ticket
     *
     * @param spaceId 空间ID
     * @param id      笔记ID
     * @return Ticket信息
     */
    @Override
    public CollabTicketVO createCollabTicket(Long spaceId, Long id) {
        ensureTeamCollaborationEnabled(spaceId);
        findNote(spaceId, id);
        permissionValidator.ensureSpaceCollaborator(spaceId, LoginUserUtil.currentUserId());
        String ticket = StringGenerator.randomAlphanumeric(32);
        int ticketExpireSeconds = systemSettingService.getCollabTicketExpireSeconds();
        redisUtil.set(
                collabTicketKey(ticket),
                buildCollabTicketValue(spaceId, id, LoginUserUtil.currentUserId()),
                Duration.ofSeconds(ticketExpireSeconds));
        return new CollabTicketVO(ticket, ticketExpireSeconds);
    }

    /**
     * 消费协作Ticket（供WebSocket服务调用）
     *
     * @param ticket Ticket字符串
     * @return 消费结果，包含用户信息、笔记内容等
     */
    @Override
    public CollabTicketConsumeVO consumeCollabTicket(String ticket) {
        String ticketValue = redisUtil.getAndDeleteString(collabTicketKey(ticket));
        if (!StringUtils.hasText(ticketValue)) {
            return new CollabTicketConsumeVO(false, null, null, null, null, null, null, false, null);
        }
        try {
            String[] parts = ticketValue.split(COLLAB_TICKET_VALUE_DELIMITER);
            if (parts.length != 3) {
                log.warn("Ticket格式无效: {}", ticket);
                return new CollabTicketConsumeVO(false, null, null, null, null, null, null, false, null);
            }
            Long spaceId = Long.valueOf(parts[0]);
            Long noteId = Long.valueOf(parts[1]);
            Long userId = Long.valueOf(parts[2]);
            ensureTeamCollaborationEnabled(spaceId);
            Note note = findNote(spaceId, noteId);
            permissionValidator.ensureSpaceCollaborator(spaceId, userId);
            User user = userMapper.selectById(userId);
            String bootstrapContent = StringUtils.hasText(note.getContentHtml())
                    ? sanitizeContentHtml(note.getContentHtml())
                    : note.getContent();
            return new CollabTicketConsumeVO(
                    true,
                    spaceId,
                    noteId,
                    userId,
                    user == null ? null : user.getUsername(),
                    user == null ? null : user.getNickname(),
                    user == null ? null : user.getAvatarUrl(),
                    true,
                    bootstrapContent);
        } catch (BusinessException | NumberFormatException exception) {
            log.warn("Ticket消费失败: {}", ticket, exception);
            return new CollabTicketConsumeVO(false, null, null, null, null, null, null, false, null);
        }
    }

    /**
     * 保存协作编辑后的正文（不生成历史版本）
     *
     * @param spaceId 空间ID
     * @param id      笔记ID
     * @param request 请求体（含新正文）
     * @return 更新后的笔记VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteVO saveCollabContent(Long spaceId, Long id, CollabContentSaveRequest request) {
        request.setContentHtml(sanitizeContentHtml(request.getContentHtml()));
        Note note = findNoteForUpdate(spaceId, id);
        ensureTeamCollaborationEnabled(spaceId);
        ensureNoteWritePermission(note, LoginUserUtil.currentUserId());
        if (Objects.equals(note.getContent(), request.getContent())
                && Objects.equals(note.getContentHtml(), request.getContentHtml())) {
            return toVO(note, true, isTeamSpace(spaceId));
        }
        note.setContent(request.getContent());
        note.setContentHtml(request.getContentHtml());
        noteMapper.updateById(note);
        publishSearchIndexUpsert(note.getId());
        publishDocumentRealtimeEvent(spaceId, SpaceRealtimeEventType.DOCUMENT_UPDATED, note);
        log.info("保存协作正文成功: spaceId={}, noteId={}, userId={}",
                spaceId, id, LoginUserUtil.currentUserId());
        return toVO(note, true, isTeamSpace(spaceId));
    }

    /**
     * 创建检查点（生成历史版本）
     *
     * @param spaceId 空间ID
     * @param id      笔记ID
     * @param request 请求体（含新正文）
     * @return 更新后的笔记VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteVO createCheckpoint(Long spaceId, Long id, CollabContentSaveRequest request) {
        request.setContentHtml(sanitizeContentHtml(request.getContentHtml()));
        Note note = findNoteForUpdate(spaceId, id);
        ensureTeamCollaborationEnabled(spaceId);
        ensureNoteWritePermission(note, LoginUserUtil.currentUserId());
        boolean contentChanged = !Objects.equals(note.getContent(), request.getContent())
                || !Objects.equals(note.getContentHtml(), request.getContentHtml());
        if (contentChanged) {
            note.setContent(request.getContent());
            note.setContentHtml(request.getContentHtml());
            noteMapper.updateById(note);
            publishSearchIndexUpsert(note.getId());
            publishDocumentRealtimeEvent(spaceId, SpaceRealtimeEventType.DOCUMENT_UPDATED, note);
        }
        if (latestHistoryMatches(note.getId(), note.getTitle(), note.getContent())) {
            return toVO(note, true, isTeamSpace(spaceId));
        }
        createSnapshot(note, NoteHistorySaveType.CHECKPOINT, note.getTitle(), note.getContent());
        cleanOldHistories(note.getId());
        log.info("创建检查点成功: spaceId={}, noteId={}, userId={}",
                spaceId, id, LoginUserUtil.currentUserId());
        return toVO(note, true, isTeamSpace(spaceId));
    }

    /**
     * 删除笔记（软删除）
     *
     * @param spaceId 空间ID
     * @param id      笔记ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long spaceId, Long id) {
        Note note = findNote(spaceId, id);
        permissionValidator.ensureNoteOwnerOrAdmin(note, LoginUserUtil.currentUserId());
        noteMapper.deleteById(id);
        noteTagMapper.physicalDeleteByNoteId(id);
        publishSearchIndexDelete(id);
        publishDocumentRealtimeEvent(spaceId, SpaceRealtimeEventType.DOCUMENT_DELETED, note);
        log.info("删除笔记成功: spaceId={}, noteId={}, userId={}",
                spaceId, id, LoginUserUtil.currentUserId());
    }

    /**
     * 查询笔记的历史版本列表
     *
     * @param spaceId 空间ID
     * @param id      笔记ID
     * @return 历史版本列表
     */
    @Override
    public List<NoteHistoryVO> listHistory(Long spaceId, Long id) {
        Note note = findNote(spaceId, id);
        ensureNoteWritePermission(note, LoginUserUtil.currentUserId());
        return noteHistoryMapper.selectList(Wrappers.<NoteHistory>lambdaQuery()
                        .eq(NoteHistory::getNoteId, id)
                        .orderByDesc(NoteHistory::getVersion))
                .stream()
                .map(this::toHistoryVO)
                .toList();
    }

    /**
     * 获取指定版本的历史详情
     *
     * @param spaceId 空间ID
     * @param id      笔记ID
     * @param version 版本号
     * @return 历史版本VO
     */
    @Override
    public NoteHistoryVO getHistory(Long spaceId, Long id, Integer version) {
        Note note = findNote(spaceId, id);
        ensureNoteWritePermission(note, LoginUserUtil.currentUserId());
        return toHistoryVO(findHistory(id, version));
    }

    /**
     * 恢复笔记到指定版本
     *
     * @param spaceId 空间ID
     * @param id      笔记ID
     * @param version 版本号
     * @return 恢复后的笔记VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteVO restore(Long spaceId, Long id, Integer version) {
        Note note = findNoteForUpdate(spaceId, id);
        ensureNoteWritePermission(note, LoginUserUtil.currentUserId());
        NoteHistory history = findHistory(id, version);
        createSnapshot(note, NoteHistorySaveType.MANUAL, history.getTitle(), history.getContent());
        note.setTitle(history.getTitle());
        note.setContent(history.getContent());
        note.setContentHtml(null);
        noteMapper.updateById(note);
        publishSearchIndexUpsert(note.getId());
        publishDocumentRealtimeEvent(spaceId, SpaceRealtimeEventType.DOCUMENT_UPDATED, note);
        cleanOldHistories(note.getId());
        log.info("恢复笔记版本成功: spaceId={}, noteId={}, version={}, userId={}",
                spaceId, id, version, LoginUserUtil.currentUserId());
        return toVO(note, true, isTeamSpace(spaceId));
    }

    /**
     * 生成分享链接
     *
     * @param spaceId 空间ID
     * @param id      笔记ID
     * @param request 分享请求（含过期时间）
     * @return 设置分享链接后的笔记VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteVO share(Long spaceId, Long id, NoteShareRequest request) {
        if (!systemSettingService.isNoteShareEnabled()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "系统暂未开放笔记公开分享");
        }
        Note note = findNote(spaceId, id);
        permissionValidator.ensureNoteOwnerOrAdmin(note, LoginUserUtil.currentUserId());
        LocalDateTime expireAt = resolveShareExpireAt(request);
        for (int retry = 0; retry < MAX_SHARE_CODE_RETRY; retry++) {
            note.setIsPublic(true);
            note.setShareCode(generateUniqueShareCode());
            note.setShareExpire(expireAt);
            try {
                noteMapper.updateById(note);
                log.info("生成分享链接成功: spaceId={}, noteId={}, userId={}",
                        spaceId, id, LoginUserUtil.currentUserId());
                return toVO(note, canCurrentUserEditNote(note), isTeamSpace(spaceId));
            } catch (DuplicateKeyException exception) {
                log.warn("分享码冲突，重试 {}/{}: spaceId={}, noteId={}",
                        retry + 1, MAX_SHARE_CODE_RETRY, spaceId, id, exception);
            }
        }
        throw new BusinessException(ErrorCode.CONFLICT, "分享码生成冲突，请稍后重试");
    }

    /**
     * 通过分享码公开访问笔记
     *
     * @param shareCode 分享码
     * @return 笔记VO（只读，不可编辑）
     */
    @Override
    public NoteVO publicAccess(String shareCode) {
        if (!systemSettingService.isNoteShareEnabled()) {
            throw new ResourceNotFoundException("分享笔记不存在或已过期");
        }
        Note note = noteMapper.selectOne(Wrappers.<Note>lambdaQuery()
                .eq(Note::getShareCode, shareCode)
                .eq(Note::getIsPublic, true)
                .and(condition -> condition.isNull(Note::getShareExpire)
                        .or()
                        .ge(Note::getShareExpire, LocalDateTime.now())));
        if (note == null) {
            throw new ResourceNotFoundException("分享笔记不存在或已过期");
        }
        increaseViewCount(note.getId());
        return toVO(note, false, false);
    }

    /**
     * 全文搜索笔记（支持标题和内容）
     *
     * @param spaceId 空间ID
     * @param keyword 搜索关键词
     * @return 匹配的笔记列表
     */
    @Override
    public List<NoteVO> search(Long spaceId, String keyword) {
        permissionValidator.ensureSpaceMember(spaceId, LoginUserUtil.currentUserId());
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        Optional<List<Long>> searchNoteIds = noteSearchService.searchNoteIds(spaceId, keyword, 50);
        if (searchNoteIds.isPresent()) {
            return findNotesBySearchIds(spaceId, searchNoteIds.get());
        }
        return noteMapper.selectList(Wrappers.<Note>lambdaQuery()
                        .eq(Note::getSpaceId, spaceId)
                        .and(condition -> condition.like(Note::getTitle, keyword)
                                .or()
                                .like(Note::getContent, keyword))
                        .orderByDesc(Note::getGmtModified))
                .stream()
                .map(note -> toVO(note, canCurrentUserEditNote(note), isTeamSpace(note.getSpaceId())))
                .toList();
    }

    private List<NoteVO> findNotesBySearchIds(Long spaceId, List<Long> noteIds) {
        if (noteIds == null || noteIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Note> noteMap = noteMapper.selectList(Wrappers.<Note>lambdaQuery()
                        .eq(Note::getSpaceId, spaceId)
                        .in(Note::getId, noteIds))
                .stream()
                .collect(Collectors.toMap(Note::getId, Function.identity(), (left, right) -> left));
        return noteIds.stream()
                .map(noteMap::get)
                .filter(Objects::nonNull)
                .map(note -> toVO(note, canCurrentUserEditNote(note), isTeamSpace(note.getSpaceId())))
                .toList();
    }

    // ============================ 私有辅助方法 ============================

    /**
     * 根据空间ID和笔记ID查询笔记（不加锁）
     *
     * @param spaceId 空间ID
     * @param id      笔记ID
     * @return 笔记实体
     */
    private Note findNote(Long spaceId, Long id) {
        Note note = noteMapper.selectOne(Wrappers.<Note>lambdaQuery()
                .eq(Note::getSpaceId, spaceId)
                .eq(Note::getId, id));
        if (note == null) {
            throw new ResourceNotFoundException("笔记不存在");
        }
        return note;
    }

    /**
     * 根据空间ID和笔记ID查询笔记（加FOR UPDATE锁，用于更新操作）
     *
     * @param spaceId 空间ID
     * @param id      笔记ID
     * @return 笔记实体
     */
    private Note findNoteForUpdate(Long spaceId, Long id) {
        Note note = noteMapper.selectOne(Wrappers.<Note>lambdaQuery()
                .eq(Note::getSpaceId, spaceId)
                .eq(Note::getId, id)
                .last(LOCK_FOR_UPDATE_SQL));
        if (note == null) {
            throw new ResourceNotFoundException("笔记不存在");
        }
        return note;
    }

    /**
     * 检查当前用户是否有编辑笔记的权限
     *
     * @param note   笔记实体
     * @param userId 用户ID
     */
    private void ensureNoteWritePermission(Note note, Long userId) {
        if (isTeamSpace(note.getSpaceId())) {
            // 团队空间：需要编辑权限（协作者）
            permissionValidator.ensureSpaceCollaborator(note.getSpaceId(), userId);
            return;
        }
        // 个人空间：需要是所有者或管理员
        permissionValidator.ensureNoteOwnerOrAdmin(note, userId);
    }

    /**
     * 判断空间是否为团队空间
     *
     * @param spaceId 空间ID
     * @return true=团队空间，false=个人空间
     */
    private boolean isTeamSpace(Long spaceId) {
        Space space = spaceMapper.selectById(spaceId);
        return space != null && SpaceType.TEAM.equals(space.getType());
    }

    /**
     * 确保当前空间支持实时协作（仅团队空间）
     *
     * @param spaceId 空间ID
     */
    private void ensureTeamCollaborationEnabled(Long spaceId) {
        if (!isTeamSpace(spaceId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅团队空间支持实时协作");
        }
    }

    /**
     * 查询指定笔记的指定版本历史
     *
     * @param noteId  笔记ID
     * @param version 版本号
     * @return 历史版本实体
     */
    private NoteHistory findHistory(Long noteId, Integer version) {
        NoteHistory history = noteHistoryMapper.selectOne(Wrappers.<NoteHistory>lambdaQuery()
                .eq(NoteHistory::getNoteId, noteId)
                .eq(NoteHistory::getVersion, version));
        if (history == null) {
            throw new ResourceNotFoundException("笔记历史版本不存在");
        }
        return history;
    }

    /**
     * 根据标签ID查找相关的笔记ID列表
     *
     * @param tagId 标签ID
     * @return 笔记ID列表
     */
    private List<Long> findNoteIdsByTag(Long tagId) {
        if (tagId == null) {
            return Collections.emptyList();
        }
        return noteTagMapper.selectList(Wrappers.<NoteTag>lambdaQuery().eq(NoteTag::getTagId, tagId))
                .stream()
                .map(NoteTag::getNoteId)
                .toList();
    }

    /**
     * 生成唯一的分享码
     *
     * @return 唯一分享码
     */
    private String generateUniqueShareCode() {
        for (int retry = 0; retry < MAX_SHARE_CODE_RETRY; retry++) {
            String shareCode = StringGenerator.randomAlphanumeric(SHARE_CODE_LENGTH);
            Long duplicateCount = noteMapper.selectCount(Wrappers.<Note>lambdaQuery()
                    .eq(Note::getShareCode, shareCode));
            if (duplicateCount == 0) {
                return shareCode;
            }
            log.warn("分享码冲突，重试 {}/{}", retry + 1, MAX_SHARE_CODE_RETRY);
        }
        throw new BusinessException(ErrorCode.CONFLICT, "分享码生成冲突，请稍后重试");
    }

    /**
     * 发布笔记搜索索引写入事件。
     *
     * @param noteId 笔记标识
     */
    private void publishSearchIndexUpsert(Long noteId) {
        applicationEventPublisher.publishEvent(new SearchIndexRequestedEvent(noteId, SearchIndexOperation.UPSERT));
    }

    /**
     * 发布笔记搜索索引删除事件。
     *
     * @param noteId 笔记标识
     */
    private void publishSearchIndexDelete(Long noteId) {
        applicationEventPublisher.publishEvent(new SearchIndexRequestedEvent(noteId, SearchIndexOperation.DELETE));
    }

    /**
     * 发布文档实时事件。
     *
     * @param spaceId 空间标识
     * @param type 事件类型
     * @param note 笔记实体
     */
    private void publishDocumentRealtimeEvent(Long spaceId, SpaceRealtimeEventType type, Note note) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("noteId", note.getId());
        payload.put("notebookId", note.getNotebookId());
        payload.put("title", note.getTitle());
        payload.put("projectId", note.getProjectId());
        spaceRealtimeEventService.publish(spaceId, type, payload);
    }

    /**
     * 同步笔记的标签关联（先删除所有旧关联，再插入新关联）
     *
     * @param spaceId 空间ID
     * @param noteId  笔记ID
     * @param tagIds  标签ID列表
     */
    private void syncTags(Long spaceId, Long noteId, List<Long> tagIds) {
        noteTagMapper.physicalDeleteByNoteId(noteId);
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<Long> uniqueTagIds = tagIds.stream().filter(Objects::nonNull).distinct().toList();
        if (uniqueTagIds.isEmpty()) {
            return;
        }
        Long count = tagMapper.selectCount(Wrappers.<Tag>lambdaQuery()
                .eq(Tag::getSpaceId, spaceId)
                .in(Tag::getId, uniqueTagIds));
        if (count != uniqueTagIds.size()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "存在不属于当前空间的标签");
        }
        for (Long tagId : uniqueTagIds) {
            NoteTag noteTag = new NoteTag();
            noteTag.setNoteId(noteId);
            noteTag.setTagId(tagId);
            noteTagMapper.insert(noteTag);
        }
    }

    /**
     * 判断是否需要生成历史快照。
     *
     * @param note 当前笔记
     * @param request 保存请求
     * @return true 表示需要生成历史版本
     */
    private boolean shouldCreateSnapshot(Note note, NoteSaveRequest request) {
        return !Objects.equals(note.getNotebookId(), request.getNotebookId())
                || !Objects.equals(note.getProjectId(), request.getProjectId())
                || !Objects.equals(note.getTitle(), request.getTitle())
                || !Objects.equals(note.getContent(), request.getContent())
                || !Objects.equals(note.getContentHtml(), request.getContentHtml())
                || !Objects.equals(Boolean.TRUE.equals(note.getIsPublic()), Boolean.TRUE.equals(request.getIsPublic()));
    }

    /**
     * 解析历史保存类型。
     *
     * @param saveType 请求中的保存类型
     * @return 保存类型
     */
    private NoteHistorySaveType resolveHistorySaveType(NoteHistorySaveType saveType) {
        return saveType == null ? NoteHistorySaveType.AUTO : saveType;
    }

    /**
     * 构建历史版本变更摘要。
     *
     * @param note 当前笔记
     * @param nextTitle 保存后的标题
     * @param nextContent 保存后的正文
     * @return 变更摘要
     */
    private String buildChangeSummary(Note note, String nextTitle, String nextContent) {
        boolean noteWillChange = !Objects.equals(note.getTitle(), nextTitle)
                || !Objects.equals(note.getContent(), nextContent);
        NoteHistory latestHistory = findLatestHistory(note.getId());
        String previousTitle = noteWillChange || latestHistory == null ? note.getTitle() : latestHistory.getTitle();
        String previousContent = noteWillChange || latestHistory == null ? note.getContent() : latestHistory.getContent();
        List<String> summaries = new ArrayList<>();
        if (!Objects.equals(previousTitle, nextTitle)) {
            summaries.add("标题已更新");
        }
        int characterDiff = countContentCharacters(nextContent) - countContentCharacters(previousContent);
        if (characterDiff > 0) {
            summaries.add("字符 +" + characterDiff);
        } else if (characterDiff < 0) {
            summaries.add("字符 " + characterDiff);
        } else {
            summaries.add("字符无变化");
        }
        return String.join("，", summaries);
    }

    /**
     * 统计正文字符数。
     *
     * @param content 正文内容
     * @return 字符数
     */
    private int countContentCharacters(String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }
        String plainContent = content.replaceAll("<[^>]+>", " ").replace("&nbsp;", " ");
        return (int) plainContent.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint))
                .count();
    }

    /**
     * 判断最新历史版本是否已经记录当前内容。
     *
     * @param noteId 笔记标识
     * @param title 标题
     * @param content 正文
     * @return true 表示最新历史版本已是当前内容
     */
    private boolean latestHistoryMatches(Long noteId, String title, String content) {
        NoteHistory latestHistory = findLatestHistory(noteId);
        if (latestHistory == null) {
            return false;
        }
        return Objects.equals(latestHistory.getTitle(), title)
                && Objects.equals(latestHistory.getContent(), content);
    }

    /**
     * 查询最新历史版本。
     *
     * @param noteId 笔记标识
     * @return 最新历史版本，不存在则返回 null
     */
    private NoteHistory findLatestHistory(Long noteId) {
        List<NoteHistory> histories = noteHistoryMapper.selectList(Wrappers.<NoteHistory>lambdaQuery()
                .eq(NoteHistory::getNoteId, noteId)
                .orderByDesc(NoteHistory::getVersion));
        if (histories.isEmpty()) {
            return null;
        }
        return histories.get(0);
    }

    /**
     * 创建笔记快照（保存当前状态到历史表）。
     *
     * @param note 笔记实体
     * @param saveType 保存类型
     * @param nextTitle 保存后的标题
     * @param nextContent 保存后的正文
     */
    private void createSnapshot(Note note, NoteHistorySaveType saveType, String nextTitle, String nextContent) {
        String lockKey = RedisKeyConstants.noteHistoryLock(note.getId());
        String lockOwner = UUID.randomUUID().toString();
        if (!redisUtil.tryLock(lockKey, lockOwner, SNAPSHOT_LOCK_TTL)) {
            throw new BusinessException(ErrorCode.CONFLICT, "笔记历史版本正在保存，请稍后再试");
        }
        try {
            for (int retry = 0; retry < MAX_SNAPSHOT_RETRY; retry++) {
                Integer nextVersion = nextVersion(note.getId());
                NoteHistory history = new NoteHistory();
                history.setNoteId(note.getId());
                history.setTitle(note.getTitle());
                history.setContent(note.getContent());
                history.setVersion(nextVersion);
                history.setSaveType(resolveHistorySaveType(saveType));
                history.setChangeSummary(buildChangeSummary(note, nextTitle, nextContent));
                try {
                    noteHistoryMapper.insert(history);
                    return;
                } catch (DuplicateKeyException exception) {
                    log.warn("笔记历史版本号冲突，重试保存快照: noteId={}, version={}, retry={}",
                            note.getId(), nextVersion, retry + 1, exception);
                }
            }
        } finally {
            redisUtil.unlock(lockKey, lockOwner);
        }
        throw new BusinessException(ErrorCode.CONFLICT, "笔记历史版本保存冲突，请稍后重试");
    }

    private Integer nextVersion(Long noteId) {
        return noteHistoryMapper.selectList(Wrappers.<NoteHistory>lambdaQuery()
                        .eq(NoteHistory::getNoteId, noteId)
                        .last(LOCK_FOR_UPDATE_SQL))
                .stream()
                .map(NoteHistory::getVersion)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    /**
     * 清理超出最大保留数量的历史版本
     *
     * @param noteId 笔记ID
     */
    private void cleanOldHistories(Long noteId) {
        int maxHistoryCount = systemSettingService.getNoteHistoryMaxVersions();
        List<NoteHistory> histories = noteHistoryMapper.selectList(Wrappers.<NoteHistory>lambdaQuery()
                        .eq(NoteHistory::getNoteId, noteId))
                .stream()
                .sorted(Comparator.comparing(NoteHistory::getVersion).reversed())
                .toList();
        if (histories.size() <= maxHistoryCount) {
            return;
        }
        histories.stream().skip(maxHistoryCount).forEach(history -> noteHistoryMapper.deleteById(history.getId()));
    }

    /**
     * 解析分享链接过期时间。
     *
     * @param request 分享请求
     * @return 分享链接过期时间
     */
    private LocalDateTime resolveShareExpireAt(NoteShareRequest request) {
        if (request != null && request.getExpireAt() != null) {
            return request.getExpireAt();
        }
        int defaultExpireMinutes = systemSettingService.getNoteShareDefaultExpireMinutes();
        if (defaultExpireMinutes <= 0) {
            return null;
        }
        return LocalDateTime.now().plusMinutes(defaultExpireMinutes);
    }

    /**
     * 增加笔记的访问计数（使用Redis）
     *
     * @param noteId 笔记ID
     */
    private void increaseViewCount(Long noteId) {
        try {
            redisUtil.increment(RedisKeyConstants.noteViewCount(noteId));
        } catch (RuntimeException exception) {
            log.warn("增加笔记访问计数失败, noteId={}", noteId, exception);
        }
    }

    /**
     * 查询笔记关联的标签列表
     *
     * @param noteId 笔记ID
     * @return 标签VO列表
     */
    private List<TagVO> findTags(Long noteId) {
        List<Long> tagIds = noteTagMapper.selectList(Wrappers.<NoteTag>lambdaQuery().eq(NoteTag::getNoteId, noteId))
                .stream()
                .map(NoteTag::getTagId)
                .toList();
        if (tagIds.isEmpty()) {
            return Collections.emptyList();
        }
        return tagMapper.selectBatchIds(tagIds).stream().map(this::toTagVO).toList();
    }

    /**
     * 将Note实体转换为NoteVO
     *
     * @param note          笔记实体
     * @param canEdit       当前用户是否有编辑权限
     * @param collabEnabled 是否启用协作编辑
     * @return 笔记VO
     */
    private NoteVO toVO(Note note, Boolean canEdit, Boolean collabEnabled) {
        return new NoteVO(note.getId(), note.getSpaceId(), note.getNotebookId(), note.getProjectId(),
                projectNameOf(note.getProjectId()), note.getUserId(), note.getTitle(), note.getContent(),
                sanitizeContentHtml(note.getContentHtml()), canEdit, collabEnabled, note.getIsPublic(), note.getShareCode(),
                note.getShareExpire(), note.getViewCount(), note.getGmtCreate(), note.getGmtModified(),
                findTags(note.getId()));
    }

    /**
     * 净化笔记HTML内容。
     *
     * @param contentHtml 原始HTML内容
     * @return 安全HTML内容
     */
    private String sanitizeContentHtml(String contentHtml) {
        return htmlSanitizerUtil.sanitize(contentHtml);
    }

    /**
     * 判断当前用户是否有编辑笔记的权限（不抛异常，返回布尔值）
     *
     * @param note 笔记实体
     * @return true=可编辑，false=不可编辑
     */
    private boolean canCurrentUserEditNote(Note note) {
        try {
            ensureNoteWritePermission(note, LoginUserUtil.currentUserId());
            return true;
        } catch (AccessDeniedException exception) {
            return false;
        }
    }

    /**
     * 验证项目是否属于当前空间且未归档
     *
     * @param spaceId   空间ID
     * @param projectId 项目ID
     * @return 项目ID（若为null则返回null）
     */
    private Long validateProjectInSpace(Long spaceId, Long projectId) {
        if (projectId == null) {
            return null;
        }
        Project project = projectMapper.selectOne(Wrappers.<Project>lambdaQuery()
                .eq(Project::getSpaceId, spaceId)
                .eq(Project::getId, projectId)
                .eq(Project::getArchived, false));
        if (project == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "项目不存在或已归档");
        }
        return projectId;
    }

    /**
     * 获取项目名称
     *
     * @param projectId 项目ID
     * @return 项目名称（若不存在或为null则返回null）
     */
    private String projectNameOf(Long projectId) {
        if (projectId == null) {
            return null;
        }
        Project project = projectMapper.selectById(projectId);
        return project == null ? null : project.getName();
    }

    /**
     * 生成协作Ticket的Redis键
     *
     * @param ticket Ticket字符串
     * @return Redis键
     */
    private String collabTicketKey(String ticket) {
        return RedisKeyConstants.collabTicket(ticket);
    }

    /**
     * 构造协作Ticket的Value字符串
     *
     * @param spaceId 空间ID
     * @param noteId  笔记ID
     * @param userId  用户ID
     * @return 拼接后的字符串
     */
    private String buildCollabTicketValue(Long spaceId, Long noteId, Long userId) {
        return spaceId + COLLAB_TICKET_VALUE_DELIMITER + noteId + COLLAB_TICKET_VALUE_DELIMITER + userId;
    }

    /**
     * 将NoteHistory实体转换为NoteHistoryVO
     *
     * @param history 历史实体
     * @return 历史VO
     */
    private NoteHistoryVO toHistoryVO(NoteHistory history) {
        return new NoteHistoryVO(history.getId(), history.getNoteId(), history.getTitle(), history.getContent(),
                history.getVersion(), history.getChangeSummary(), resolveHistorySaveType(history.getSaveType()),
                history.getGmtCreate());
    }

    /**
     * 将Tag实体转换为TagVO
     *
     * @param tag 标签实体
     * @return 标签VO
     */
    private TagVO toTagVO(Tag tag) {
        return new TagVO(tag.getId(), tag.getName(), tag.getSpaceId(), tag.getGmtCreate());
    }
}
