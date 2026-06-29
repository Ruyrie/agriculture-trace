package com.example.agriculturetrace.controller;

import com.example.agriculturetrace.service.WarningService;
import com.example.agriculturetrace.util.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 预警中心接口。
 *
 * 全部接口受 /api/warnings/** → hasAnyRole(ADMIN, INSPECTOR) 保护（见 SecurityConfig）。
 * 预警由 WarningService 实时计算，不依赖独立预警表。
 */
@RestController
@RequestMapping("/api/warnings")
public class WarningController {

    private final WarningService warningService;

    public WarningController(WarningService warningService) {
        this.warningService = warningService;
    }

    /**
     * 返回全部预警，可按级别（HIGH/MEDIUM/LOW）筛选。
     * 预警数量通常有限，直接返回完整列表，由前端表格分页展示。
     */
    @GetMapping
    public Result<?> list(@RequestParam(required = false) String level) {
        List<Map<String, Object>> warnings = warningService.scan();
        if (level != null && !level.isBlank()) {
            warnings = warnings.stream()
                    .filter(w -> level.trim().equalsIgnoreCase(String.valueOf(w.get("level"))))
                    .toList();
        }
        return Result.success(warnings);
    }

    /**
     * 返回各级别预警数量汇总，供顶部卡片展示。
     */
    @GetMapping("/summary")
    public Result<?> summary() {
        return Result.success(warningService.summary());
    }
}
