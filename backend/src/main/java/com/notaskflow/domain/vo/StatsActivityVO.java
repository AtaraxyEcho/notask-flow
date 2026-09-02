package com.notaskflow.domain.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统计页近期动态视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsActivityVO {

    private LocalDateTime time;

    private Long memberUserId;

    private String member;

    private String type;

    private String content;

    private String impact;
}
