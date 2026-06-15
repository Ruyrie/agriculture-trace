package com.example.agriculturetrace.service;

import com.example.agriculturetrace.entity.Batch;
import com.example.agriculturetrace.entity.BlockchainLog;
import com.example.agriculturetrace.entity.Product;
import com.example.agriculturetrace.repository.BatchRepository;
import com.example.agriculturetrace.repository.BlockchainLogRepository;
import com.example.agriculturetrace.repository.ProductRepository;
import com.example.agriculturetrace.util.HashUtil;
import com.example.agriculturetrace.util.Ids;
import com.example.agriculturetrace.util.TimeUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 批次管理服务。
 *
 * 批次是产品溯源的关键业务单元：生产、质检、物流记录都挂在 batch 上。
 * 因此本服务除了常规 CRUD，还要负责批次号唯一性、批次数据指纹和批次审计日志。
 */
@Service
public class BatchService {

    private final BatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final BlockchainLogRepository logRepository;
    private final ObjectMapper objectMapper;

    public BatchService(BatchRepository batchRepository,
                        ProductRepository productRepository,
                        BlockchainLogRepository logRepository,
                        ObjectMapper objectMapper) {
        this.batchRepository = batchRepository;
        this.productRepository = productRepository;
        this.logRepository = logRepository;
        this.objectMapper = objectMapper;
    }

    public Page<Batch> list(String productId, String productName, String batchNo, int page, int pageSize) {
        PageRequest request = PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by(Sort.Direction.DESC, "productionDate"));
        boolean hasProduct = productId != null && !productId.isBlank();
        boolean hasProductName = !hasProduct && productName != null && !productName.isBlank();
        boolean hasBatchNo = batchNo != null && !batchNo.isBlank();
        // 根据筛选条件选择不同 Repository 方法，让数据库完成过滤和分页。
        // 这样前端无论按产品、批次号还是组合筛选，都能拿到统一的分页结构。
        if (!hasProduct && !hasProductName && !hasBatchNo) {
            return batchRepository.findAll(request);
        }
        if (hasProduct && hasBatchNo) {
            return batchRepository.findByProduct_IdAndBatchNoContaining(productId, batchNo.trim(), request);
        }
        if (hasProduct) {
            return batchRepository.findByProduct_Id(productId, request);
        }
        if (hasProductName && hasBatchNo) {
            return batchRepository.findByProduct_NameIgnoreCaseAndBatchNoContaining(productName.trim(), batchNo.trim(), request);
        }
        if (hasProductName) {
            return batchRepository.findByProduct_NameIgnoreCase(productName.trim(), request);
        }
        return batchRepository.findByBatchNoContaining(batchNo.trim(), request);
    }

    @Transactional
    public Batch create(Batch batch, String productId) {
        // 批次号是面向业务人员和二维码溯源的可读标识，必须全局唯一。
        ensureBatchNoAvailable(batch.getBatchNo(), null);
        Product product = productRepository.findById(productId).orElseThrow();
        batch.setId(Ids.uuid32());
        batch.setProduct(product);
        batch.setCreateTime(TimeUtils.nowText());
        // productId、productionDate、remark 等字段共同构成批次指纹。
        batch.setDataHash(computeBatchHash(batch));
        Batch saved = batchRepository.save(batch);
        recordLog("CREATE", saved.getId(), null, toAuditRow(saved));
        return saved;
    }

    @Transactional
    public Batch update(Batch batch, String productId) {
        Batch existing = batchRepository.findById(batch.getId()).orElseThrow();
        // 更新前快照进入 data_before；更新后快照进入 data_after，便于审计页面对比。
        Map<String, Object> before = toAuditRow(existing);
        ensureBatchNoAvailable(batch.getBatchNo(), existing.getId());
        if (productId != null && !productId.isBlank()) {
            existing.setProduct(productRepository.findById(productId).orElseThrow());
        }
        existing.setBatchNo(batch.getBatchNo());
        existing.setProductionDate(batch.getProductionDate());
        existing.setRemark(batch.getRemark());
        // 修改批次关键字段后立即重算指纹，保证正常业务操作不会制造误报。
        existing.setDataHash(computeBatchHash(existing));
        Batch saved = batchRepository.save(existing);
        recordLog("UPDATE", saved.getId(), before, toAuditRow(saved));
        return saved;
    }

    @Transactional
    public void delete(String id) {
        Batch existing = batchRepository.findDetailById(id).orElseThrow();
        Map<String, Object> before = toAuditRow(existing);
        batchRepository.deleteById(id);
        recordLog("DELETE", id, before, null);
    }

    public Map<String, Object> toRow(Batch batch) {
        return toRow(batch, batch.getProduct().getId());
    }

    public Map<String, Object> toRow(Batch batch, String productId) {
        Product product = productRepository.findById(productId).orElseThrow();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", batch.getId());
        row.put("batchNo", batch.getBatchNo());
        row.put("productId", product.getId());
        row.put("productName", product.getName());
        row.put("productionDate", batch.getProductionDate());
        row.put("remark", batch.getRemark());
        row.put("createTime", batch.getCreateTime());
        row.put("dataHash", batch.getDataHash());
        return row;
    }

    public Map<String, Object> toRowById(String id) {
        Batch batch = batchRepository.findDetailById(id).orElseThrow();
        return toRow(batch);
    }

    private void ensureBatchNoAvailable(String batchNo, String currentId) {
        if (batchNo == null || batchNo.isBlank()) {
            throw new IllegalArgumentException("批次号不能为空");
        }
        // currentId 用来区分“编辑自己保持原批次号”和“占用别人的批次号”。
        batchRepository.findByBatchNo(batchNo.trim())
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("批次号已存在，请更换批次号");
                });
    }

    public String computeBatchHash(Batch batch) {
        // 字段顺序与 schema.sql / BlockchainSchemaInitializer 的 SQL 回填逻辑保持一致。
        // 这里使用 product.id 而不是产品名称，是为了让产品改名不影响已有批次指纹。
        return HashUtil.sha256(String.join("|",
                nullToEmpty(batch.getId()),
                nullToEmpty(batch.getBatchNo()),
                batch.getProduct() == null ? "" : nullToEmpty(batch.getProduct().getId()),
                batch.getProductionDate() == null ? "" : batch.getProductionDate().toString(),
                nullToEmpty(batch.getRemark()),
                nullToEmpty(batch.getCreateTime())
        ));
    }

    public Map<String, Object> verifyBatchHash(String id) {
        Batch batch = batchRepository.findDetailById(id).orElseThrow();
        // findDetailById 会预取 Product，避免计算哈希时访问 LAZY product 触发额外问题。
        String currentHash = computeBatchHash(batch);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", batch.getId());
        result.put("batchNo", batch.getBatchNo());
        result.put("storedHash", batch.getDataHash());
        result.put("currentHash", currentHash);
        result.put("valid", currentHash.equals(batch.getDataHash()));
        return result;
    }

    public Map<String, Object> verifyAllBatchHashes() {
        List<Map<String, Object>> invalidItems = new ArrayList<>();
        List<Batch> batches = batchRepository.findAllWithProduct();
        for (Batch batch : batches) {
            String currentHash = computeBatchHash(batch);
            if (!currentHash.equals(batch.getDataHash())) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", batch.getId());
                item.put("batchNo", batch.getBatchNo());
                item.put("productName", batch.getProduct() == null ? null : batch.getProduct().getName());
                item.put("productionDate", batch.getProductionDate());
                item.put("storedHash", batch.getDataHash());
                item.put("currentHash", currentHash);
                invalidItems.add(item);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid", invalidItems.isEmpty());
        result.put("total", batches.size());
        result.put("invalidCount", invalidItems.size());
        result.put("invalidItems", invalidItems);
        return result;
    }

    public Map<String, Object> toAuditRow(Batch batch) {
        // 审计日志展示需要同时看到 productId 和 productName，
        // 但指纹计算只使用 productId，二者职责不同。
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", batch.getId());
        row.put("batchNo", batch.getBatchNo());
        row.put("productId", batch.getProduct() == null ? null : batch.getProduct().getId());
        row.put("productName", batch.getProduct() == null ? null : batch.getProduct().getName());
        row.put("productionDate", batch.getProductionDate());
        row.put("remark", batch.getRemark());
        row.put("createTime", batch.getCreateTime());
        row.put("dataHash", batch.getDataHash());
        return row;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 将对象转为 JSON 字符串（用于存储操作前后数据）。
     */
    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 记录审计日志（链式哈希）。
     */
    private void recordLog(String action, String targetId, Object before, Object after) {
        BlockchainLog log = new BlockchainLog();
        // 日志主键必须使用时间前缀、单调递增的 ID，确保 (timestamp, id) 排序与写入顺序一致，
        // 否则同一秒内写入的多条日志会因随机 UUID 排序歧义而误判“日志链断裂”。
        log.setId(Ids.logId());
        log.setActionType(action);
        log.setTargetType("BATCH");
        log.setTargetId(targetId);
        log.setOperator(currentOperator());
        log.setDataBefore(toJson(before));
        log.setDataAfter(toJson(after));
        log.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // 获取上一条日志哈希（创世块 previous_hash = "0"）。
        BlockchainLog lastLog = logRepository.findLastLog();
        String previousHash = (lastLog == null) ? "0" : lastLog.getDataHash();
        log.setPreviousHash(previousHash);

        // 计算当前日志哈希：操作类型+目标ID+操作人+时间戳+上一条哈希+变更后数据。
        String content = action + targetId + log.getOperator() + log.getTimestamp() + previousHash
                + (log.getDataAfter() != null ? log.getDataAfter() : "");
        log.setDataHash(HashUtil.sha256(content));
        logRepository.save(log);
    }

    private String currentOperator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return "system";
        }
        return authentication.getName();
    }
}
