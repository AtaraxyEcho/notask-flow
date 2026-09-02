package com.notaskflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notaskflow.domain.entity.NoteTag;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 笔记标签关联 Mapper。
 *
 * @author LIN
 */
public interface NoteTagMapper extends BaseMapper<NoteTag> {

    /**
     * 按笔记物理删除全部标签关联。
     *
     * @param noteId 笔记标识
     * @return 受影响行数
     */
    @Delete("DELETE FROM nt_note_tag WHERE note_id = #{noteId}")
    int physicalDeleteByNoteId(@Param("noteId") Long noteId);

    /**
     * 按笔记列表物理删除全部标签关联。
     *
     * @param noteIds 笔记标识列表
     * @return 受影响行数
     */
    @Delete({
            "<script>",
            "DELETE FROM nt_note_tag",
            "WHERE note_id IN",
            "<foreach collection='noteIds' item='noteId' open='(' separator=',' close=')'>",
            "#{noteId}",
            "</foreach>",
            "</script>"
    })
    int physicalDeleteByNoteIds(@Param("noteIds") List<Long> noteIds);

    /**
     * 按标签物理删除全部笔记关联。
     *
     * @param tagId 标签标识
     * @return 受影响行数
     */
    @Delete("DELETE FROM nt_note_tag WHERE tag_id = #{tagId}")
    int physicalDeleteByTagId(@Param("tagId") Long tagId);

    /**
     * 按笔记和标签物理删除单条关联。
     *
     * @param noteId 笔记标识
     * @param tagId 标签标识
     * @return 受影响行数
     */
    @Delete("DELETE FROM nt_note_tag WHERE note_id = #{noteId} AND tag_id = #{tagId}")
    int physicalDeleteByNoteIdAndTagId(@Param("noteId") Long noteId, @Param("tagId") Long tagId);
}
