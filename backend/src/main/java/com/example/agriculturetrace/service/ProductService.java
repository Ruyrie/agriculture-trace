package com.example.agriculturetrace.service;

import com.example.agriculturetrace.entity.Product;
import com.example.agriculturetrace.entity.BlockchainLog;
import com.example.agriculturetrace.repository.BlockchainLogRepository;
import com.example.agriculturetrace.repository.ProductRepository;
import com.example.agriculturetrace.util.HashUtil;
import com.example.agriculturetrace.util.Ids;
import com.example.agriculturetrace.util.TimeUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 产品管理服务。
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final BlockchainLogRepository logRepository;
    private final ObjectMapper objectMapper;

    public ProductService(ProductRepository productRepository,
                          BlockchainLogRepository logRepository,
                          ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.logRepository = logRepository;
        this.objectMapper = objectMapper;
    }

    public Page<Product> list(String keyword, int page, int pageSize) {
        PageRequest request = PageRequest.of(Math.max(page - 1, 0), pageSize);
        if (keyword == null || keyword.isBlank()) {
            return productRepository.findAll(request);
        }
        return productRepository.findByNameContainingIgnoreCase(keyword, request);
    }

    public Product get(String id) {
        return productRepository.findById(id).orElseThrow();
    }

    @Transactional
    public Product create(Product product) {
        product.setId(Ids.uuid32());
        product.setCreateTime(TimeUtils.nowText());
        product.setDataHash(computeProductHash(product));
        Product saved = productRepository.save(product);
        recordLog("CREATE", saved.getId(), null, toAuditRow(saved));
        return saved;
    }

    @Transactional
    public Product update(Product product) {
        Product existing = get(product.getId());
        Map<String, Object> before = toAuditRow(existing);
        existing.setName(product.getName());
        existing.setCategory(product.getCategory());
        existing.setOrigin(product.getOrigin());
        existing.setPrice(product.getPrice());
        existing.setDataHash(computeProductHash(existing));
        Product saved = productRepository.save(existing);
        recordLog("UPDATE", saved.getId(), before, toAuditRow(saved));
        return saved;
    }

    @Transactional
    public void delete(String id) {
        Product existing = get(id);
        Map<String, Object> before = toAuditRow(existing);
        productRepository.deleteById(id);
        recordLog("DELETE", id, before, null);
    }

    public String computeProductHash(Product product) {
        return HashUtil.sha256(String.join("|",
                nullToEmpty(product.getId()),
                nullToEmpty(product.getName()),
                nullToEmpty(product.getCategory()),
                nullToEmpty(product.getOrigin()),
                normalizePrice(product.getPrice()),
                nullToEmpty(product.getCreateTime())
        ));
    }

    public Map<String, Object> verifyProductHash(String id) {
        Product product = get(id);
        String currentHash = computeProductHash(product);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", product.getId());
        result.put("name", product.getName());
        result.put("storedHash", product.getDataHash());
        result.put("currentHash", currentHash);
        result.put("valid", currentHash.equals(product.getDataHash()));
        return result;
    }

    public Map<String, Object> verifyAllProductHashes() {
        List<Map<String, Object>> invalidItems = new ArrayList<>();
        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            String currentHash = computeProductHash(product);
            if (!currentHash.equals(product.getDataHash())) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", product.getId());
                item.put("name", product.getName());
                item.put("category", product.getCategory());
                item.put("origin", product.getOrigin());
                item.put("storedHash", product.getDataHash());
                item.put("currentHash", currentHash);
                invalidItems.add(item);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid", invalidItems.isEmpty());
        result.put("total", products.size());
        result.put("invalidCount", invalidItems.size());
        result.put("invalidItems", invalidItems);
        return result;
    }

    public Map<String, Object> toAuditRow(Product product) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", product.getId());
        row.put("name", product.getName());
        row.put("category", product.getCategory());
        row.put("origin", product.getOrigin());
        row.put("price", normalizePrice(product.getPrice()));
        row.put("createTime", product.getCreateTime());
        row.put("dataHash", product.getDataHash());
        return row;
    }

    private String normalizePrice(BigDecimal price) {
        return price == null ? "" : price.stripTrailingZeros().toPlainString();
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
        log.setId(UUID.randomUUID().toString().replace("-", ""));
        log.setActionType(action);
        log.setTargetType("PRODUCT");
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
