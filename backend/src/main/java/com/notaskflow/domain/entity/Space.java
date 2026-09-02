package com.notaskflow.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.notaskflow.common.enums.SpaceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 空间实体。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("nt_space")
public class Space extends BaseEntity {

    private String name;

    private SpaceType type;

    private Long ownerUserId;

    private Boolean joinApprovalRequired;
}
