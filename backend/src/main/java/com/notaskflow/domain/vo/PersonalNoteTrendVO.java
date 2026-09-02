package com.notaskflow.domain.vo;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 个人笔记趋势视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalNoteTrendVO {

    private LocalDate date;

    private Long createdCount;

    private Long updatedCount;
}
