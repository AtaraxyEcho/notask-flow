package com.notaskflow.domain.dto.request;

import com.notaskflow.common.enums.BusinessType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 附件绑定请求。
 *
 * @author LIN
 */
@Data
public class AttachmentBindRequest {

    @NotNull(message = "附件ID不能为空")
    private Long attachmentId;

    @NotNull(message = "业务类型不能为空")
    private BusinessType businessType;

    @NotNull(message = "业务ID不能为空")
    private Long businessId;

    @Size(max = 100, message = "引用标识长度不能超过100")
    private String referenceKey;
}
