package com.notaskflow.service;

import com.notaskflow.common.enums.NoteExportFormat;
import com.notaskflow.domain.vo.NoteExportFileVO;

/**
 * 笔记导出服务。
 *
 * @author LIN
 */
public interface NoteExportService {

    /**
     * 导出笔记文件。
     *
     * @param spaceId 空间标识
     * @param noteId 笔记标识
     * @param format 导出格式
     * @return 导出文件
     */
    NoteExportFileVO export(Long spaceId, Long noteId, NoteExportFormat format);
}
