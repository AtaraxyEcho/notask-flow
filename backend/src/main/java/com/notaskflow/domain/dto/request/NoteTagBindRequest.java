package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

/**
 * 笔记标签绑定请求。
 *
 * @author LIN
 */
@Data
public class NoteTagBindRequest {

    @NotEmpty(message = "标签ID列表不能为空")
    private List<Long> tagIds;
}
