package com.notaskflow.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 待办实体。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("nt_todo")
public class Todo extends BaseEntity {

    private Long spaceId;

    private Long userId;

    private Long taskMemberId;

    private String title;

    @TableField("is_completed")
    private Boolean isCompleted;

    private LocalDateTime deadline;
}
