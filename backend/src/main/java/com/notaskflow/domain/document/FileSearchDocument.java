package com.notaskflow.domain.document;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * 文件 Elasticsearch 搜索文档。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "notask_files", createIndex = false)
public class FileSearchDocument {

    /** 文件管理条目标识。 */
    @Id
    @Field(type = FieldType.Long)
    private Long fileId;

    /** 附件标识。 */
    @Field(type = FieldType.Long)
    private Long attachmentId;

    /** 空间标识。 */
    @Field(type = FieldType.Long)
    private Long spaceId;

    /** 文件夹标识。 */
    @Field(type = FieldType.Long)
    private Long folderId;

    /** 展示文件名。 */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String displayName;

    /** 原始文件名。 */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String fileName;

    /** 文件类型。 */
    @Field(type = FieldType.Keyword)
    private String mimeType;

    /** 文件大小。 */
    @Field(type = FieldType.Long)
    private Long fileSize;

    /** 上传用户标识。 */
    @Field(type = FieldType.Long)
    private Long uploaderId;

    /** 创建用户标识。 */
    @Field(type = FieldType.Long)
    private Long createdBy;

    /** 是否在回收站。 */
    @Field(type = FieldType.Boolean)
    private Boolean trashed;

    /** 后续文件正文提取内容。 */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String extractedText;

    /** 创建时间。 */
    @Field(type = FieldType.Date)
    private LocalDateTime gmtCreate;

    /** 修改时间。 */
    @Field(type = FieldType.Date)
    private LocalDateTime gmtModified;
}
