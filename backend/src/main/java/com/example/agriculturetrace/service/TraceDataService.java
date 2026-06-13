package com.example.agriculturetrace.service;

import com.example.agriculturetrace.entity.Batch;
import com.example.agriculturetrace.entity.InspectionRecord;
import com.example.agriculturetrace.entity.LogisticsRecord;
import com.example.agriculturetrace.entity.ProductionRecord;
import com.example.agriculturetrace.repository.InspectionRecordRepository;
import com.example.agriculturetrace.repository.LogisticsRecordRepository;
import com.example.agriculturetrace.repository.ProductionRecordRepository;
import com.example.agriculturetrace.util.Ids;
import com.example.agriculturetrace.util.TimeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 溯源业务记录服务。
 *
 * 负责把新增产品时填写的生产、质检、物流记录落库，并按前端展示结构返回。
 */
@Service
public class TraceDataService {

    private final ProductionRecordRepository productionRecordRepository;
    private final InspectionRecordRepository inspectionRecordRepository;
    private final LogisticsRecordRepository logisticsRecordRepository;

    public TraceDataService(ProductionRecordRepository productionRecordRepository,
                            InspectionRecordRepository inspectionRecordRepository,
                            LogisticsRecordRepository logisticsRecordRepository) {
        this.productionRecordRepository = productionRecordRepository;
        this.inspectionRecordRepository = inspectionRecordRepository;
        this.logisticsRecordRepository = logisticsRecordRepository;
    }

    @Transactional
    public void saveTraceRecords(Batch batch,
                                 List<Map<String, Object>> productionRecords,
                                 List<Map<String, Object>> inspectionRecords,
                                 List<Map<String, Object>> logisticsRecords) {
        saveProductionRecords(batch, productionRecords);
        saveInspectionRecords(batch, inspectionRecords);
        saveLogisticsRecords(batch, logisticsRecords);
    }

    @Transactional
    public void replaceTraceRecords(Batch batch,
                                    List<Map<String, Object>> productionRecords,
                                    List<Map<String, Object>> inspectionRecords,
                                    List<Map<String, Object>> logisticsRecords) {
        productionRecordRepository.deleteByBatch_Id(batch.getId());
        inspectionRecordRepository.deleteByBatch_Id(batch.getId());
        logisticsRecordRepository.deleteByBatch_Id(batch.getId());
        saveTraceRecords(batch, productionRecords, inspectionRecords, logisticsRecords);
    }

    public List<Map<String, Object>> productionRows(String productId) {
        return productionRecordRepository.findRowsByProductId(productId)
                .stream()
                .map(this::toProductionRow)
                .toList();
    }

    public List<Map<String, Object>> productionRowsByBatch(String batchId) {
        return productionRecordRepository.findRowsByBatchId(batchId)
                .stream()
                .map(this::toProductionRow)
                .toList();
    }

    public List<Map<String, Object>> inspectionRows(String productId) {
        return inspectionRecordRepository.findRowsByProductId(productId)
                .stream()
                .map(this::toInspectionRow)
                .toList();
    }

    public List<Map<String, Object>> inspectionRowsByBatch(String batchId) {
        return inspectionRecordRepository.findRowsByBatchId(batchId)
                .stream()
                .map(this::toInspectionRow)
                .toList();
    }

    public List<Map<String, Object>> logisticsRows(String productId) {
        return logisticsRecordRepository.findRowsByProductId(productId)
                .stream()
                .map(this::toLogisticsRow)
                .toList();
    }

    public List<Map<String, Object>> logisticsRowsByBatch(String batchId) {
        return logisticsRecordRepository.findRowsByBatchId(batchId)
                .stream()
                .map(this::toLogisticsRow)
                .toList();
    }

    private void saveProductionRecords(Batch batch, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<ProductionRecord> records = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            ProductionRecord record = new ProductionRecord();
            record.setId(Ids.uuid32());
            record.setBatch(batch);
            record.setActivityName(text(row, "activityName"));
            record.setOperator(text(row, "operator"));
            record.setActivityDate(localDate(row, "activityDate"));
            record.setRemark(text(row, "remark"));
            record.setSortOrder(i + 1);
            records.add(record);
        }
        productionRecordRepository.saveAll(records);
    }

    private void saveInspectionRecords(Batch batch, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<InspectionRecord> records = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            InspectionRecord record = new InspectionRecord();
            record.setId(Ids.uuid32());
            record.setBatch(batch);
            record.setInspectionItem(text(row, "inspectionItem"));
            record.setResult(text(row, "result"));
            record.setInspector(text(row, "inspector"));
            record.setInspectionDate(localDate(row, "inspectionDate"));
            record.setSortOrder(i + 1);
            records.add(record);
        }
        inspectionRecordRepository.saveAll(records);
    }

    private void saveLogisticsRecords(Batch batch, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<LogisticsRecord> records = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            LogisticsRecord record = new LogisticsRecord();
            record.setId(Ids.uuid32());
            record.setBatch(batch);
            record.setNodeName(text(row, "nodeName"));
            record.setLocation(text(row, "location"));
            record.setOperator(text(row, "operator"));
            record.setUpdateTime(defaultText(row, "updateTime", TimeUtils.nowText()));
            record.setSortOrder(i + 1);
            records.add(record);
        }
        logisticsRecordRepository.saveAll(records);
    }

    private Map<String, Object> toProductionRow(ProductionRecord record) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("batchNo", record.getBatch().getBatchNo());
        row.put("activityName", record.getActivityName());
        row.put("operator", record.getOperator());
        row.put("activityDate", record.getActivityDate());
        row.put("remark", record.getRemark());
        return row;
    }

    private Map<String, Object> toInspectionRow(InspectionRecord record) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("batchNo", record.getBatch().getBatchNo());
        row.put("inspectionItem", record.getInspectionItem());
        row.put("item", record.getInspectionItem());
        row.put("result", record.getResult());
        row.put("inspector", record.getInspector());
        row.put("inspectionDate", record.getInspectionDate());
        row.put("date", record.getInspectionDate());
        return row;
    }

    private Map<String, Object> toLogisticsRow(LogisticsRecord record) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("batchNo", record.getBatch().getBatchNo());
        row.put("nodeName", record.getNodeName());
        row.put("node", record.getNodeName());
        row.put("location", record.getLocation());
        row.put("operator", record.getOperator());
        row.put("updateTime", record.getUpdateTime());
        row.put("time", record.getUpdateTime());
        return row;
    }

    private String text(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : value.toString().trim();
    }

    private String defaultText(Map<String, Object> row, String key, String fallback) {
        String value = text(row, key);
        return value.isBlank() ? fallback : value;
    }

    private LocalDate localDate(Map<String, Object> row, String key) {
        String value = text(row, key);
        return value.isBlank() ? null : LocalDate.parse(value);
    }
}
