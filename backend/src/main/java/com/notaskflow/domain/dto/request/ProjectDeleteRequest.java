package com.notaskflow.domain.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 项目删除请求，声明关联任务与文档的处理方式。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDeleteRequest {

    /** 关联任务处理策略，为空时按「解除关联」处理。 */
    private ProjectRelationStrategy taskStrategy;

    /** 任务转移目标项目标识，仅 TRANSFER 策略必填。 */
    private Long taskTargetProjectId;

    /** 关联文档处理策略，为空时按「解除关联」处理。 */
    private ProjectRelationStrategy docStrategy;

    /** 文档转移目标项目标识，仅 TRANSFER 策略必填。 */
    private Long docTargetProjectId;
}
