package com.notaskflow.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 笔记标签关联实体，记录笔记与标签的绑定关系。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("nt_note_tag")
public class NoteTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noteId;

    private Long tagId;

    @TableLogic
    @TableField(value = "is_deleted", fill = FieldFill.INSERT)
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;
}
