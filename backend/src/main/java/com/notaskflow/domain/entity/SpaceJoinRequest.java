package com.notaskflow.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.notaskflow.common.enums.JoinRequestStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 团队加入申请实体，记录申请人和审批人之间的审批流程。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("nt_space_join_request")
public class SpaceJoinRequest extends BaseEntity {

    private Long applicantUserId;

    private Long supervisorUserId;

    private Long targetSpaceId;

    private String teamName;

    private JoinRequestStatus status;

    private String remark;

    private String rejectReason;

    private Long reviewerUserId;

    private LocalDateTime reviewedAt;
}
