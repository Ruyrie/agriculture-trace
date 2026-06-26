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
        // currentHash：对当前数据库中该产品的字段即时重新计算的 SHA-256 指纹。
        String currentHash = productService.computeProductHash(product);
        // toAuditRow 已包含 id/name/category/origin/price/createTime 等展示字段；
        // 用 LinkedHashMap 复制，保证字段顺序稳定，方便前端展示和调试。
        Map<String, Object> row = new LinkedHashMap<>(productService.toAuditRow(product));
        // storedHash：数据库 data_hash 列存的历史值，代表”系统上次修改时算出的指纹”。
        row.put("storedHash", product.getDataHash());
        // currentHash：基于当前字段即时计算的指纹，用于与 storedHash 比对。
        row.put("currentHash", currentHash);
        // valid：两者一致则 true（正常），不一致则 false（字段被直接改库篡改）。
        row.put("valid", currentHash.equals(product.getDataHash()));
        return row;
    }

    /**
     * 计算简化版全局根哈希：把所有行的 currentHash 按固定顺序连接后再 SHA-256。
     */
    private String calculateRootHash(List<Map<String, Object>> rows) {
        // 将所有产品的当前指纹按 “|” 分隔串联成一个字符串，再 SHA-256 得到全局摘要。
        // 这是简化版 Merkle Root：不是标准二叉树结构，但任意一条产品数据变化都会使根哈希变化。
        // reduce 的逻辑：第一条不加前缀，后续用 “|” 分隔拼接。
        String joined = rows.stream()
                .map(row -> String.valueOf(row.get("currentHash")))
                .reduce("", (left, right) -> left.isEmpty() ? right : left + "|" + right);
        // 对拼接结果再做 SHA-256，得到固定长度的全局摘要，便于外部存证和比对。
        return HashUtil.sha256(joined);
    }
}
