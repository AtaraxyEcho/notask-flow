package com.notaskflow.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成员任务负载视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberTaskLoadVO {

    private Long userId;

    private String username;

    private Long loadCount;

    private Long completedCount;
}
