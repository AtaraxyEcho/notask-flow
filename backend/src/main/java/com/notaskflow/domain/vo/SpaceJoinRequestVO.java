package com.notaskflow.domain.vo;

import com.notaskflow.common.enums.JoinRequestStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 团队加入申请视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpaceJoinRequestVO {

    private Long id;

    private Long applicantUserId;

    private String applicantUsername;

    private String applicantEmail;

    private Long supervisorUserId;

    private String supervisorUsername;

    private Long targetSpaceId;

    private String targetSpaceName;

    private String teamName;

    private JoinRequestStatus status;

    private String remark;

    private String rejectReason;

    private Long reviewerUserId;

    private LocalDateTime reviewedAt;

    private LocalDateTime gmtCreate;
}
