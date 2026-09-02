package com.notaskflow.domain.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 笔记本视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotebookVO {

    private Long id;

    private Long spaceId;

    private Long parentId;

    private String path;

    private String name;

    private Integer sortOrder;

    private LocalDateTime gmtCreate;

    private List<NotebookVO> children = new ArrayList<>();
}
