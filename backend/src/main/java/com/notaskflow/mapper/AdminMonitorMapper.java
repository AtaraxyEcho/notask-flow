package com.notaskflow.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 管理端监控数据 Mapper。
 *
 * @author LIN
 */
@Mapper
public interface AdminMonitorMapper {

    /**
     * 查询 MySQL 全局状态变量。
     *
     * @param name 状态变量名称
     * @return 状态变量行
     */
    @Select("SHOW GLOBAL STATUS LIKE #{name}")
    List<Map<String, Object>> selectGlobalStatus(@Param("name") String name);
}
