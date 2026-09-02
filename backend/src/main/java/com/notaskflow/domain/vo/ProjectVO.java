package com.notaskflow.domain.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 项目视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectVO {

    private Long id;

    private Long spaceId;

    private String name;

    private String description;

    private String coverColor;

    private String coverImageUrl;

    private Boolean archived;

    private Long ownerUserId;

    private Long taskCount;

    private Long completedTaskCount;

    private Long overdueTaskCount;

    private Long documentCount;

    private Integer completionRate;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    private List<ProjectMemberVO> members = new ArrayList<>();
}
