package com.notaskflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notaskflow.domain.entity.SystemSetting;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统设置数据访问接口。
 *
 * @author LIN
 */
@Mapper
public interface SystemSettingMapper extends BaseMapper<SystemSetting> {
}
