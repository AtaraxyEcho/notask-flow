package com.notaskflow.common.enums;

/**
 * 文件处理操作类型。
 *
 * @author LIN
 */
public enum FileProcessOperation {

    /** 文件已上传 */
    UPLOADED,

    /** 文件已移入回收站 */
    TRASHED,

    /** 文件已从回收站恢复 */
    RESTORED,

    /** 文件已物理删除 */
    PHYSICAL_DELETED
}
