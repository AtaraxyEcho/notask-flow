package com.notaskflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notaskflow.domain.entity.Attachment;
import java.util.List;
import org.apache.ibatis.annotations.Select;

/**
 * 附件 Mapper。
 *
 * @author LIN
 */
public interface AttachmentMapper extends BaseMapper<Attachment> {

    /**
     * 查询已逻辑删除附件的存储统计字段。
     *
     * @return 已逻辑删除附件列表
     */
    @Select("""
            SELECT id, file_size
            FROM nt_attachment
            WHERE is_deleted = 1
            """)
    List<Attachment> selectDeletedStorageRows();
}
