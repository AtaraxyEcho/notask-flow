package com.notaskflow.domain.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件夹视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileFolderVO {

    private Long id;

    private Long spaceId;

    private Long parentId;

    private String name;

    private Integer sortOrder;

    private Long createdBy;

    private LocalDateTime gmtCreate;

    private List<FileFolderVO> children;
}
