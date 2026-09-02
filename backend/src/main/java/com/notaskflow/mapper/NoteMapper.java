package com.notaskflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notaskflow.domain.entity.Note;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 笔记 Mapper。
 *
 * @author LIN
 */
public interface NoteMapper extends BaseMapper<Note> {

    /**
     * 增加笔记浏览次数。
     *
     * @param noteId 笔记标识
     * @param delta 增加数量
     * @return 更新行数
     */
    @Update("UPDATE nt_note SET view_count = view_count + #{delta} WHERE id = #{noteId} AND is_deleted = 0")
    int incrementViewCount(@Param("noteId") Long noteId, @Param("delta") Long delta);
}
