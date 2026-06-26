package com.example.agriculturetrace.controller;

import com.example.agriculturetrace.entity.BlockchainLog;
import com.example.agriculturetrace.entity.Batch;
import com.example.agriculturetrace.entity.Product;
import com.example.agriculturetrace.repository.BatchRepository;
import com.example.agriculturetrace.repository.BlockchainLogRepository;
import com.example.agriculturetrace.repository.ProductRepository;
import com.example.agriculturetrace.service.BatchService;
import com.example.agriculturetrace.service.BlockchainAnchorService;
import com.example.agriculturetrace.service.ProductService;
import com.example.agriculturetrace.util.HashUtil;
import com.example.agriculturetrace.util.Result;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 审计日志链接口。
 *
 * blockchain_log 表用 previous_hash -> data_hash 的方式模拟区块链链式结构：
 * 每条日志的哈希都包含上一条日志哈希，因此插入、删除或篡改中间日志都会被发现。
 */
@RestController
@RequestMapping("/api/blockchain")
public class BlockchainLogController {

    private final BlockchainLogRepository logRepository;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final ProductService productService;
    private final BatchService batchService;
    private final BlockchainAnchorService anchorService;

    public BlockchainLogController(BlockchainLogRepository logRepository,
                                   ProductRepository productRepository,
                                   BatchRepository batchRepository,
                                   ProductService productService,
                                   BatchService batchService,
                                   BlockchainAnchorService anchorService) {
        this.logRepository = logRepository;
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.productService = productService;
        this.batchService = batchService;
        this.anchorService = anchorService;
    }

    /**
     * 分页返回审计日志列表。
     * 日志按单调递增的 logId 升序展示，和链式哈希校验的遍历顺序保持一致。
     */
    @GetMapping("/logs")
    public Result<?> logs(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String actionType,
                          @RequestParam(required = false) String targetType,
                          @RequestParam(required = false) String operator,
                          @RequestParam(required = false) String targetId,
                          @RequestParam(required = false) String startTime,
                          @RequestParam(required = false) String endTime) {
        // 按 id（logId，UTC 毫秒前缀、单调、与时区无关）升序，保证展示顺序与链条验证顺序一致。
        // 不能按 timestamp 排序：timestamp 是本地墙钟字符串，跨时区会乱序导致链条误判断裂。
        PageRequest request = PageRequest.of(
                Math.max(page - 1, 0),
                Math.max(pageSize, 1),
                Sort.by(Sort.Direction.ASC, "id")
        );
        Page<BlockchainLog> logs = logRepository.findAll(
                buildLogSpecification(actionType, targetType, operator, targetId, startTime, endTime),
                request
        );
        return Result.success(Map.of(
                "records", logs.getContent().stream().map(this::toLogRow).toList(),
                "total", logs.getTotalElements(),
                "page", logs.getNumber() + 1,
                "pageSize", logs.getSize()
        ));
    }

    /**
     * 审计日志筛选条件。
     * timestamp 使用 yyyy-MM-dd HH:mm:ss 字符串，按字典序比较即可保持时间顺序。
     */
    private Specification<BlockchainLog> buildLogSpecification(String actionType,
                                                               String targetType,
                                                               String operator,
                                                               String targetId,
                                                               String startTime,
                                                               String endTime) {
        return (root, query, builder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            addEqualsPredicate(predicates, builder, root.get("actionType"), actionType);
            addEqualsPredicate(predicates, builder, root.get("targetType"), targetType);
            addLikePredicate(predicates, builder, root.get("operator"), operator);
            addLikePredicate(predicates, builder, root.get("targetId"), targetId);
            if (hasText(startTime)) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("timestamp"), startTime.trim()));
            }
            if (hasText(endTime)) {
                predicates.add(builder.lessThanOrEqualTo(root.get("timestamp"), endTime.trim()));
            }
            return builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private void addEqualsPredicate(List<jakarta.persistence.criteria.Predicate> predicates,
                                    jakarta.persistence.criteria.CriteriaBuilder builder,
                                    jakarta.persistence.criteria.Path<String> path,
                                    String value) {
        if (hasText(value)) {
            predicates.add(builder.equal(path, value.trim()));
        }
    }

    private void addLikePredicate(List<jakarta.persistence.criteria.Predicate> predicates,
                                  jakarta.persistence.criteria.CriteriaBuilder builder,
                                  jakarta.persistence.criteria.Path<String> path,
                                  String value) {
        if (hasText(value)) {
            predicates.add(builder.like(path, "%" + value.trim() + "%"));
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 校验审计日志链、链尾锚点和当前业务数据指纹。
     * 返回 valid/logChainValid/tailValid/dataIntegrityValid，前端据此展示不同异常类型。
     */
    @GetMapping("/logs/verify")
    public Result<?> verify() {
        List<BlockchainLog> logs = logRepository.findAllOrderedByIdAsc();
        // 从创世值开始校验：第一条日志没有上一条日志，所以它的 previous_hash 约定为 "0"。
        // 每校验通过一条日志，就把 previousHash 推进为当前日志的 data_hash，供下一条日志比对。
        String previousHash = "0";
        for (int i = 0; i < logs.size(); i++) {
            BlockchainLog log = logs.get(i);
            // 按写入日志时完全相同的字段顺序重建原始内容，再 SHA-256 得到 calcHash。
            // 只要日志的动作、目标、操作人、时间、上一哈希或变更后数据被改过，calcHash 都会变化。
            String content = log.getActionType() + log.getTargetId() + log.getOperator()
                    + log.getTimestamp() + log.getPreviousHash()
                    + (log.getDataAfter() != null ? log.getDataAfter() : "");
            String calcHash = HashUtil.sha256(content);

            // 判断当前日志自身是否可信：
            // calcHash 是根据当前数据库内容即时算出的哈希，dataHash 是写入日志时保存的哈希。
            // 两者不一致，说明这一条日志内容可能被直接改库篡改。
            boolean hashValid = calcHash.equals(log.getDataHash());

            // 判断当前日志能否接上上一条日志：
            // previousHash 保存的是“上一条已校验日志的 data_hash”；
            // 当前日志的 previous_hash 必须等于它，否则说明中间有日志被删除、插入或顺序被破坏。
            boolean previousValid = previousHash.equals(log.getPreviousHash());

            // 两个条件任意一个失败，都说明链条在当前日志位置出现异常。
            if (!hashValid || !previousValid) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("valid", false);
                result.put("logChainValid", false);
                result.put("tailValid", null);
                result.put("expectedTotal", null);
                result.put("dataIntegrityValid", null);
                result.put("total", logs.size());
                // i 是 Java List 的下标，从 0 开始；页面展示给用户时从第 1 条开始数，所以返回 i + 1。
                result.put("brokenIndex", i + 1);
                result.put("brokenLogId", log.getId());
                result.put("message", hashValid ? "上一哈希不连续，日志链已断裂" : "日志自身哈希不一致，可能被篡改");
                return Result.success(result);
            }

            // 当前日志通过校验后，下一条日志应该把当前日志的 data_hash 作为 previous_hash。
            previousHash = log.getDataHash();
        }

        // 链条遍历通过，只能说明“现存日志彼此连续”，但发现不了“从链尾整段删除最新日志”——
        // 删掉链尾后剩下的日志依旧首尾相连。用外部锚点（期望条数 + 期望链尾哈希）做二次校验：
        // 此刻 previousHash 已是最后一条日志的哈希（空表时为创世值 "0"），即当前链尾。
        BlockchainAnchorService.Anchor anchor = anchorService.getAnchor();
        long currentTotal = logs.size();
        String currentTip = previousHash;
        boolean tailValid = true;
        String tailMessage = null;
        Long expectedTotal = null;
        if (anchor != null) {
            expectedTotal = anchor.logCount();
            if (currentTotal < anchor.logCount()) {
                tailValid = false;
                tailMessage = "日志总数减少：当前 " + currentTotal + " 条，锚点期望 " + anchor.logCount()
                        + " 条，疑似从链尾删除了最新日志";
            } else if (!currentTip.equals(anchor.tipHash())) {
                tailValid = false;
                tailMessage = "链尾哈希与锚点不一致，最新日志可能被删除或替换";
            }
        }

        // 日志链完整也不等于业务数据没被改库，所以还要联动校验产品和批次指纹。
        Map<String, Object> dataIntegrity = verifyBusinessData();
        boolean dataIntegrityValid = Boolean.TRUE.equals(dataIntegrity.get("valid"));
        boolean valid = tailValid && dataIntegrityValid;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid", valid);
        result.put("logChainValid", true);
        result.put("tailValid", tailValid);
        result.put("expectedTotal", expectedTotal);
        result.put("dataIntegrityValid", dataIntegrityValid);
        result.put("total", logs.size());
        result.put("invalidCount", dataIntegrity.get("invalidCount"));
        result.put("invalidItems", dataIntegrity.get("invalidItems"));
        if (logs.isEmpty()) {
            result.put("message", "暂无审计日志");
        } else if (!tailValid) {
            // 尾部截断是最严重的篡改信号，优先作为结论展示。
            result.put("message", tailMessage);
        } else if (dataIntegrityValid) {
            result.put("message", "日志链完整，链尾锚点一致，业务数据指纹一致");
        } else {
            result.put("message", "日志链完整，但当前业务数据有 " + dataIntegrity.get("invalidCount") + " 项指纹异常");
        }
        return Result.success(result);
    }

    /**
     * 重新计算产品和批次当前业务数据指纹，发现绕过系统直接改库造成的 dataHash 不一致。
     */
    private Map<String, Object> verifyBusinessData() {
        List<Map<String, Object>> invalidItems = new java.util.ArrayList<>();

        // 产品和批次分别调用各自 Service 的同一套哈希规则，避免控制器复制算法。
        for (Product product : productRepository.findAll()) {
            String currentHash = productService.computeProductHash(product);
            if (!currentHash.equals(product.getDataHash())) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("targetType", "PRODUCT");
                item.put("targetId", product.getId());
                item.put("name", product.getName());
                item.put("storedHash", product.getDataHash());
                item.put("currentHash", currentHash);
                invalidItems.add(item);
            }
        }

        for (Batch batch : batchRepository.findAllWithProduct()) {
            String currentHash = batchService.computeBatchHash(batch);
            if (!currentHash.equals(batch.getDataHash())) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("targetType", "BATCH");
                item.put("targetId", batch.getId());
                item.put("name", batch.getBatchNo());
                item.put("storedHash", batch.getDataHash());
                item.put("currentHash", currentHash);
                invalidItems.add(item);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid", invalidItems.isEmpty());
        result.put("invalidCount", invalidItems.size());
        result.put("invalidItems", invalidItems);
        return result;
    }

    /**
     * 将 BlockchainLog 实体转换成前端展示行。
     * 采用显式字段映射，避免 JPA 实体结构变化影响接口返回格式。
     */
    private Map<String, Object> toLogRow(BlockchainLog log) {
        // 明确输出字段而不是直接返回实体，避免后续实体字段调整影响前端契约。
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", log.getId());
        row.put("actionType", log.getActionType());
        row.put("targetType", log.getTargetType());
        row.put("targetId", log.getTargetId());
        row.put("operator", log.getOperator());
        row.put("dataBefore", log.getDataBefore());
        row.put("dataAfter", log.getDataAfter());
        row.put("dataHash", log.getDataHash());
        row.put("previousHash", log.getPreviousHash());
        row.put("timestamp", log.getTimestamp());
        return row;
    }
}
