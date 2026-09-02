package com.notaskflow.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 笔记本实体。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("nt_notebook")
public class Notebook extends BaseEntity {

    private Long spaceId;

    private Long parentId;

    private String path;

    private String name;

    private Integer sortOrder;
}
