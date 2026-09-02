package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.notaskflow.common.enums.BusinessType;
import com.notaskflow.config.MinioProperties;
import com.notaskflow.domain.dto.request.AttachmentBindRequest;
import com.notaskflow.domain.dto.request.AttachmentUnbindRequest;
import com.notaskflow.domain.entity.Attachment;
import com.notaskflow.domain.entity.BusinessAttachment;
import com.notaskflow.domain.entity.Note;
import com.notaskflow.domain.entity.Task;
import com.notaskflow.domain.entity.Todo;
import com.notaskflow.domain.vo.AttachmentVO;
import com.notaskflow.exception.BusinessException;
import com.notaskflow.exception.ErrorCode;
import com.notaskflow.exception.ResourceNotFoundException;
import com.notaskflow.mapper.AttachmentMapper;
import com.notaskflow.mapper.BusinessAttachmentMapper;
import com.notaskflow.mapper.NoteMapper;
import com.notaskflow.mapper.TaskMapper;
import com.notaskflow.mapper.TodoMapper;
import com.notaskflow.security.PermissionValidator;
import com.notaskflow.service.AttachmentService;
import com.notaskflow.storage.MinioStorageService;
import com.notaskflow.utils.LoginUserUtil;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 附件服务实现，处理空间内附件元数据、对象存储和业务绑定。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentMapper attachmentMapper;

    private final BusinessAttachmentMapper businessAttachmentMapper;

    private final NoteMapper noteMapper;

    private final TaskMapper taskMapper;

    private final TodoMapper todoMapper;

    private final PermissionValidator permissionValidator;

    private final MinioStorageService minioStorageService;

    private final MinioProperties minioProperties;

    /**
     * 上传附件并记录空间内的附件元数据。
     *
     * @param spaceId 空间标识
     * @param file 文件对象
     * @return 附件信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttachmentVO upload(Long spaceId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "附件文件不能为空");
        }
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
        String storagePath = minioStorageService.uploadAttachment(spaceId, file);
        Attachment attachment = new Attachment();
        attachment.setFileName(safeFileName(file.getOriginalFilename()));
        attachment.setStoragePath(storagePath);
        attachment.setBucketName(minioProperties.getBucketName());
        attachment.setFileSize(file.getSize());
        attachment.setMimeType(file.getContentType());
        attachment.setUploaderId(currentUserId);
        attachment.setSpaceId(spaceId);
        attachmentMapper.insert(attachment);
        log.info("附件上传完成，spaceId={}，attachmentId={}，uploaderId={}",
                spaceId, attachment.getId(), currentUserId);
        return toVO(attachment, true);
    }

    /**
     * 查询指定空间内的附件元信息。
     *
     * @param spaceId 空间标识
     * @param id 附件标识
     * @return 附件信息
     */
    @Override
    public AttachmentVO get(Long spaceId, Long id) {
        Attachment attachment = findAttachment(id);
        ensureAttachmentInSpace(attachment, spaceId);
        permissionValidator.ensureSpaceMember(spaceId, LoginUserUtil.currentUserId());
        log.debug("查询附件元信息，spaceId={}，attachmentId={}", spaceId, id);
        return toVO(attachment, false);
    }

    /**
     * 查询指定空间内的附件并生成临时下载地址。
     *
     * @param spaceId 空间标识
     * @param id 附件标识
     * @return 附件信息
     */
    @Override
    public AttachmentVO download(Long spaceId, Long id) {
        Attachment attachment = findAttachment(id);
        ensureAttachmentInSpace(attachment, spaceId);
        permissionValidator.ensureSpaceMember(spaceId, LoginUserUtil.currentUserId());
        log.debug("生成附件下载地址，spaceId={}，attachmentId={}", spaceId, id);
        return toVO(attachment, true);
    }

    /**
     * 删除指定空间内的附件及其业务绑定。
     *
     * @param spaceId 空间标识
     * @param id 附件标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long spaceId, Long id) {
        Attachment attachment = findAttachment(id);
        ensureAttachmentInSpace(attachment, spaceId);
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
        businessAttachmentMapper.physicalDeleteByAttachmentId(id);
        attachmentMapper.deleteById(id);
        minioStorageService.delete(attachment.getStoragePath());
        log.info("附件删除完成，spaceId={}，attachmentId={}，operatorId={}",
                spaceId, id, currentUserId);
    }

    /**
     * 绑定指定空间内的附件到业务对象。
     *
     * @param spaceId 空间标识
     * @param request 绑定请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bind(Long spaceId, AttachmentBindRequest request) {
        Attachment attachment = findAttachment(request.getAttachmentId());
        ensureAttachmentInSpace(attachment, spaceId);
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
        ensureBusinessBelongsToSpace(spaceId, request.getBusinessType(), request.getBusinessId());
        String referenceKey = normalizeReferenceKey(request.getReferenceKey());
        Long duplicateCount = businessAttachmentMapper.selectCount(Wrappers.<BusinessAttachment>lambdaQuery()
                .eq(BusinessAttachment::getAttachmentId, request.getAttachmentId())
                .eq(BusinessAttachment::getBusinessType, request.getBusinessType())
                .eq(BusinessAttachment::getBusinessId, request.getBusinessId())
                .eq(BusinessAttachment::getReferenceKey, referenceKey));
        if (duplicateCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "附件引用已存在");
        }
        BusinessAttachment businessAttachment = new BusinessAttachment();
        businessAttachment.setAttachmentId(request.getAttachmentId());
        businessAttachment.setBusinessType(request.getBusinessType());
        businessAttachment.setBusinessId(request.getBusinessId());
        businessAttachment.setReferenceKey(referenceKey);
        businessAttachmentMapper.insert(businessAttachment);
        log.info("附件绑定完成，spaceId={}，attachmentId={}，businessType={}，businessId={}，operatorId={}",
                spaceId, request.getAttachmentId(), request.getBusinessType(), request.getBusinessId(), currentUserId);
    }

    /**
     * 解除指定空间内的附件业务绑定。
     *
     * @param spaceId 空间标识
     * @param id 附件标识
     * @param request 解绑请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbind(Long spaceId, Long id, AttachmentUnbindRequest request) {
        Attachment attachment = findAttachment(id);
        ensureAttachmentInSpace(attachment, spaceId);
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
        ensureBusinessBelongsToSpace(spaceId, request.getBusinessType(), request.getBusinessId());
        int deleted = businessAttachmentMapper.physicalDeleteReference(
                id,
                request.getBusinessType(),
                request.getBusinessId(),
                normalizeReferenceKey(request.getReferenceKey()));
        log.info("附件解绑完成，spaceId={}，attachmentId={}，businessType={}，businessId={}，deleted={}，operatorId={}",
                spaceId, id, request.getBusinessType(), request.getBusinessId(), deleted, currentUserId);
    }

    /**
     * 查询笔记关联的附件列表。
     *
     * @param spaceId 空间标识
     * @param noteId 笔记标识
     * @return 附件列表
     */
    @Override
    public List<AttachmentVO> listNoteAttachments(Long spaceId, Long noteId) {
        permissionValidator.ensureSpaceMember(spaceId, LoginUserUtil.currentUserId());
        ensureBusinessBelongsToSpace(spaceId, BusinessType.NOTE, noteId);
        return listByBusiness(BusinessType.NOTE, noteId);
    }

    /**
     * 查询任务关联的附件列表。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     * @return 附件列表
     */
    @Override
    public List<AttachmentVO> listTaskAttachments(Long spaceId, Long taskId) {
        permissionValidator.ensureSpaceMember(spaceId, LoginUserUtil.currentUserId());
        ensureBusinessBelongsToSpace(spaceId, BusinessType.TASK, taskId);
        return listByBusiness(BusinessType.TASK, taskId);
    }

    /**
     * 查询附件实体。
     *
     * @param id 附件标识
     * @return 附件实体
     */
    private Attachment findAttachment(Long id) {
        Attachment attachment = attachmentMapper.selectById(id);
        if (attachment == null) {
            throw new ResourceNotFoundException("附件不存在");
        }
        return attachment;
    }

    /**
     * 校验附件是否归属于指定空间。
     *
     * @param attachment 附件实体
     * @param spaceId 空间标识
     */
    private void ensureAttachmentInSpace(Attachment attachment, Long spaceId) {
        if (!spaceId.equals(attachment.getSpaceId())) {
            throw new ResourceNotFoundException("附件不存在");
        }
    }

    /**
     * 校验业务对象是否属于指定空间。
     *
     * @param spaceId 空间标识
     * @param businessType 业务类型
     * @param businessId 业务标识
     */
    private void ensureBusinessBelongsToSpace(Long spaceId, BusinessType businessType, Long businessId) {
        switch (businessType) {
            case NOTE:
                ensureNoteBelongsToSpace(spaceId, businessId);
                return;
            case TASK:
                ensureTaskBelongsToSpace(spaceId, businessId);
                return;
            case TODO:
                ensureTodoBelongsToSpace(spaceId, businessId);
                return;
            default:
                throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的业务类型");
        }
    }

    /**
     * 校验笔记归属空间。
     *
     * @param spaceId 空间标识
     * @param noteId 笔记标识
     */
    private void ensureNoteBelongsToSpace(Long spaceId, Long noteId) {
        Note note = noteMapper.selectOne(Wrappers.<Note>lambdaQuery()
                .eq(Note::getId, noteId)
                .eq(Note::getSpaceId, spaceId));
        if (note == null) {
            throw new ResourceNotFoundException("笔记不存在");
        }
    }

    /**
     * 校验任务归属空间。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     */
    private void ensureTaskBelongsToSpace(Long spaceId, Long taskId) {
        Task task = taskMapper.selectOne(Wrappers.<Task>lambdaQuery()
                .eq(Task::getId, taskId)
                .eq(Task::getSpaceId, spaceId));
        if (task == null) {
            throw new ResourceNotFoundException("任务不存在");
        }
    }

    /**
     * 校验待办归属空间。
     *
     * @param spaceId 空间标识
     * @param todoId 待办标识
     */
    private void ensureTodoBelongsToSpace(Long spaceId, Long todoId) {
        Todo todo = todoMapper.selectOne(Wrappers.<Todo>lambdaQuery()
                .eq(Todo::getId, todoId)
                .eq(Todo::getSpaceId, spaceId));
        if (todo == null) {
            throw new ResourceNotFoundException("待办不存在");
        }
    }

    /**
     * 查询指定业务对象关联的附件。
     *
     * @param businessType 业务类型
     * @param businessId 业务标识
     * @return 附件列表
     */
    private List<AttachmentVO> listByBusiness(BusinessType businessType, Long businessId) {
        List<Long> attachmentIds = businessAttachmentMapper.selectList(Wrappers.<BusinessAttachment>lambdaQuery()
                        .eq(BusinessAttachment::getBusinessType, businessType)
                        .eq(BusinessAttachment::getBusinessId, businessId))
                .stream()
                .map(BusinessAttachment::getAttachmentId)
                .distinct()
                .toList();
        if (attachmentIds.isEmpty()) {
            return Collections.emptyList();
        }
        return attachmentMapper.selectBatchIds(attachmentIds).stream()
                .map(attachment -> toVO(attachment, true))
                .toList();
    }

    /**
     * 标准化引用标识。
     *
     * @param referenceKey 引用标识
     * @return 标准化后的引用标识
     */
    private String normalizeReferenceKey(String referenceKey) {
        return StringUtils.hasText(referenceKey) ? referenceKey : "";
    }

    /**
     * 生成安全的文件名。
     *
     * @param originalFilename 原始文件名
     * @return 清理后的文件名
     */
    private String safeFileName(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "unnamed";
        }
        return StringUtils.cleanPath(originalFilename);
    }

    /**
     * 转换附件视图对象。
     *
     * @param attachment 附件实体
     * @param includeUrl 是否包含下载地址
     * @return 附件视图对象
     */
    private AttachmentVO toVO(Attachment attachment, boolean includeUrl) {
        String downloadUrl = includeUrl ? minioStorageService.presignedDownloadUrl(attachment.getStoragePath()) : null;
        return new AttachmentVO(attachment.getId(), attachment.getFileName(), attachment.getFileSize(),
                attachment.getMimeType(), attachment.getUploaderId(), attachment.getSpaceId(), downloadUrl,
                attachment.getGmtCreate());
    }
}
