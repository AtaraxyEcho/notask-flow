package com.notaskflow.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户选择项视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOptionVO {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private String avatarUrl;
}
