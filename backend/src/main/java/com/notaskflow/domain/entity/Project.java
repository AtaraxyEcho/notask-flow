package com.notaskflow.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 项目实体。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("nt_project")
public class Project extends BaseEntity {

    private Long spaceId;

    private String name;

    private String description;

    private String coverColor;

    private String coverImageUrl;

    @TableField("is_archived")
    private Boolean archived;

    private Long ownerUserId;
}
