package com.example.agriculturetrace;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 系统启动入口。
 *
 * MapperScan 只扫描 MyBatis 统计接口，业务 CRUD 由 JPA Repository 承担，
 * 与需求文档中的“JPA + MyBatis”分工保持一致。
 */
@MapperScan("com.example.agriculturetrace.mapper")
@SpringBootApplication
public class AgricultureTraceApplication {

    /**
     * Spring Boot 应用启动入口，负责加载容器、扫描组件并启动内嵌 Web 服务。
     */
    public static void main(String[] args) {
        SpringApplication.run(AgricultureTraceApplication.class, args);
    }
}
