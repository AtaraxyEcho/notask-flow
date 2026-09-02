//package com.notaskflow.config;
//
//import java.sql.Connection;
//import java.util.Arrays;
//import java.util.Comparator;
//import javax.sql.DataSource;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.io.Resource;
//import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
//import org.springframework.core.io.support.ResourcePatternResolver;
//import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
//
///**
// * 数据库补丁配置，应用启动时按文件名顺序执行幂等结构补丁脚本。
// *
// * @author LIN
// */
//@Slf4j
//@Configuration
//@RequiredArgsConstructor
//public class DatabasePatchConfig implements ApplicationRunner {
//
//    private static final String PATCH_LOCATION_PATTERN = "classpath:/db/patch/*.sql";
//
//    private final DataSource dataSource;
//
//    /**
//     * 执行数据库结构补丁。
//     *
//     * @param args 启动参数
//     * @throws Exception 补丁脚本读取或执行失败时抛出
//     */
//    @Override
//    public void run(ApplicationArguments args) throws Exception {
//        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
//        Resource[] resources = resolver.getResources(PATCH_LOCATION_PATTERN);
//        if (resources.length == 0) {
//            return;
//        }
//        Arrays.sort(resources, Comparator.comparing(Resource::getFilename, Comparator.nullsLast(String::compareTo)));
//        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
//        populator.setContinueOnError(false);
//        for (Resource resource : resources) {
//            populator.addScript(resource);
//            log.info("加载数据库补丁脚本，file={}", resource.getFilename());
//        }
//        try (Connection connection = dataSource.getConnection()) {
//            populator.populate(connection);
//        }
//        log.info("数据库补丁脚本执行完成，count={}", resources.length);
//    }
//}
