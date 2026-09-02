package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.notaskflow.common.enums.SpaceRealtimeEventType;
import com.notaskflow.domain.dto.request.NoteTagBindRequest;
import com.notaskflow.domain.dto.request.TagSaveRequest;
import com.notaskflow.domain.entity.Note;
import com.notaskflow.domain.entity.NoteTag;
import com.notaskflow.domain.entity.Tag;
import com.notaskflow.domain.vo.TagVO;
import com.notaskflow.exception.BusinessException;
import com.notaskflow.exception.ErrorCode;
import com.notaskflow.exception.ResourceNotFoundException;
import com.notaskflow.mapper.NoteMapper;
import com.notaskflow.mapper.NoteTagMapper;
import com.notaskflow.mapper.TagMapper;
import com.notaskflow.security.PermissionValidator;
import com.notaskflow.service.SpaceRealtimeEventService;
import com.notaskflow.service.TagService;
import com.notaskflow.utils.LoginUserUtil;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 标签服务实现，处理空间标签和笔记标签绑定。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    private final NoteMapper noteMapper;

    private final NoteTagMapper noteTagMapper;

    private final PermissionValidator permissionValidator;

    private final SpaceRealtimeEventService spaceRealtimeEventService;

    /**
     * 查询空间内标签列表。
     *
     * @param spaceId 空间标识
     * @return 标签列表
     */
    @Override
    public List<TagVO> list(Long spaceId) {
        permissionValidator.ensureSpaceMember(spaceId, LoginUserUtil.currentUserId());
        return tagMapper.selectList(Wrappers.<Tag>lambdaQuery()
                        .eq(Tag::getSpaceId, spaceId)
                        .orderByAsc(Tag::getName))
                .stream()
                .map(this::toVO)
                .toList();
    }

    /**
     * 创建空间内标签。
     *
     * @param spaceId 空间标识
     * @param request 标签保存请求
     * @return 标签信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagVO create(Long spaceId, TagSaveRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
        validateUniqueName(spaceId, request.getName(), null);
        Tag tag = new Tag();
        tag.setSpaceId(spaceId);
        tag.setName(request.getName());
        tagMapper.insert(tag);
        spaceRealtimeEventService.publish(spaceId, SpaceRealtimeEventType.TAG_CREATED,
                Map.of("tagId", tag.getId(), "name", tag.getName()));
        log.info("标签创建完成，spaceId={}，tagId={}，operatorId={}", spaceId, tag.getId(), currentUserId);
        return toVO(tag);
    }

    /**
     * 更新空间内标签。
     *
     * @param spaceId 空间标识
     * @param id 标签标识
     * @param request 标签保存请求
     * @return 标签信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagVO update(Long spaceId, Long id, TagSaveRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
        Tag tag = findTag(spaceId, id);
        validateUniqueName(spaceId, request.getName(), id);
        tag.setName(request.getName());
        tagMapper.updateById(tag);
        spaceRealtimeEventService.publish(spaceId, SpaceRealtimeEventType.TAG_UPDATED,
                Map.of("tagId", tag.getId(), "name", tag.getName()));
        log.info("标签更新完成，spaceId={}，tagId={}，operatorId={}", spaceId, id, currentUserId);
        return toVO(tag);
    }

    /**
     * 删除空间内标签及笔记绑定。
     *
     * @param spaceId 空间标识
     * @param id 标签标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long spaceId, Long id) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
        findTag(spaceId, id);
        noteTagMapper.physicalDeleteByTagId(id);
        tagMapper.deleteById(id);
        spaceRealtimeEventService.publish(spaceId, SpaceRealtimeEventType.TAG_DELETED,
                Map.of("tagId", id));
        log.info("标签删除完成，spaceId={}，tagId={}，operatorId={}", spaceId, id, currentUserId);
    }

    /**
     * 为笔记批量绑定标签。
     *
     * @param spaceId 空间标识
     * @param noteId 笔记标识
     * @param request 标签绑定请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindTags(Long spaceId, Long noteId, NoteTagBindRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
        Note note = findNote(spaceId, noteId);
        permissionValidator.ensureNoteOwnerOrAdmin(note, currentUserId);
        List<Long> uniqueTagIds = request.getTagIds().stream().filter(Objects::nonNull).distinct().toList();
        if (uniqueTagIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "标签ID列表不能为空");
        }
        Long count = tagMapper.selectCount(Wrappers.<Tag>lambdaQuery()
                .eq(Tag::getSpaceId, spaceId)
                .in(Tag::getId, uniqueTagIds));
        if (count != uniqueTagIds.size()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "存在不属于当前空间的标签");
        }
        for (Long tagId : uniqueTagIds) {
            Long exists = noteTagMapper.selectCount(Wrappers.<NoteTag>lambdaQuery()
                    .eq(NoteTag::getNoteId, noteId)
                    .eq(NoteTag::getTagId, tagId));
            if (exists == 0) {
                NoteTag noteTag = new NoteTag();
                noteTag.setNoteId(noteId);
                noteTag.setTagId(tagId);
                noteTagMapper.insert(noteTag);
            }
        }
        spaceRealtimeEventService.publish(spaceId, SpaceRealtimeEventType.DOCUMENT_UPDATED,
                Map.of("noteId", noteId, "changedField", "tags"));
        log.info("笔记标签绑定完成，spaceId={}，noteId={}，tagCount={}，operatorId={}",
                spaceId, noteId, uniqueTagIds.size(), currentUserId);
    }

    /**
     * 移除笔记的单个标签。
     *
     * @param spaceId 空间标识
     * @param noteId 笔记标识
     * @param tagId 标签标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTag(Long spaceId, Long noteId, Long tagId) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
        Note note = findNote(spaceId, noteId);
        permissionValidator.ensureNoteOwnerOrAdmin(note, currentUserId);
        findTag(spaceId, tagId);
        noteTagMapper.physicalDeleteByNoteIdAndTagId(noteId, tagId);
        spaceRealtimeEventService.publish(spaceId, SpaceRealtimeEventType.DOCUMENT_UPDATED,
                Map.of("noteId", noteId, "changedField", "tags"));
        log.info("笔记标签移除完成，spaceId={}，noteId={}，tagId={}，operatorId={}",
                spaceId, noteId, tagId, currentUserId);
    }

    /**
     * 查询标签实体。
     *
     * @param spaceId 空间标识
     * @param id 标签标识
     * @return 标签实体
     */
    private Tag findTag(Long spaceId, Long id) {
        Tag tag = tagMapper.selectOne(Wrappers.<Tag>lambdaQuery()
                .eq(Tag::getSpaceId, spaceId)
                .eq(Tag::getId, id));
        if (tag == null) {
            throw new ResourceNotFoundException("标签不存在");
        }
        return tag;
    }

    /**
     * 查询笔记实体。
     *
     * @param spaceId 空间标识
     * @param noteId 笔记标识
     * @return 笔记实体
     */
    private Note findNote(Long spaceId, Long noteId) {
        Note note = noteMapper.selectOne(Wrappers.<Note>lambdaQuery()
                .eq(Note::getSpaceId, spaceId)
                .eq(Note::getId, noteId));
        if (note == null) {
            throw new ResourceNotFoundException("笔记不存在");
        }
        return note;
    }

    /**
     * 校验标签名称在空间内唯一。
     *
     * @param spaceId 空间标识
     * @param name 标签名称
     * @param excludeId 排除标识
     */
    private void validateUniqueName(Long spaceId, String name, Long excludeId) {
        Long count = tagMapper.selectCount(Wrappers.<Tag>lambdaQuery()
                .eq(Tag::getSpaceId, spaceId)
                .eq(Tag::getName, name)
                .ne(excludeId != null, Tag::getId, excludeId));
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "标签名称已存在");
        }
    }

    /**
     * 转换标签视图对象。
     *
     * @param tag 标签实体
     * @return 标签视图对象
     */
    private TagVO toVO(Tag tag) {
        return new TagVO(tag.getId(), tag.getName(), tag.getSpaceId(), tag.getGmtCreate());
    }
}
