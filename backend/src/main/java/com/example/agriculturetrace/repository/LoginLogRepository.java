package com.example.agriculturetrace.repository;

import com.example.agriculturetrace.entity.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 登录日志仓库。
 *
 * 继承 JpaSpecificationExecutor 以支持按状态、用户名、时间范围的动态筛选。
 */
public interface LoginLogRepository extends JpaRepository<LoginLog, String>, JpaSpecificationExecutor<LoginLog> {

    /**
     * 统计指定结果（SUCCESS/FAILURE）的登录次数，供管理员汇总展示。
     */
    long countByStatus(String status);
}
