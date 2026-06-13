package com.example.agriculturetrace.controller;

import com.example.agriculturetrace.mapper.StatisticsMapper;
import com.example.agriculturetrace.util.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 仪表盘与统计分析接口。
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final StatisticsMapper statisticsMapper;

    public DashboardController(StatisticsMapper statisticsMapper) {
        this.statisticsMapper = statisticsMapper;
    }

    @GetMapping("/statistics")
    public Result<?> statistics() {
        return Result.success(Map.of(
                "productCount", statisticsMapper.countProducts(),
                "batchCount", statisticsMapper.countBatches(),
                "traceCount", statisticsMapper.countTraceRecords()
        ));
    }

    @GetMapping("/categoryDistribution")
    public Result<?> categoryDistribution() {
        return Result.success(statisticsMapper.categoryDistribution());
    }

    @GetMapping("/traceTrend")
    public Result<?> traceTrend() {
        var rows = statisticsMapper.traceTrend();
        Map<String, Object> countByDate = new LinkedHashMap<>();
        rows.forEach(row -> countByDate.put(String.valueOf(row.get("date")), row.get("count")));

        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        var dates = java.util.stream.IntStream.rangeClosed(0, 6)
                .mapToObj(offset -> today.minusDays(6L - offset).format(formatter))
                .toList();
        var counts = dates.stream()
                .map(date -> Objects.requireNonNullElse(countByDate.get(date), 0))
                .toList();

        return Result.success(Map.of(
                "dates", dates,
                "counts", counts
        ));
    }

    @GetMapping("/reports")
    public Result<?> reports() {
        return Result.success(Map.of(
                "monthlyBatchOutput", statisticsMapper.monthlyBatchOutput(),
                "traceRanking", statisticsMapper.traceRanking(),
                "originDistribution", statisticsMapper.originDistribution(),
                "productBatchOutput", statisticsMapper.productBatchOutput()
        ));
    }
}
