package com.notaskflow.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统设置视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingVO {

    private String settingKey;

    private String settingValue;

    private String description;
}
