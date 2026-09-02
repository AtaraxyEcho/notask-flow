package com.notaskflow.domain.vo;

import com.notaskflow.common.enums.NoteHistorySaveType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 笔记历史版本视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteHistoryVO {

    private Long id;

    private Long noteId;

    private String title;

    private String content;

    private Integer version;

    private String changeSummary;

    private NoteHistorySaveType saveType;

    private LocalDateTime gmtCreate;
}
