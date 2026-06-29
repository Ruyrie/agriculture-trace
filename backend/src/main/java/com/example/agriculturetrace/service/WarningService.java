package com.example.agriculturetrace.service;

import com.example.agriculturetrace.entity.Batch;
import com.example.agriculturetrace.entity.InspectionRecord;
import com.example.agriculturetrace.repository.BatchRepository;
import com.example.agriculturetrace.repository.InspectionRecordRepository;
import com.example.agriculturetrace.repository.LogisticsRecordRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 预警中心业务服务。
 *
 * 预警实时从现有业务数据计算，不落库（不引入会过期的预警表），保证每次查询都是最新结论。
 *
 * 质检结果采用"白名单"判定（只有明确写明合格才算通过），避免录入无意义结果蒙混过关：
 *   - HIGH   质检不合格：结果命中不合格关键字（不合格/异常/阳性/检出超标 等）。
 *   - MEDIUM 质检结果存疑：结果既非明确合格也非明确不合格（如"随意/测试/空"），需人工复核。
 *   - MEDIUM 缺少质检：批次没有任何质检记录，无法证明质量。
 *   - LOW    缺少物流：批次没有任何物流轨迹，流向不可追。
 * 同一批次的多条问题由 scan() 聚合为一行，级别取最高。
 *
 * 关联：
 *   - InspectionRecordRepository.findAllWithBatch()/findDistinctBatchIds()
 *   - LogisticsRecordRepository.findDistinctBatchIds()
 *   - BatchRepository.findAllWithProduct()
 *   - WarningController 暴露 /api/warnings 列表与汇总接口（ADMIN/INSPECTOR）。
 */
@Service
public class WarningService {

    /**
     * 质检结果"不合格"判定关键字（命中其一 → 高危）。
     * 顺序上先于 PASS 判定，所以"不合格""不达标""不符合"会被正确判为不合格，
     * 而不会因为分别包含"合格""达标""符合"被误判为通过。
     * 刻意不含单独的"超标"，避免把"未检出超标"这类合格结论误伤（用"检出超标/残留超标"更精确）。
     */
    private static final List<String> FAIL_KEYWORDS = List.of(
            "不合格", "不通过", "未通过", "不达标", "未达标", "不符合", "异常", "阳性", "检出超标", "残留超标");

    /**
     * 否定式"合格"表述：这些词把后面的不合格语义否定掉了（如"未检出超标""无异常"="合格"）。
     * 先于 FAIL 判定，避免它们因包含"检出超标""异常"等 FAIL 子串被误判为不合格。
     */
    private static final List<String> PASS_NEGATIONS = List.of(
            "未检出", "未超标", "无超标", "无异常", "无残留");

    /**
     * 质检结果"合格/通过"判定关键字（采用白名单：只有明确写明通过才视为合格）。
     * 这样设计的目的：不能靠"没写不合格"就当作合格——录入"随意""测试"之类无意义结果，
     * 既不命中 FAIL 也不命中 PASS，会被判为"存疑"并照样预警，避免问题批次蒙混过关。
     */
    private static final List<String> PASS_KEYWORDS = List.of(
            "合格", "未检出", "正常", "通过", "达标", "符合", "阴性");

    private final InspectionRecordRepository inspectionRecordRepository;
    private final LogisticsRecordRepository logisticsRecordRepository;
    private final BatchRepository batchRepository;

    public WarningService(InspectionRecordRepository inspectionRecordRepository,
                          LogisticsRecordRepository logisticsRecordRepository,
                          BatchRepository batchRepository) {
        this.inspectionRecordRepository = inspectionRecordRepository;
        this.logisticsRecordRepository = logisticsRecordRepository;
        this.batchRepository = batchRepository;
    }

    /**
     * 扫描并返回预警列表，<strong>以批次为单位聚合</strong>：
     * 同一批次的多个问题（如同时缺质检与缺物流）合并成一行，
     * 该行级别取其中最高的（HIGH→MEDIUM→LOW），并在 issues 中保留每条子问题。
     * 结果按批次最高级别排序。
     */
    public List<Map<String, Object>> scan() {
        // 先得到逐条问题，再按批次聚合，避免同一批次在列表里重复出现多行。
        LinkedHashMap<String, List<Map<String, Object>>> byBatch = new LinkedHashMap<>();
        int orphanSeq = 0;
        for (Map<String, Object> issue : scanIssues()) {
            Object targetId = issue.get("targetId");
            // targetId 为空属脏数据，给唯一键让其独立成行，不与他人误聚合。
            String key = targetId != null ? targetId.toString() : "__orphan_" + (orphanSeq++);
            byBatch.computeIfAbsent(key, k -> new ArrayList<>()).add(issue);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (List<Map<String, Object>> batchIssues : byBatch.values()) {
            result.add(aggregateBatch(batchIssues));
        }
        // 批次行按最高级别排序：HIGH 在前，LOW 在后。
        result.sort(java.util.Comparator.comparingInt(row -> levelRank((String) row.get("level"))));
        return result;
    }

    /**
     * 把同一批次的多条问题聚合成一行：级别取最高，时间取最高级别问题的时间，
     * 并保留 issues 子列表与合并后的 typeLabel/message 文本，方便前端逐条展示或简单显示。
     */
    private Map<String, Object> aggregateBatch(List<Map<String, Object>> issues) {
        // 最高级别问题决定整批次的级别和展示时间。
        Map<String, Object> top = issues.stream()
                .min(java.util.Comparator.comparingInt(i -> levelRank((String) i.get("level"))))
                .orElse(issues.get(0));

        List<Map<String, Object>> subIssues = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        for (Map<String, Object> issue : issues) {
            Map<String, Object> sub = new LinkedHashMap<>();
            sub.put("level", issue.get("level"));
            sub.put("type", issue.get("type"));
            sub.put("typeLabel", issue.get("typeLabel"));
            sub.put("message", issue.get("message"));
            subIssues.add(sub);
            String label = String.valueOf(issue.get("typeLabel"));
            if (!labels.contains(label)) {
                labels.add(label);
            }
            messages.add(String.valueOf(issue.get("message")));
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("level", top.get("level"));
        row.put("targetType", "BATCH");
        row.put("targetId", top.get("targetId"));
        row.put("targetName", top.get("targetName"));
        row.put("issueCount", issues.size());
        row.put("issues", subIssues);
        // 合并文本仅用于简单展示/导出；前端优先用 issues 数组逐条渲染标签。
        row.put("typeLabel", String.join("、", labels));
        row.put("message", String.join("；", messages));
        row.put("time", top.get("time"));
        return row;
    }

    /**
     * 级别排序权重：HIGH 最高（0），其次 MEDIUM、LOW，未知级别排最后。
     */
    private int levelRank(String level) {
        return switch (level == null ? "" : level) {
            case "HIGH" -> 0;
            case "MEDIUM" -> 1;
            case "LOW" -> 2;
            default -> 3;
        };
    }

    /**
     * 逐条扫描所有问题（未聚合），按严重级别（HIGH→MEDIUM→LOW）排序。
     * 聚合由 scan() 负责；本方法保留细粒度问题，供聚合时拆分到 issues。
     */
    private List<Map<String, Object>> scanIssues() {
        List<Map<String, Object>> high = new ArrayList<>();
        List<Map<String, Object>> medium = new ArrayList<>();
        List<Map<String, Object>> low = new ArrayList<>();

        // 规则一：按质检结果判定。
        //   FAIL    → 高危（质检不合格）
        //   UNKNOWN → 中风险（结果存疑：既非明确合格也非明确不合格，如"随意/测试/空"），需人工复核
        //   PASS    → 不预警
        for (InspectionRecord record : inspectionRecordRepository.findAllWithBatch()) {
            String verdict = classifyResult(record.getResult());
            Batch batch = record.getBatch();
            String batchId = batch != null ? batch.getId() : null;
            if ("FAIL".equals(verdict)) {
                high.add(warning(
                        "HIGH",
                        "INSPECTION_FAILED",
                        "质检不合格",
                        batchId,
                        batchName(batch),
                        "质检项「" + record.getInspectionItem() + "」结果为「" + record.getResult() + "」",
                        record.getInspectionDate()));
            } else if ("UNKNOWN".equals(verdict)) {
                String shown = (record.getResult() == null || record.getResult().isBlank()) ? "（空）" : record.getResult();
                medium.add(warning(
                        "MEDIUM",
                        "INSPECTION_INCONCLUSIVE",
                        "质检结果存疑",
                        batchId,
                        batchName(batch),
                        "质检项「" + record.getInspectionItem() + "」结果为「" + shown + "」，无法判定是否合格，需人工复核",
                        record.getInspectionDate()));
            }
        }

        // 规则二/三需要"哪些批次已有质检/物流"的集合，做一次性差集计算，避免逐批次 N+1 查询。
        Set<String> batchesWithInspection = new HashSet<>(inspectionRecordRepository.findDistinctBatchIds());
        Set<String> batchesWithLogistics = new HashSet<>(logisticsRecordRepository.findDistinctBatchIds());

        for (Batch batch : batchRepository.findAllWithProduct()) {
            // 规则二：批次缺少质检记录（MEDIUM）。
            if (!batchesWithInspection.contains(batch.getId())) {
                medium.add(warning(
                        "MEDIUM",
                        "MISSING_INSPECTION",
                        "缺少质检记录",
                        batch.getId(),
                        batchName(batch),
                        "该批次尚无任何质检记录，无法证明质量合规",
                        batch.getCreateTime()));
            }
            // 规则三：批次缺少物流轨迹（LOW）。
            if (!batchesWithLogistics.contains(batch.getId())) {
                low.add(warning(
                        "LOW",
                        "MISSING_LOGISTICS",
                        "缺少物流轨迹",
                        batch.getId(),
                        batchName(batch),
                        "该批次尚无任何物流记录，流向无法追溯",
                        batch.getCreateTime()));
            }
        }

        List<Map<String, Object>> all = new ArrayList<>();
        all.addAll(high);
        all.addAll(medium);
        all.addAll(low);
        return all;
    }

    /**
     * 汇总各级别预警数量，供预警中心顶部卡片展示。
     */
    public Map<String, Object> summary() {
        List<Map<String, Object>> all = scan();
        long highCount = all.stream().filter(w -> "HIGH".equals(w.get("level"))).count();
        long mediumCount = all.stream().filter(w -> "MEDIUM".equals(w.get("level"))).count();
        long lowCount = all.stream().filter(w -> "LOW".equals(w.get("level"))).count();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", all.size());
        summary.put("high", highCount);
        summary.put("medium", mediumCount);
        summary.put("low", lowCount);
        return summary;
    }

    /**
     * 三态判定质检结果：
     *   FAIL    命中不合格关键字；
     *   PASS    未命中不合格、但命中合格白名单关键字；
     *   UNKNOWN 其余情况（空、或既非合格也非不合格的无效/非常规结果，如"随意""测试"）。
     * 先判 FAIL 再判 PASS，保证"不合格""不达标"等不会因子串"合格""达标"被误判为通过。
     */
    private String classifyResult(String result) {
        if (result == null || result.isBlank()) {
            return "UNKNOWN";
        }
        String text = result.trim();
        // 否定式合格（未检出超标/无异常 等）优先判为通过，避免被 FAIL 子串误伤。
        if (PASS_NEGATIONS.stream().anyMatch(text::contains)) {
            return "PASS";
        }
        if (FAIL_KEYWORDS.stream().anyMatch(text::contains)) {
            return "FAIL";
        }
        if (PASS_KEYWORDS.stream().anyMatch(text::contains)) {
            return "PASS";
        }
        return "UNKNOWN";
    }

    /**
     * 组装单条预警的展示结构。
     */
    private Map<String, Object> warning(String level, String type, String typeLabel,
                                        String targetId, String targetName, String message, String time) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("level", level);
        row.put("type", type);
        row.put("typeLabel", typeLabel);
        row.put("targetType", "BATCH");
        row.put("targetId", targetId);
        row.put("targetName", targetName);
        row.put("message", message);
        row.put("time", time);
        return row;
    }

    /**
     * 批次的可读名称：批次号（产品名），缺失字段时做兜底。
     */
    private String batchName(Batch batch) {
        if (batch == null) {
            return "未知批次";
        }
        String productName = batch.getProduct() != null ? batch.getProduct().getName() : null;
        return productName != null ? batch.getBatchNo() + "（" + productName + "）" : batch.getBatchNo();
    }
}
