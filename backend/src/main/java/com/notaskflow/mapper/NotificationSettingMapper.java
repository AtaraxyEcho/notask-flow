package com.notaskflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notaskflow.domain.entity.NotificationSetting;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知偏好 Mapper。
 *
 * @author LIN
 */
@Mapper
public interface NotificationSettingMapper extends BaseMapper<NotificationSetting> {
}
