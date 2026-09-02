package com.notaskflow.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件预签名上传地址视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagedFileUploadUrlVO {

    private String uploadToken;

    private String uploadUrl;

    private String method;

    private Integer expiresIn;
}
