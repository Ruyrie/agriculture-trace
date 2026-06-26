package com.example.agriculturetrace.controller;

import com.example.agriculturetrace.entity.Batch;
import com.example.agriculturetrace.entity.Product;
import com.example.agriculturetrace.entity.TraceRecord;
import com.example.agriculturetrace.repository.BatchRepository;
import com.example.agriculturetrace.repository.ProductRepository;
import com.example.agriculturetrace.repository.TraceRecordRepository;
import com.example.agriculturetrace.service.TraceDataService;
import com.example.agriculturetrace.util.Ids;
import com.example.agriculturetrace.util.Result;
import com.example.agriculturetrace.util.TimeUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 产品溯源详情接口。
 *
 * 访问时写入 trace_record，用于近一周趋势和总访问次数统计。
 */
@RestController
@RequestMapping("/api/trace")
public class TraceController {

    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final TraceRecordRepository traceRecordRepository;
    private final TraceDataService traceDataService;

    public TraceController(ProductRepository productRepository,
                           BatchRepository batchRepository,
                           TraceRecordRepository traceRecordRepository,
                           TraceDataService traceDataService) {
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.traceRecordRepository = traceRecordRepository;
        this.traceDataService = traceDataService;
    }

    /**
     * 按产品 ID 查询公开溯源详情。
     * 每次访问都会记录 trace_record，用于统计扫码/访问趋势。
     */
    @GetMapping("/{productId}")
    public Result<?> trace(@PathVariable String productId, HttpServletRequest request) {
        // 产品不存在时 orElseThrow() 抛 NoSuchElementException，由 GlobalExceptionHandler 转成 404。
        Product product = productRepository.findById(productId).orElseThrow();
        // 记录本次访问，用于统计近 7 天溯源趋势和总扫码量。
        recordVisit(product, request);

        // 查该产品的所有批次，按创建时间倒序，让最新批次排在前面。
        List<Batch> batches = batchRepository.findByProduct_IdOrderByCreateTimeDesc(productId);
        return Result.success(Map.of(
                "product", product,
                // 批次只返回展示字段，null 转空字符串防止前端渲染报错。
                "batches", batches.stream().map(batch -> Map.of(
                        "batchNo", batch.getBatchNo(),
                        "productionDate", batch.getProductionDate() == null ? "" : batch.getProductionDate().toString(),
                        "remark", batch.getRemark() == null ? "" : batch.getRemark(),
                        "imageUrls", batch.getImageUrls() == null ? "" : batch.getImageUrls()
                )).toList(),
                // 三类溯源明细聚合该产品所有批次的记录，让溯源页一次性看到完整生命周期。
                "productionRecords", traceDataService.productionRows(productId),
                "inspectionReports", traceDataService.inspectionRows(productId),
                "logistics", traceDataService.logisticsRows(productId)
        ));
    }

    /**
     * 按批次 ID 查询公开溯源详情，只返回当前批次关联的生产、质检和物流记录。
     */
    @GetMapping("/batch/{batchId}")
    public Result<?> batchTrace(@PathVariable String batchId, HttpServletRequest request) {
        // findDetailById 使用 @EntityGraph 预取 batch.product，避免下面 getProduct() 触发 LazyInit 异常。
        Batch batch = batchRepository.findDetailById(batchId).orElseThrow();
        // batch.getProduct() 此时已加载完毕，不会触发额外的 SQL 查询。
        Product product = batch.getProduct();
        recordVisit(product, request);

        return Result.success(Map.of(
                "product", product,
                // 精准溯源只包含当前批次，用 List.of() 包装成单元素列表，与按产品溯源保持相同数据结构。
                "batches", List.of(Map.of(
                        "batchNo", batch.getBatchNo(),
                        "productionDate", batch.getProductionDate() == null ? "" : batch.getProductionDate().toString(),
                        "remark", batch.getRemark() == null ? "" : batch.getRemark(),
                        "imageUrls", batch.getImageUrls() == null ? "" : batch.getImageUrls()
                )),
                // 以 batchId 为维度查询三类明细，只返回当前批次的溯源记录，不跨批次聚合。
                "productionRecords", traceDataService.productionRowsByBatch(batchId),
                "inspectionReports", traceDataService.inspectionRowsByBatch(batchId),
                "logistics", traceDataService.logisticsRowsByBatch(batchId)
        ));
    }

    /**
     * 记录一次溯源访问，包括产品、访问时间和客户端 IP。
     * 这些记录后续被 DashboardController 用来统计访问趋势。
     */
    private void recordVisit(Product product, HttpServletRequest request) {
        TraceRecord record = new TraceRecord();
        // uuid32() 生成 32 位无横线 UUID，满足 trace_record 主键 varchar(32) 设计。
        record.setId(Ids.uuid32());
        // 关联到被访问的产品，便于后续按产品聚合统计访问量排行。
        record.setProduct(product);
        // 记录服务器本地时间字符串，作为 trace_time 存储；注意这是写入时刻，不是用户请求时刻。
        record.setTraceTime(TimeUtils.nowText());
        // getRemoteAddr() 返回客户端 IP；若经过反向代理（Nginx），需用 X-Forwarded-For 头获取真实 IP。
        record.setIp(request.getRemoteAddr());
        traceRecordRepository.save(record);
    }
}
