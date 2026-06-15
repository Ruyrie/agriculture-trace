package com.example.agriculturetrace.controller;

import com.example.agriculturetrace.entity.Batch;
import com.example.agriculturetrace.entity.Product;
import com.example.agriculturetrace.service.BatchService;
import com.example.agriculturetrace.service.ProductService;
import com.example.agriculturetrace.service.TraceDataService;
import com.example.agriculturetrace.util.Result;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 产品管理接口。
 */
@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;
    private final BatchService batchService;
    private final TraceDataService traceDataService;

    public ProductController(ProductService productService,
                             BatchService batchService,
                             TraceDataService traceDataService) {
        this.productService = productService;
        this.batchService = batchService;
        this.traceDataService = traceDataService;
    }

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String keyword) {
        Page<Product> products = productService.list(keyword, page, pageSize);
        return Result.success(Map.of(
                "records", products.getContent(),
                "total", products.getTotalElements(),
                "page", products.getNumber() + 1,
                "pageSize", products.getSize()
        ));
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable String id) {
        return Result.success(productService.get(id));
    }

    @PostMapping
    public Result<?> create(@RequestBody Product product) {
        return Result.success(productService.create(product));
    }

    @PutMapping
    public Result<?> update(@RequestBody Product product) {
        return Result.success(productService.update(product));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable String id) {
        productService.delete(id);
        return Result.success(null);
    }

    @PostMapping("/create-with-trace")
    @SuppressWarnings("unchecked")
    @Transactional
    public Result<?> createWithTrace(@RequestBody Map<String, Object> body) {
        // 复合新增接口：前端弹窗一次提交产品、批次、生产/质检/物流记录。
        // 放在同一个事务里，是为了保证任一环节失败时不会出现“产品有了但溯源不完整”的半成品。
        Product product = productService.create(mapProduct((Map<String, Object>) body.get("product")));
        Map<String, Object> batchMap = (Map<String, Object>) body.get("batch");
        if (batchMap != null && batchMap.get("batchNo") != null) {
            // 批次与产品建立关联后，三类溯源记录都以 batch_id 作为外键保存。
            Batch batch = new Batch();
            batch.setBatchNo((String) batchMap.get("batchNo"));
            batch.setRemark((String) batchMap.getOrDefault("remark", ""));
            Object date = batchMap.get("productionDate");
            if (date != null && !date.toString().isBlank()) {
                batch.setProductionDate(LocalDate.parse(date.toString()));
            }
            Batch savedBatch = batchService.create(batch, product.getId());
            traceDataService.saveTraceRecords(
                    savedBatch,
                    (List<Map<String, Object>>) body.get("productionRecords"),
                    (List<Map<String, Object>>) body.get("inspectionRecords"),
                    (List<Map<String, Object>>) body.get("logisticsRecords")
            );
        }
        return Result.success(product);
    }

    @GetMapping("/origins")
    public Result<?> origins() {
        return Result.success(Map.of(
                "international", List.of("中国", "新西兰", "泰国", "越南"),
                "domestic", List.of("山东烟台", "黑龙江五常", "山东日照", "云南昆明", "河北保定")
        ));
    }

    private Product mapProduct(Map<String, Object> map) {
        // create-with-trace 使用 Map 接收复合 JSON，这里只抽取产品字段，
        // 避免把 batch/records 等前端表单字段误绑定到 Product 实体。
        Product product = new Product();
        product.setName((String) map.get("name"));
        product.setCategory((String) map.get("category"));
        product.setOrigin((String) map.get("origin"));
        if (map.get("price") != null && !map.get("price").toString().isBlank()) {
            product.setPrice(new java.math.BigDecimal(map.get("price").toString()));
        }
        return product;
    }
}
