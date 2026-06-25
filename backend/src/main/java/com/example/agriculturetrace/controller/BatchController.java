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

    /**
     * 分页查询批次列表，支持按产品 ID、产品名称和批次号组合筛选。
     * Controller 将实体批次转换为前端更好展示的行对象。
     */
    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String productId,
                          @RequestParam(required = false) String productName,
                          @RequestParam(required = false) String batchNo) {
        Page<Batch> batches = batchService.list(productId, productName, batchNo, page, pageSize);
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
    /**
     * 新增批次，并可同步保存生产、质检、物流三类溯源明细。
     * 批次基础信息和明细写入处于同一事务，任一失败都会整体回滚。
     */
    public Result<?> create(@RequestBody Map<String, Object> body) {
        // 新增批次时允许同步提交生产、质检、物流明细；
        // 与 ProductController#createWithTrace 一样，使用事务保证批次和明细同生共死。
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
    /**
     * 更新批次基础信息；当请求体包含溯源数组时，同时覆盖该批次的三类明细。
     * 不带数组时只更新批次字段，避免误删已有溯源记录。
     */
    public Result<?> update(@RequestBody Map<String, Object> body) {
        Batch batch = mapBatch(body);
        batch.setId((String) body.get("id"));
        String productId = (String) body.get("productId");
        Batch savedBatch = batchService.update(batch, (String) body.get("productId"));
        if (body.containsKey("productionRecords")
                || body.containsKey("inspectionRecords")
                || body.containsKey("logisticsRecords")) {
            // 只有请求体明确带了溯源数组时才覆盖明细，避免普通批次字段编辑误清空溯源记录。
            traceDataService.replaceTraceRecords(
                    savedBatch,
                    (List<Map<String, Object>>) body.get("productionRecords"),
                    (List<Map<String, Object>>) body.get("inspectionRecords"),
                    (List<Map<String, Object>>) body.get("logisticsRecords")
            );
        }
        return Result.success(batchService.toRow(savedBatch, productId));
    }

    /**
     * 删除指定批次并写入审计日志。
     * 关联的明细记录由数据库外键/仓储删除策略配合处理。
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable String id) {
        batchService.delete(id);
        return Result.success(null);
    }

    /**
     * 将前端 Map 请求体里的批次字段转换成 Batch 实体。
     * 这里只处理 batchNo、remark、productionDate，产品关联由 Service 根据 productId 设置。
     */
    private Batch mapBatch(Map<String, Object> body) {
        // Controller 保持轻量，只负责把前端 JSON 中的基础字段转换成实体对象。
        Batch batch = new Batch();
        batch.setBatchNo((String) body.get("batchNo"));
        batch.setRemark((String) body.get("remark"));
        batch.setImageUrls((String) body.get("imageUrls"));
        Object date = body.get("productionDate");
        if (date != null && !date.toString().isBlank()) {
            batch.setProductionDate(java.time.LocalDate.parse(date.toString()));
        }
        return batch;
    }
}
