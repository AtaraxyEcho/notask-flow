package com.notaskflow.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 含主键、逻辑删除和审计时间的基础实体。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableLogic
    @TableField(value = "is_deleted", fill = FieldFill.INSERT)
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;
}
