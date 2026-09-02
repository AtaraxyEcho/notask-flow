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
 * 笔记 Elasticsearch 搜索文档。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "notask_notes", createIndex = false)
public class NoteSearchDocument {

    /** 笔记标识。 */
    @Id
    @Field(type = FieldType.Long)
    private Long noteId;

    /** 空间标识。 */
    @Field(type = FieldType.Long)
    private Long spaceId;

    /** 笔记本标识。 */
    @Field(type = FieldType.Long)
    private Long notebookId;

    /** 项目标识。 */
    @Field(type = FieldType.Long)
    private Long projectId;

    /** 笔记标题。 */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    /** Markdown 正文。 */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;

    /** HTML 正文。 */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String contentHtml;

    /** 修改时间。 */
    @Field(type = FieldType.Date)
    private LocalDateTime gmtModified;
}
