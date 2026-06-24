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

    /**
     * 统计产品总数。
     */
    long countProducts();

    /**
     * 统计批次总数。
     */
    long countBatches();

    /**
     * 统计溯源访问记录总数。
     */
    long countTraceRecords();

    /**
     * 按产品类别聚合数量，供首页饼图使用。
     */
    List<Map<String, Object>> categoryDistribution();

    /**
     * 按日期聚合溯源访问次数，供近 7 天趋势图使用。
     */
    List<Map<String, Object>> traceTrend();

    /**
     * 按月份统计批次产出量。
     */
    List<Map<String, Object>> monthlyBatchOutput();

    /**
     * 统计溯源访问排行。
     */
    List<Map<String, Object>> traceRanking();

    /**
     * 按产地聚合产品分布。
     */
    List<Map<String, Object>> originDistribution();

    /**
     * 按产品统计批次数量。
     */
    List<Map<String, Object>> productBatchOutput();

    /**
     * 按链上日志操作类型聚合数量，用于数据概览页链上操作图表。
     */
    List<Map<String, Object>> blockchainActionMix();
}
