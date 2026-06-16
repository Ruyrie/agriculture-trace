package com.example.agriculturetrace.controller;

import com.example.agriculturetrace.entity.Product;
import com.example.agriculturetrace.repository.ProductRepository;
import com.example.agriculturetrace.service.BatchService;
import com.example.agriculturetrace.service.ProductService;
import com.example.agriculturetrace.util.HashUtil;
import com.example.agriculturetrace.util.Result;
import com.example.agriculturetrace.util.TimeUtils;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据指纹和根哈希接口。
 *
 * 这里实现的是“业务数据完整性”视角：对 product / batch 的当前字段重新计算哈希，
 * 再与表里保存的 data_hash 比较。它不同于审计日志链校验，后者关注日志是否连续。
 */
@RestController
@RequestMapping("/api/integrity")
public class IntegrityController {

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final BatchService batchService;

    public IntegrityController(ProductRepository productRepository,
                               ProductService productService,
                               BatchService batchService) {
        this.productRepository = productRepository;
        this.productService = productService;
        this.batchService = batchService;
    }

    /**
     * 输出产品数据指纹列表，并计算当前产品集合的全局 rootHash。
     * 前端“数据指纹”页面通过它展示 storedHash/currentHash 是否一致。
     */
    @GetMapping({"/fingerprints", "/products"})
    public Result<?> fingerprints() {
        // 固定按 id 升序输出，保证同一份产品集合计算出的 rootHash 稳定可复现。
        List<Map<String, Object>> rows = productRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(this::toIntegrityRow)
                .toList();
        return Result.success(Map.of(
                "records", rows,
                "total", rows.size(),
                "rootHash", calculateRootHash(rows),
                "generatedAt", TimeUtils.nowText()
        ));
    }

    /**
     * 只返回产品集合根哈希和生成时间，用于需要轻量校验全局摘要的场景。
     */
    @GetMapping("/root-hash")
    public Result<?> rootHash() {
        List<Map<String, Object>> rows = productRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(this::toIntegrityRow)
                .toList();
        return Result.success(Map.of(
                "rootHash", calculateRootHash(rows),
                "total", rows.size(),
                "generatedAt", TimeUtils.nowText()
        ));
    }

    /**
     * 校验单个产品当前字段重新计算出的哈希是否等于数据库保存的 dataHash。
     */
    @GetMapping({"/verify/{id}", "/product/{id}/verify"})
    public Result<?> verifyProduct(@PathVariable String id) {
        return Result.success(productService.verifyProductHash(id));
    }

    /**
     * 批量校验所有产品指纹，只把异常项放进 invalidItems 返回。
     */
    @GetMapping("/products/verify")
    public Result<?> verifyProducts() {
        return Result.success(productService.verifyAllProductHashes());
    }

    /**
     * 校验单个批次的 dataHash，用于批次管理页“验证”按钮。
     */
    @GetMapping("/batch/{id}/verify")
    public Result<?> verifyBatch(@PathVariable String id) {
        return Result.success(batchService.verifyBatchHash(id));
    }

    /**
     * 批量校验所有批次指纹，返回总数、异常数和异常明细。
     */
    @GetMapping("/batches/verify")
    public Result<?> verifyBatches() {
        return Result.success(batchService.verifyAllBatchHashes());
    }

    /**
     * 将 Product 转为数据指纹展示行，并附带即时计算的 currentHash。
     */
    private Map<String, Object> toIntegrityRow(Product product) {
        // storedHash 是数据库保存值，currentHash 是当前字段即时计算值；
        // 前端用 valid 字段直接渲染“一致/异常”。
        String currentHash = productService.computeProductHash(product);
        Map<String, Object> row = new LinkedHashMap<>(productService.toAuditRow(product));
        row.put("storedHash", product.getDataHash());
        row.put("currentHash", currentHash);
        row.put("valid", currentHash.equals(product.getDataHash()));
        return row;
    }

    /**
     * 计算简化版全局根哈希：把所有行的 currentHash 按固定顺序连接后再 SHA-256。
     */
    private String calculateRootHash(List<Map<String, Object>> rows) {
        // 简化版根哈希：将每条产品当前指纹按固定顺序串联后再 SHA-256。
        // 它不是完整 Merkle Tree，但能模拟“全局数据摘要”的校验效果。
        String joined = rows.stream()
                .map(row -> String.valueOf(row.get("currentHash")))
                .reduce("", (left, right) -> left.isEmpty() ? right : left + "|" + right);
        return HashUtil.sha256(joined);
    }
}
