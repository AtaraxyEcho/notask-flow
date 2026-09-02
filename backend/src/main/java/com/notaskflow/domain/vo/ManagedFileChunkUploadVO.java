package com.notaskflow.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件分片上传会话视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagedFileChunkUploadVO {

    private String uploadToken;

    private Long chunkSize;

    private Integer totalChunks;

    private Integer expiresIn;
}
