package com.notaskflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notaskflow.domain.entity.Project;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目 Mapper。
 *
 * @author LIN
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
}
