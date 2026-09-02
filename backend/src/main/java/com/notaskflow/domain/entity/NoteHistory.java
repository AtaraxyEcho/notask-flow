package com.notaskflow.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.notaskflow.common.enums.NoteHistorySaveType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 笔记历史版本实体。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("nt_note_history")
public class NoteHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noteId;

    private String title;

    private String content;

    private Integer version;

    private String changeSummary;

    private NoteHistorySaveType saveType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    @TableLogic
    @TableField(value = "is_deleted", fill = FieldFill.INSERT)
    private Boolean isDeleted;
}
