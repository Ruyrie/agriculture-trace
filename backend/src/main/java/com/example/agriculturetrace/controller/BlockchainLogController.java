package com.example.agriculturetrace.controller;

import com.example.agriculturetrace.entity.BlockchainLog;
import com.example.agriculturetrace.entity.Batch;
import com.example.agriculturetrace.entity.Product;
import com.example.agriculturetrace.repository.BatchRepository;
import com.example.agriculturetrace.repository.BlockchainLogRepository;
import com.example.agriculturetrace.repository.ProductRepository;
import com.example.agriculturetrace.service.BatchService;
import com.example.agriculturetrace.service.ProductService;
import com.example.agriculturetrace.util.HashUtil;
import com.example.agriculturetrace.util.Result;
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

    public BlockchainLogController(BlockchainLogRepository logRepository,
                                   ProductRepository productRepository,
                                   BatchRepository batchRepository,
                                   ProductService productService,
                                   BatchService batchService) {
        this.logRepository = logRepository;
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.productService = productService;
        this.batchService = batchService;
    }

    @GetMapping("/logs")
    public Result<?> logs(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int pageSize) {
        // 按 id（logId，UTC 毫秒前缀、单调、与时区无关）升序，保证展示顺序与链条验证顺序一致。
        // 不能按 timestamp 排序：timestamp 是本地墙钟字符串，跨时区会乱序导致链条误判断裂。
        PageRequest request = PageRequest.of(
                Math.max(page - 1, 0),
                Math.max(pageSize, 1),
                Sort.by(Sort.Direction.ASC, "id")
        );
        Page<BlockchainLog> logs = logRepository.findAll(request);
        return Result.success(Map.of(
                "records", logs.getContent().stream().map(this::toLogRow).toList(),
                "total", logs.getTotalElements(),
                "page", logs.getNumber() + 1,
                "pageSize", logs.getSize()
        ));
    }

    @GetMapping("/logs/verify")
    public Result<?> verify() {
        List<BlockchainLog> logs = logRepository.findAllOrderedByIdAsc();
        String previousHash = "0";
        for (int i = 0; i < logs.size(); i++) {
            BlockchainLog log = logs.get(i);
            // 重建写入日志时使用的原始字符串，再 SHA-256 得到 calcHash。
            // 如果 calcHash != dataHash，说明日志内容被改；如果 previousHash 不连续，说明链断了。
            String content = log.getActionType() + log.getTargetId() + log.getOperator()
                    + log.getTimestamp() + log.getPreviousHash()
                    + (log.getDataAfter() != null ? log.getDataAfter() : "");
            String calcHash = HashUtil.sha256(content);
            boolean hashValid = calcHash.equals(log.getDataHash());
            boolean previousValid = previousHash.equals(log.getPreviousHash());
            if (!hashValid || !previousValid) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("valid", false);
                result.put("logChainValid", false);
                result.put("dataIntegrityValid", null);
                result.put("total", logs.size());
                result.put("brokenIndex", i + 1);
                result.put("brokenLogId", log.getId());
                result.put("message", hashValid ? "上一哈希不连续，日志链已断裂" : "日志自身哈希不一致，可能被篡改");
                return Result.success(result);
            }
            previousHash = log.getDataHash();
        }

        // 日志链完整不等于业务数据没被改库，所以链条通过后还要联动校验产品和批次指纹。
        Map<String, Object> dataIntegrity = verifyBusinessData();
        boolean dataIntegrityValid = Boolean.TRUE.equals(dataIntegrity.get("valid"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid", dataIntegrityValid);
        result.put("logChainValid", true);
        result.put("dataIntegrityValid", dataIntegrityValid);
        result.put("total", logs.size());
        result.put("invalidCount", dataIntegrity.get("invalidCount"));
        result.put("invalidItems", dataIntegrity.get("invalidItems"));
        if (logs.isEmpty()) {
            result.put("message", "暂无审计日志");
        } else if (dataIntegrityValid) {
            result.put("message", "日志链完整，业务数据指纹一致");
        } else {
            result.put("message", "日志链完整，但当前业务数据有 " + dataIntegrity.get("invalidCount") + " 项指纹异常");
        }
        return Result.success(result);
    }

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
