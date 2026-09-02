package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.notaskflow.common.enums.SpaceRealtimeEventType;
import com.notaskflow.domain.dto.request.NotebookSaveRequest;
import com.notaskflow.domain.entity.Note;
import com.notaskflow.domain.entity.NoteHistory;
import com.notaskflow.domain.entity.Notebook;
import com.notaskflow.domain.vo.NotebookVO;
import com.notaskflow.exception.BusinessException;
import com.notaskflow.exception.ErrorCode;
import com.notaskflow.exception.ResourceNotFoundException;
import com.notaskflow.mapper.NoteHistoryMapper;
import com.notaskflow.mapper.NoteMapper;
import com.notaskflow.mapper.NoteTagMapper;
import com.notaskflow.mapper.NotebookMapper;
import com.notaskflow.security.PermissionValidator;
import com.notaskflow.service.NotebookService;
import com.notaskflow.service.SpaceRealtimeEventService;
import com.notaskflow.utils.LoginUserUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 笔记本服务实现，处理空间内笔记本树和层级路径。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotebookServiceImpl implements NotebookService {

    private final NotebookMapper notebookMapper;

    private final NoteMapper noteMapper;

    private final NoteHistoryMapper noteHistoryMapper;

    private final NoteTagMapper noteTagMapper;

    private final PermissionValidator permissionValidator;

    private final SpaceRealtimeEventService spaceRealtimeEventService;

    /**
     * 查询空间内的笔记本树。
     *
     * @param spaceId 空间标识
     * @return 笔记本树
     */
    @Override
    public List<NotebookVO> listTree(Long spaceId) {
        permissionValidator.ensureSpaceMember(spaceId, LoginUserUtil.currentUserId());
        List<Notebook> notebooks = notebookMapper.selectList(Wrappers.<Notebook>lambdaQuery()
                .eq(Notebook::getSpaceId, spaceId)
                .orderByAsc(Notebook::getSortOrder)
                .orderByAsc(Notebook::getId));
        Map<Long, NotebookVO> nodeMap = notebooks.stream()
                .map(this::toVO)
                .collect(Collectors.toMap(NotebookVO::getId, Function.identity(), (first, second) -> first,
                        LinkedHashMap::new));
        List<NotebookVO> roots = new ArrayList<>();
        for (NotebookVO node : nodeMap.values()) {
            Long parentId = node.getParentId();
            if (parentId == null || parentId == 0 || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                nodeMap.get(parentId).getChildren().add(node);
            }
        }
        return roots;
    }

    /**
     * 查询空间内的单个笔记本。
     *
     * @param spaceId 空间标识
     * @param id 笔记本标识
     * @return 笔记本信息
     */
    @Override
    public NotebookVO get(Long spaceId, Long id) {
        permissionValidator.ensureSpaceMember(spaceId, LoginUserUtil.currentUserId());
        return toVO(findNotebook(spaceId, id));
    }

    /**
     * 创建空间内的笔记本。
     *
     * @param spaceId 空间标识
     * @param request 笔记本保存请求
     * @return 笔记本信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotebookVO create(Long spaceId, NotebookSaveRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
        Long parentId = normalizeParentId(request.getParentId());
        validateUniqueName(spaceId, request.getName(), null);
        Notebook notebook = new Notebook();
        notebook.setSpaceId(spaceId);
        notebook.setParentId(parentId);
        notebook.setName(request.getName());
        notebook.setSortOrder(request.getSortOrder());
        notebook.setPath(buildPath(spaceId, parentId));
        notebookMapper.insert(notebook);
        spaceRealtimeEventService.publish(spaceId, SpaceRealtimeEventType.DOCUMENT_TREE_CHANGED,
                Map.of("notebookId", notebook.getId(), "action", "created", "parentId", parentId));
        log.info("笔记本创建完成，spaceId={}，notebookId={}，operatorId={}",
                spaceId, notebook.getId(), currentUserId);
        return toVO(notebook);
    }

    /**
     * 更新空间内的笔记本。
     *
     * @param spaceId 空间标识
     * @param id 笔记本标识
     * @param request 笔记本保存请求
     * @return 笔记本信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotebookVO update(Long spaceId, Long id, NotebookSaveRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
        Notebook notebook = findNotebook(spaceId, id);
        Long parentId = normalizeParentId(request.getParentId());
        validateMove(spaceId, notebook, parentId);
        validateUniqueName(spaceId, request.getName(), id);
        notebook.setName(request.getName());
        notebook.setParentId(parentId);
        notebook.setSortOrder(request.getSortOrder());
        notebook.setPath(buildPath(spaceId, parentId));
        notebookMapper.updateById(notebook);
        refreshChildPaths(spaceId, notebook.getId());
        spaceRealtimeEventService.publish(spaceId, SpaceRealtimeEventType.DOCUMENT_TREE_CHANGED,
                Map.of("notebookId", notebook.getId(), "action", "updated", "parentId", parentId));
        log.info("笔记本更新完成，spaceId={}，notebookId={}，operatorId={}", spaceId, id, currentUserId);
        return toVO(notebook);
    }

    /**
     * 删除空间内的笔记本及其下级内容。
     *
     * @param spaceId 空间标识
     * @param id 笔记本标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long spaceId, Long id) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
        Notebook notebook = findNotebook(spaceId, id);
        List<Long> notebookIds = collectNotebookIds(spaceId, notebook);
        List<Long> noteIds = noteMapper.selectList(Wrappers.<Note>lambdaQuery()
                        .eq(Note::getSpaceId, spaceId)
                        .in(Note::getNotebookId, notebookIds))
                .stream()
                .map(Note::getId)
                .toList();
        if (!noteIds.isEmpty()) {
            noteHistoryMapper.delete(Wrappers.<NoteHistory>lambdaQuery().in(NoteHistory::getNoteId, noteIds));
            noteTagMapper.physicalDeleteByNoteIds(noteIds);
            noteMapper.deleteBatchIds(noteIds);
        }
        notebookMapper.deleteBatchIds(notebookIds);
        spaceRealtimeEventService.publish(spaceId, SpaceRealtimeEventType.DOCUMENT_TREE_CHANGED,
                Map.of("notebookId", id, "action", "deleted", "deletedNoteIds", noteIds));
        log.info("笔记本递归删除完成，spaceId={}，rootNotebookId={}，deletedNotebookCount={}，deletedNoteCount={}，operatorId={}",
                spaceId, id, notebookIds.size(), noteIds.size(), currentUserId);
    }

    /**
     * 查询笔记本实体。
     *
     * @param spaceId 空间标识
     * @param id 笔记本标识
     * @return 笔记本实体
     */
    private Notebook findNotebook(Long spaceId, Long id) {
        Notebook notebook = notebookMapper.selectOne(Wrappers.<Notebook>lambdaQuery()
                .eq(Notebook::getSpaceId, spaceId)
                .eq(Notebook::getId, id));
        if (notebook == null) {
            throw new ResourceNotFoundException("笔记本不存在");
        }
        return notebook;
    }

    /**
     * 标准化父级标识。
     *
     * @param parentId 父级标识
     * @return 父级标识
     */
    private Long normalizeParentId(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    /**
     * 构造路径字段。
     *
     * @param spaceId 空间标识
     * @param parentId 父级标识
     * @return 路径
     */
    private String buildPath(Long spaceId, Long parentId) {
        if (parentId == null || parentId == 0) {
            return "/";
        }
        Notebook parent = findNotebook(spaceId, parentId);
        return parent.getPath() + parent.getId() + "/";
    }

    /**
     * 校验移动操作不会形成循环。
     *
     * @param spaceId 空间标识
     * @param notebook 当前笔记本
     * @param parentId 新父级标识
     */
    private void validateMove(Long spaceId, Notebook notebook, Long parentId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        if (notebook.getId().equals(parentId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "笔记本不能移动到自身下级");
        }
        Notebook parent = findNotebook(spaceId, parentId);
        String currentPathSegment = "/" + notebook.getId() + "/";
        if (parent.getPath().contains(currentPathSegment)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "笔记本不能移动到自身子级");
        }
    }

    /**
     * 校验笔记本名称在空间内唯一。
     *
     * @param spaceId 空间标识
     * @param name 笔记本名称
     * @param excludeId 排除的笔记本标识
     */
    private void validateUniqueName(Long spaceId, String name, Long excludeId) {
        Long count = notebookMapper.selectCount(Wrappers.<Notebook>lambdaQuery()
                .eq(Notebook::getSpaceId, spaceId)
                .eq(Notebook::getName, name)
                .ne(excludeId != null, Notebook::getId, excludeId));
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "笔记本名称已存在");
        }
    }

    /**
     * 刷新子级路径。
     *
     * @param spaceId 空间标识
     * @param parentId 父级标识
     */
    private void refreshChildPaths(Long spaceId, Long parentId) {
        List<Notebook> children = notebookMapper.selectList(Wrappers.<Notebook>lambdaQuery()
                .eq(Notebook::getSpaceId, spaceId)
                .eq(Notebook::getParentId, parentId));
        children.sort(Comparator.comparing(Notebook::getId));
        for (Notebook child : children) {
            child.setPath(buildPath(spaceId, child.getParentId()));
            notebookMapper.updateById(child);
            refreshChildPaths(spaceId, child.getId());
        }
    }

    /**
     * 收集当前笔记本及其全部下级笔记本标识。
     *
     * @param spaceId 空间标识
     * @param root 根笔记本
     * @return 笔记本标识列表
     */
    private List<Long> collectNotebookIds(Long spaceId, Notebook root) {
        List<Long> notebookIds = new ArrayList<>();
        notebookIds.add(root.getId());
        String descendantPathPrefix = root.getPath() + root.getId() + "/";
        List<Long> descendantIds = notebookMapper.selectList(Wrappers.<Notebook>lambdaQuery()
                        .eq(Notebook::getSpaceId, spaceId)
                        .likeRight(Notebook::getPath, descendantPathPrefix))
                .stream()
                .map(Notebook::getId)
                .toList();
        notebookIds.addAll(descendantIds);
        return notebookIds;
    }

    /**
     * 转换笔记本视图对象。
     *
     * @param notebook 笔记本实体
     * @return 笔记本视图对象
     */
    private NotebookVO toVO(Notebook notebook) {
        return new NotebookVO(notebook.getId(), notebook.getSpaceId(), notebook.getParentId(), notebook.getPath(),
                notebook.getName(), notebook.getSortOrder(), notebook.getGmtCreate(), new ArrayList<>());
    }
}
