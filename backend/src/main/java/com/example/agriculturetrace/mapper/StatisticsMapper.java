package com.example.agriculturetrace.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计 MyBatis Mapper。
 *
 * 聚合类 SQL 放到 MyBatis 中，避免在控制器里拼接统计逻辑。
 */
@Mapper
public interface StatisticsMapper {

    long countProducts();

    long countBatches();

    long countTraceRecords();

    List<Map<String, Object>> categoryDistribution();

    List<Map<String, Object>> traceTrend();

    List<Map<String, Object>> monthlyBatchOutput();

    List<Map<String, Object>> traceRanking();

    List<Map<String, Object>> originDistribution();

    List<Map<String, Object>> productBatchOutput();
}
