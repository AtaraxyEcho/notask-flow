package com.notaskflow.domain.vo;

import com.notaskflow.common.enums.BusinessType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件引用视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileReferenceVO {

    private Long id;

    private Long attachmentId;

    private BusinessType businessType;

    private Long businessId;

    private String referenceKey;

    private LocalDateTime gmtCreate;
}
