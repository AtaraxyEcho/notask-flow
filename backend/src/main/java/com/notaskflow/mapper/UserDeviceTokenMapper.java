package com.notaskflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notaskflow.domain.entity.UserDeviceToken;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户移动端推送设备令牌 Mapper。
 *
 * @author LIN
 */
@Mapper
public interface UserDeviceTokenMapper extends BaseMapper<UserDeviceToken> {
}
