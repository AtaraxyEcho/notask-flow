package com.notaskflow.domain.dto.request;

/**
 * 项目删除关联数据处理策略。
 *
 * @author LIN
 */
public enum ProjectRelationStrategy {

    /** 将关联数据转移到其他项目。 */
    TRANSFER,

    /** 仅解除与项目的关联。 */
    UNLINK,

    /** 直接删除关联数据。 */
    DELETE;

    /**
     * 规范化策略值，未指定时默认为解除关联。
     *
     * @param value 原始值
     * @return 规范化后的策略
     */
    public static ProjectRelationStrategy normalize(ProjectRelationStrategy value) {
        return value == null ? UNLINK : value;
    }
}
