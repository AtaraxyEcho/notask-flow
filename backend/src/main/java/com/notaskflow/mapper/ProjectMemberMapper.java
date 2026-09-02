package com.notaskflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notaskflow.domain.entity.ProjectMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目成员 Mapper。
 *
 * @author LIN
 */
@Mapper
public interface ProjectMemberMapper extends BaseMapper<ProjectMember> {
}
