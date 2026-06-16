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

    /**
     * 返回仪表盘顶部三项核心统计：产品数、批次数、溯源访问记录数。
     */
    @GetMapping("/statistics")
    public Result<?> statistics() {
        return Result.success(Map.of(
                "productCount", statisticsMapper.countProducts(),
                "batchCount", statisticsMapper.countBatches(),
                "traceCount", statisticsMapper.countTraceRecords()
        ));
    }
    /**Result<?>表示这个方法返回的是项目统一响应格式 Result<?> 表示里面的数据类型不固定，这里实际返回的是一个 Map
        *Map.of(...) 是 Java 里快速创建键值对对象的方法
        *项目里接口一般不是直接返回数据,拿去包装过的数据
     * /

    /**
     * 返回产品类别分布，用于首页饼图展示各类别产品占比。
     */
    @GetMapping("/categoryDistribution")
    public Result<?> categoryDistribution() {
        return Result.success(statisticsMapper.categoryDistribution());
    }

    /**
     * 返回最近 7 天溯源访问趋势。
     * 即使某天没有访问，也会补 0，保证前端折线图横轴固定连续。
     */
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

    /**
     * 返回统计分析页的多组报表数据，包括月度批次、溯源排行、产地分布等。
     */
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
