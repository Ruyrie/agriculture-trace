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

    /**
     * 保存某个批次下的生产、质检和物流记录。
     * 该方法通常由新增产品/新增批次流程调用，负责把前端动态表单数组落库。
     */
    @Transactional
    public void saveTraceRecords(Batch batch,
                                 List<Map<String, Object>> productionRecords,
                                 List<Map<String, Object>> inspectionRecords,
                                 List<Map<String, Object>> logisticsRecords) {
        // 三类记录都归属于同一批次，新增批次时一次性写入，前端溯源页再按 batchNo 聚合展示。
        saveProductionRecords(batch, productionRecords);
        saveInspectionRecords(batch, inspectionRecords);
        saveLogisticsRecords(batch, logisticsRecords);
    }

    /**
     * 覆盖式更新某个批次的所有溯源明细。
     * 先删除旧记录再写入新记录，适合前端动态增删行后的整体提交。
     */
    @Transactional
    public void replaceTraceRecords(Batch batch,
                                    List<Map<String, Object>> productionRecords,
                                    List<Map<String, Object>> inspectionRecords,
                                    List<Map<String, Object>> logisticsRecords) {
        // 编辑批次溯源信息时采用“先删后写”的方式，避免逐条比对前端动态表单的增删改。
        // 由于外层事务包裹，任一写入失败都会整体回滚，不会留下半套溯源记录。
        productionRecordRepository.deleteByBatch_Id(batch.getId());
        inspectionRecordRepository.deleteByBatch_Id(batch.getId());
        logisticsRecordRepository.deleteByBatch_Id(batch.getId());
        saveTraceRecords(batch, productionRecords, inspectionRecords, logisticsRecords);
    }

    /**
     * 查询某个产品下所有批次的生产记录，并转换成前端溯源页字段。
     */
    public List<Map<String, Object>> productionRows(String productId) {
        return productionRecordRepository.findRowsByProductId(productId)
                .stream()
                .map(this::toProductionRow)
                .toList();
    }

    /**
     * 查询单个批次的生产记录，供 /trace/batch/{batchId} 精准溯源使用。
     */
    public List<Map<String, Object>> productionRowsByBatch(String batchId) {
        return productionRecordRepository.findRowsByBatchId(batchId)
                .stream()
                .map(this::toProductionRow)
                .toList();
    }

    /**
     * 查询某个产品下所有批次的质检记录。
     */
    public List<Map<String, Object>> inspectionRows(String productId) {
        return inspectionRecordRepository.findRowsByProductId(productId)
                .stream()
                .map(this::toInspectionRow)
                .toList();
    }

    /**
     * 查询单个批次的质检记录。
     */
    public List<Map<String, Object>> inspectionRowsByBatch(String batchId) {
        return inspectionRecordRepository.findRowsByBatchId(batchId)
                .stream()
                .map(this::toInspectionRow)
                .toList();
    }

    /**
     * 查询某个产品下所有批次的物流记录。
     */
    public List<Map<String, Object>> logisticsRows(String productId) {
        return logisticsRecordRepository.findRowsByProductId(productId)
                .stream()
                .map(this::toLogisticsRow)
                .toList();
    }

    /**
     * 查询单个批次的物流记录。
     */
    public List<Map<String, Object>> logisticsRowsByBatch(String batchId) {
        return logisticsRecordRepository.findRowsByBatchId(batchId)
                .stream()
                .map(this::toLogisticsRow)
                .toList();
    }

    /**
     * 将前端生产记录行转换为 ProductionRecord 实体并批量保存。
     */
    private void saveProductionRecords(Batch batch, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<ProductionRecord> records = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            // sortOrder 保存前端录入顺序，溯源详情页可以按真实业务顺序展示。
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

    /**
     * 将前端质检记录行转换为 InspectionRecord 实体并批量保存。
     */
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

    /**
     * 将前端物流记录行转换为 LogisticsRecord 实体并批量保存。
     */
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

    /**
     * 把生产记录实体转换成前端 TraceDetail.vue 直接使用的字段名。
     */
    private Map<String, Object> toProductionRow(ProductionRecord record) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("batchNo", record.getBatch().getBatchNo());
        row.put("activityName", record.getActivityName());
        row.put("operator", record.getOperator());
        row.put("activityDate", record.getActivityDate());
        row.put("remark", record.getRemark());
        return row;
    }

    /**
     * 把质检记录实体转换成前端字段，并保留 item/date 兼容别名。
     */
    private Map<String, Object> toInspectionRow(InspectionRecord record) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("batchNo", record.getBatch().getBatchNo());
        row.put("inspectionItem", record.getInspectionItem());
        // item/date 是给早期前端字段名保留的兼容别名，避免页面改版时接口断裂。
        row.put("item", record.getInspectionItem());
        row.put("result", record.getResult());
        row.put("inspector", record.getInspector());
        row.put("inspectionDate", record.getInspectionDate());
        row.put("date", record.getInspectionDate());
        return row;
    }

    /**
     * 把物流记录实体转换成前端字段，并保留 node/time 兼容别名。
     */
    private Map<String, Object> toLogisticsRow(LogisticsRecord record) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("batchNo", record.getBatch().getBatchNo());
        row.put("nodeName", record.getNodeName());
        // node/time 同样是展示层兼容字段，TraceDetail.vue 可以直接读取统一名称。
        row.put("node", record.getNodeName());
        row.put("location", record.getLocation());
        row.put("operator", record.getOperator());
        row.put("updateTime", record.getUpdateTime());
        row.put("time", record.getUpdateTime());
        return row;
    }

    /**
     * 从 Map 行中安全读取字符串字段，空值转为空字符串并去掉首尾空白。
     */
    private String text(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : value.toString().trim();
    }

    /**
     * 读取字符串字段；当字段为空时使用 fallback，常用于物流时间默认当前时间。
     */
    private String defaultText(Map<String, Object> row, String key, String fallback) {
        String value = text(row, key);
        return value.isBlank() ? fallback : value;
    }

    /**
     * 从前端 YYYY-MM-DD 字符串解析 LocalDate，空字符串表示未填写日期。
     */
    private LocalDate localDate(Map<String, Object> row, String key) {
        String value = text(row, key);
        return value.isBlank() ? null : LocalDate.parse(value);
    }
}
