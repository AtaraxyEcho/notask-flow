package com.notaskflow.domain.vo;

import com.notaskflow.common.enums.SpaceType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 空间视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpaceVO {

    private Long id;

    private String name;

    private SpaceType type;

    private Long ownerUserId;

    private Long memberCount;

    private Long unreadCount;

    private Boolean joinApprovalRequired;

    private LocalDateTime gmtCreate;
}
