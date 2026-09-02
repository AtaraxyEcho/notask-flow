package com.notaskflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notaskflow.common.enums.BusinessType;
import com.notaskflow.domain.entity.BusinessAttachment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 业务附件关联 Mapper。
 *
 * @author LIN
 */
public interface BusinessAttachmentMapper extends BaseMapper<BusinessAttachment> {

    /**
     * 物理删除指定附件的所有业务引用。
     *
     * @param attachmentId 附件标识
     * @return 删除行数
     */
    @Delete("""
            DELETE FROM nt_business_attachment
            WHERE attachment_id = #{attachmentId}
            """)
    int physicalDeleteByAttachmentId(@Param("attachmentId") Long attachmentId);

    /**
     * 物理删除指定业务对象上的附件引用。
     *
     * @param attachmentId 附件标识
     * @param businessType 业务类型
     * @param businessId 业务标识
     * @param referenceKey 引用标识
     * @return 删除行数
     */
    @Delete("""
            DELETE FROM nt_business_attachment
            WHERE attachment_id = #{attachmentId}
              AND business_type = #{businessType}
              AND business_id = #{businessId}
              AND reference_key = #{referenceKey}
            """)
    int physicalDeleteReference(@Param("attachmentId") Long attachmentId,
                                @Param("businessType") BusinessType businessType,
                                @Param("businessId") Long businessId,
                                @Param("referenceKey") String referenceKey);
}
