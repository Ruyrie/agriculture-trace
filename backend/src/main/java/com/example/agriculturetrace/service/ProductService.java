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
import java.util.Objects;

/**
 * 产品管理服务。
 *
 * 这一层是“产品业务”的核心落点：Controller 只负责接收参数和返回 Result，
 * 真正的分页查询、哈希计算、增删改审计都集中在 Service 中。
 *
 * 之所以在产品表中保存 dataHash，是为了给每条产品记录做一份可重复计算的
 * 数据指纹；后续校验时重新按同样字段计算 SHA-256，如果和存储值不同，就说明
 * 业务字段可能被绕过系统直接修改过。
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final BlockchainLogRepository logRepository;
    private final BlockchainAnchorService anchorService;
    private final ObjectMapper objectMapper;

    public ProductService(ProductRepository productRepository,
                          BlockchainLogRepository logRepository,
                          BlockchainAnchorService anchorService,
                          ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.logRepository = logRepository;
        this.anchorService = anchorService;
        this.objectMapper = objectMapper;
    }

    /**
     * 分页查询产品；keyword 为空时查全部，否则按名称忽略大小写模糊匹配。
     */
    public Page<Product> list(String keyword, int page, int pageSize) {
        PageRequest request = PageRequest.of(Math.max(page - 1, 0), pageSize);
        if (keyword == null || keyword.isBlank()) {
            return productRepository.findAll(request);
        }
        return productRepository.findByNameContainingIgnoreCase(keyword, request);
    }

    /**
     * 按 ID 获取产品实体，找不到则抛出异常交给全局异常处理器转换成 404。
     */
    public Product get(String id) {
        return productRepository.findById(id).orElseThrow();
    }

    /**
     * 生成下一个可读的顺序产品 ID（prod_N）。
     * 取当前最大编号 +1；若库中暂无规范编号，则从 prod_1 起始。
     */
    private String nextProductId() {
        String maxId = productRepository.findMaxProductId();
        long next = 1L;
        if (maxId != null && maxId.startsWith("prod_")) {
            try {
                next = Long.parseLong(maxId.substring("prod_".length())) + 1L;
            } catch (NumberFormatException ignored) {
                // 理论上 SQL 已用正则保证是纯数字后缀，这里仅做兜底，保持 next = 1。
            }
        }
        return "prod_" + next;
    }

    /**
     * 创建产品并写入审计日志。
     * 创建时会先生成业务 ID、创建时间和 dataHash，保证后续完整性校验可复现。
     */
    @Transactional
    public Product create(Product product) {
        // 新增时必须先生成稳定 ID 和创建时间，因为这两个字段也是产品指纹的一部分。
        // 如果先计算哈希再补 ID/时间，保存后的数据就无法通过后续完整性校验。
        // 产品 ID 采用与种子数据一致的可读顺序编号 prod_N，避免界面出现去横线 UUID 这类“乱码”。
        product.setId(nextProductId());
        product.setCreateTime(TimeUtils.nowText());
        product.setDataHash(computeProductHash(product));
        Product saved = productRepository.save(product);
        // 业务数据保存成功后写入审计日志，形成“业务表指纹 + 操作日志链”的双层校验。
        recordLog("CREATE", saved.getId(), null, toAuditRow(saved));
        return saved;
    }

    /**
     * 更新产品基础字段并在发生真实变化时重算 dataHash。
     * 无变化时直接返回原实体，避免产生没有业务意义的审计日志。
     */
    @Transactional
    public Product update(Product product) {
        Product existing = get(product.getId());
        if (!hasProductChanges(existing, product)) {
            return existing;
        }
        // 先保存修改前快照，审计日志详情页需要展示 data_before/data_after。
        Map<String, Object> before = toAuditRow(existing);
        existing.setName(product.getName());
        existing.setCategory(product.getCategory());
        existing.setOrigin(product.getOrigin());
        existing.setPrice(product.getPrice());
        existing.setImageUrls(product.getImageUrls());
        // 只要参与指纹的字段发生变化，就必须同步重算 dataHash。
        existing.setDataHash(computeProductHash(existing));
        Product saved = productRepository.save(existing);
        recordLog("UPDATE", saved.getId(), before, toAuditRow(saved));
        return saved;
    }

    /**
     * 删除产品并把删除前快照写入审计日志。
     */
    @Transactional
    public void delete(String id) {
        Product existing = get(id);
        Map<String, Object> before = toAuditRow(existing);
        productRepository.deleteById(id);
        recordLog("DELETE", id, before, null);
    }

    /**
     * 按固定字段顺序计算产品业务数据指纹。
     * 任一参与字段被绕过系统修改，重新计算出的哈希都会和 dataHash 不一致。
     */
    public String computeProductHash(Product product) {
        // 字段顺序必须保持稳定，并且要和 schema.sql、BlockchainSchemaInitializer 中的
        // SQL 回填规则一致；否则同一条数据在 Java 和 SQL 中会得到不同哈希。
        return HashUtil.sha256(String.join("|",
                nullToEmpty(product.getId()),
                nullToEmpty(product.getName()),
                nullToEmpty(product.getCategory()),
                nullToEmpty(product.getOrigin()),
                normalizePrice(product.getPrice()),
                nullToEmpty(product.getCreateTime())
        ));
    }

    /**
     * 校验单个产品指纹，并返回存储哈希、当前哈希和是否一致。
     */
    public Map<String, Object> verifyProductHash(String id) {
        Product product = get(id);
        // 校验不读取历史日志，而是直接对当前业务表记录重算指纹，
        // 因此能发现“直接改数据库但没有更新 data_hash”的篡改场景。
        String currentHash = computeProductHash(product);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", product.getId());
        result.put("name", product.getName());
        result.put("storedHash", product.getDataHash());
        result.put("currentHash", currentHash);
        result.put("valid", currentHash.equals(product.getDataHash()));
        return result;
    }

    /**
     * 批量校验所有产品指纹，只收集异常产品，减少前端弹窗展示压力。
     */
    public Map<String, Object> verifyAllProductHashes() {
        List<Map<String, Object>> invalidItems = new ArrayList<>();
        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            String currentHash = computeProductHash(product);
            if (!currentHash.equals(product.getDataHash())) {
                // 只返回异常项，前端可以把 invalidItems 做成弹窗明细，避免全量数据过多。
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

    /**
     * 生成产品审计快照，用于审计日志 data_before/data_after 和完整性展示。
     */
    public Map<String, Object> toAuditRow(Product product) {
        // 审计快照使用 LinkedHashMap 固定 JSON 字段顺序，减少哈希和展示时的不确定性。
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", product.getId());
        row.put("name", product.getName());
        row.put("category", product.getCategory());
        row.put("origin", product.getOrigin());
        row.put("price", normalizePrice(product.getPrice()));
        row.put("createTime", product.getCreateTime());
        row.put("dataHash", product.getDataHash());
        row.put("imageUrls", product.getImageUrls());
        return row;
    }

    /**
     * 标准化价格文本，避免 12.50 和 12.5 这类等价数值产生不同哈希。
     */
    private String normalizePrice(BigDecimal price) {
        // 价格统一去掉末尾 0，保证 12.50 和 12.5 在指纹层面表示为同一个业务值。
        return price == null ? "" : price.stripTrailingZeros().toPlainString();
    }

    /**
     * 将 null 字符串统一转换为空字符串，保证哈希拼接逻辑稳定。
     */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 判断产品关键字段是否发生变化，避免无意义更新和空审计日志。
     */
    private boolean hasProductChanges(Product existing, Product incoming) {
        return !Objects.equals(nullToEmpty(existing.getName()), nullToEmpty(incoming.getName()))
                || !Objects.equals(nullToEmpty(existing.getCategory()), nullToEmpty(incoming.getCategory()))
                || !Objects.equals(nullToEmpty(existing.getOrigin()), nullToEmpty(incoming.getOrigin()))
                || !Objects.equals(normalizePrice(existing.getPrice()), normalizePrice(incoming.getPrice()))
                || !Objects.equals(nullToEmpty(existing.getImageUrls()), nullToEmpty(incoming.getImageUrls()));
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
     * 每条日志都包含上一条日志哈希，形成 previous_hash -> data_hash 的连续链。
     */
    private void recordLog(String action, String targetId, Object before, Object after) {
        BlockchainLog log = new BlockchainLog();
        // 日志主键必须使用时间前缀、单调递增的 ID，确保 (timestamp, id) 排序与写入顺序一致，
        // 否则同一秒内写入的多条日志会因随机 UUID 排序歧义而误判“日志链断裂”。
        log.setId(Ids.logId());
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

        // 合法写入后推进锚点：count() 会触发 JPA flush，因此能统计到刚保存的这条；
        // 新链尾即本条日志哈希。事后绕过系统的删除不会走到这里，于是会被验证发现。
        anchorService.refresh(logRepository.count(), log.getDataHash());
    }

    /**
     * 获取当前操作人；没有登录态或匿名访问时使用 system 作为审计操作者。
     */
    private String currentOperator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return "system";
        }
        return authentication.getName();
    }
}
