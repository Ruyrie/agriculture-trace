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

    @GetMapping("/{productId}")
    public Result<?> trace(@PathVariable String productId, HttpServletRequest request) {
        Product product = productRepository.findById(productId).orElseThrow();
        recordVisit(product, request);

        List<Batch> batches = batchRepository.findByProduct_IdOrderByCreateTimeDesc(productId);
        return Result.success(Map.of(
                "product", product,
                "batches", batches.stream().map(batch -> Map.of(
                        "batchNo", batch.getBatchNo(),
                        "productionDate", batch.getProductionDate() == null ? "" : batch.getProductionDate().toString(),
                        "remark", batch.getRemark() == null ? "" : batch.getRemark()
                )).toList(),
                "productionRecords", traceDataService.productionRows(productId),
                "inspectionReports", traceDataService.inspectionRows(productId),
                "logistics", traceDataService.logisticsRows(productId)
        ));
    }

    @GetMapping("/batch/{batchId}")
    public Result<?> batchTrace(@PathVariable String batchId, HttpServletRequest request) {
        Batch batch = batchRepository.findDetailById(batchId).orElseThrow();
        Product product = batch.getProduct();
        recordVisit(product, request);

        return Result.success(Map.of(
                "product", product,
                "batches", List.of(Map.of(
                        "batchNo", batch.getBatchNo(),
                        "productionDate", batch.getProductionDate() == null ? "" : batch.getProductionDate().toString(),
                        "remark", batch.getRemark() == null ? "" : batch.getRemark()
                )),
                "productionRecords", traceDataService.productionRowsByBatch(batchId),
                "inspectionReports", traceDataService.inspectionRowsByBatch(batchId),
                "logistics", traceDataService.logisticsRowsByBatch(batchId)
        ));
    }

    private void recordVisit(Product product, HttpServletRequest request) {
        TraceRecord record = new TraceRecord();
        record.setId(Ids.uuid32());
        record.setProduct(product);
        record.setTraceTime(TimeUtils.nowText());
        record.setIp(request.getRemoteAddr());
        traceRecordRepository.save(record);
    }
}
