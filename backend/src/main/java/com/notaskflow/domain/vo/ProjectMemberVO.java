package com.notaskflow.domain.vo;

import com.notaskflow.common.enums.ProjectMemberRole;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 项目成员视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberVO {

    private Long projectId;

    private Long userId;

    private String username;

    private String nickname;

    private String email;

    private String avatarUrl;

    private ProjectMemberRole role;

    private LocalDateTime joinedAt;
}
