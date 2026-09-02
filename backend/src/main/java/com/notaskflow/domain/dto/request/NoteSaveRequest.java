package com.notaskflow.domain.dto.request;

import com.notaskflow.common.enums.NoteHistorySaveType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 笔记保存请求。
 *
 * @author LIN
 */
@Data
public class NoteSaveRequest {

    @NotNull(message = "笔记本ID不能为空")
    private Long notebookId;

    @NotBlank(message = "笔记标题不能为空")
    @Size(max = 200, message = "笔记标题长度不能超过200")
    private String title;

    private Long projectId;

    private String content;

    private String contentHtml;

    private Boolean isPublic = false;

    private List<Long> tagIds = new ArrayList<>();

    private NoteHistorySaveType saveType = NoteHistorySaveType.AUTO;
}
