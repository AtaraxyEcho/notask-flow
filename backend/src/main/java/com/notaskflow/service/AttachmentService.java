package com.notaskflow.service;

import com.notaskflow.domain.dto.request.AttachmentBindRequest;
import com.notaskflow.domain.dto.request.AttachmentUnbindRequest;
import com.notaskflow.domain.vo.AttachmentVO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 附件服务接口。
 *
 * @author LIN
 */
public interface AttachmentService {

    /**
     * 上传附件。
     *
     * @param spaceId 空间标识
     * @param file 文件对象
     * @return 附件信息
     */
    AttachmentVO upload(Long spaceId, MultipartFile file);

    /**
     * 查询附件元信息。
     *
     * @param id 附件标识
     * @return 附件信息
     */
    AttachmentVO get(Long spaceId, Long id);

    /**
     * 生成下载地址。
     *
     * @param id 附件标识
     * @return 附件信息
     */
    AttachmentVO download(Long spaceId, Long id);

    /**
     * 删除附件及业务绑定。
     *
     * @param id 附件标识
     */
    void delete(Long spaceId, Long id);

    /**
     * 绑定附件到业务对象。
     *
     * @param request 绑定请求
     */
    void bind(Long spaceId, AttachmentBindRequest request);

    /**
     * 解除附件业务绑定。
     *
     * @param id 附件标识
     * @param request 解绑请求
     */
    void unbind(Long spaceId, Long id, AttachmentUnbindRequest request);

    /**
     * 查询笔记附件。
     *
     * @param spaceId 空间标识
     * @param noteId 笔记标识
     * @return 附件列表
     */
    List<AttachmentVO> listNoteAttachments(Long spaceId, Long noteId);

    /**
     * 查询任务附件。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     * @return 附件列表
     */
    List<AttachmentVO> listTaskAttachments(Long spaceId, Long taskId);
}
