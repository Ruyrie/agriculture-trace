package com.example.agriculturetrace.controller;

import com.example.agriculturetrace.entity.Batch;
import com.example.agriculturetrace.service.BatchService;
import com.example.agriculturetrace.service.TraceDataService;
import com.example.agriculturetrace.util.Result;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 批次管理接口。
 */
@RestController
@RequestMapping("/api/batch")
public class BatchController {

    private final BatchService batchService;
    private final TraceDataService traceDataService;

    public BatchController(BatchService batchService, TraceDataService traceDataService) {
        this.batchService = batchService;
        this.traceDataService = traceDataService;
    }

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String productId,
                          @RequestParam(required = false) String batchNo) {
        Page<Batch> batches = batchService.list(productId, batchNo, page, pageSize);
        return Result.success(Map.of(
                "records", batches.getContent().stream().map(batchService::toRow).toList(),
                "total", batches.getTotalElements(),
                "page", batches.getNumber() + 1,
                "pageSize", batches.getSize()
        ));
    }

    @PostMapping
    @SuppressWarnings("unchecked")
    @Transactional
    public Result<?> create(@RequestBody Map<String, Object> body) {
        Batch batch = mapBatch(body);
        String productId = (String) body.get("productId");
        Batch savedBatch = batchService.create(batch, (String) body.get("productId"));
        traceDataService.saveTraceRecords(
                savedBatch,
                (List<Map<String, Object>>) body.get("productionRecords"),
                (List<Map<String, Object>>) body.get("inspectionRecords"),
                (List<Map<String, Object>>) body.get("logisticsRecords")
        );
        return Result.success(batchService.toRow(savedBatch, productId));
    }

    @PutMapping
    @SuppressWarnings("unchecked")
    @Transactional
    public Result<?> update(@RequestBody Map<String, Object> body) {
        Batch batch = mapBatch(body);
        batch.setId((String) body.get("id"));
        String productId = (String) body.get("productId");
        Batch savedBatch = batchService.update(batch, (String) body.get("productId"));
        if (body.containsKey("productionRecords")
                || body.containsKey("inspectionRecords")
                || body.containsKey("logisticsRecords")) {
            traceDataService.replaceTraceRecords(
                    savedBatch,
                    (List<Map<String, Object>>) body.get("productionRecords"),
                    (List<Map<String, Object>>) body.get("inspectionRecords"),
                    (List<Map<String, Object>>) body.get("logisticsRecords")
            );
        }
        return Result.success(batchService.toRow(savedBatch, productId));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable String id) {
        batchService.delete(id);
        return Result.success(null);
    }

    private Batch mapBatch(Map<String, Object> body) {
        Batch batch = new Batch();
        batch.setBatchNo((String) body.get("batchNo"));
        batch.setRemark((String) body.get("remark"));
        Object date = body.get("productionDate");
        if (date != null && !date.toString().isBlank()) {
            batch.setProductionDate(java.time.LocalDate.parse(date.toString()));
        }
        return batch;
    }
}
