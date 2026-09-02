package com.notaskflow.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一分页响应对象。
 *
 * @param <T> 列表元素类型
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private Long total;

    private Long pageNum;

    private Long pageSize;

    private List<T> list;

    /**
     * 根据 MyBatis-Plus 分页结果构造响应对象。
     *
     * @param page 分页结果
     * @param list 转换后的列表
     * @param <T> 列表元素类型
     * @return 分页响应
     */
    public static <T> PageResponse<T> of(Page<?> page, List<T> list) {
        return new PageResponse<>(page.getTotal(), page.getCurrent(), page.getSize(), list);
    }
}
