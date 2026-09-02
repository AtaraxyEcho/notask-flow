package com.notaskflow.domain.dto.request;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 笔记分享请求。
 *
 * @author LIN
 */
@Data
public class NoteShareRequest {

    private LocalDateTime expireAt;
}
