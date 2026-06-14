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

    @GetMapping({"/fingerprints", "/products"})
    public Result<?> fingerprints() {
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

    @GetMapping({"/verify/{id}", "/product/{id}/verify"})
    public Result<?> verifyProduct(@PathVariable String id) {
        return Result.success(productService.verifyProductHash(id));
    }

    @GetMapping("/products/verify")
    public Result<?> verifyProducts() {
        return Result.success(productService.verifyAllProductHashes());
    }

    @GetMapping("/batch/{id}/verify")
    public Result<?> verifyBatch(@PathVariable String id) {
        return Result.success(batchService.verifyBatchHash(id));
    }

    @GetMapping("/batches/verify")
    public Result<?> verifyBatches() {
        return Result.success(batchService.verifyAllBatchHashes());
    }

    private Map<String, Object> toIntegrityRow(Product product) {
        String currentHash = productService.computeProductHash(product);
        Map<String, Object> row = new LinkedHashMap<>(productService.toAuditRow(product));
        row.put("storedHash", product.getDataHash());
        row.put("currentHash", currentHash);
        row.put("valid", currentHash.equals(product.getDataHash()));
        return row;
    }

    private String calculateRootHash(List<Map<String, Object>> rows) {
        String joined = rows.stream()
                .map(row -> String.valueOf(row.get("currentHash")))
                .reduce("", (left, right) -> left.isEmpty() ? right : left + "|" + right);
        return HashUtil.sha256(joined);
    }
}
