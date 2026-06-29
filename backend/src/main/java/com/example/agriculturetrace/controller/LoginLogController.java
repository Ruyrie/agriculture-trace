package com.example.agriculturetrace.controller;

import com.example.agriculturetrace.entity.LoginLog;
import com.example.agriculturetrace.service.LoginLogService;
import com.example.agriculturetrace.util.Result;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 登录日志查询接口。
 *
 * 全部接口受 /api/login-logs/** → hasAnyRole(ADMIN, INSPECTOR) 保护（见 SecurityConfig），
 * 供管理员和监管员做登录安全审计。日志的写入在 SecurityConfig 的认证处理器中完成。
 */
@RestController
@RequestMapping("/api/login-logs")
public class LoginLogController {

    private final LoginLogService loginLogService;

    public LoginLogController(LoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    /**
     * 分页查询登录日志，支持按结果、用户名/IP 关键字、时间范围筛选。
     */
    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String startTime,
                          @RequestParam(required = false) String endTime) {
        Page<LoginLog> logs = loginLogService.list(status, keyword, startTime, endTime, page, pageSize);
        return Result.success(Map.of(
                "records", logs.getContent().stream().map(loginLogService::toRow).toList(),
                "total", logs.getTotalElements(),
                "page", logs.getNumber() + 1,
                "pageSize", logs.getSize()));
    }

    /**
     * 登录日志汇总：总次数、成功次数、失败次数。
     */
    @GetMapping("/summary")
    public Result<?> summary() {
        return Result.success(loginLogService.summary());
    }
}
