package com.example.agriculturetrace.service;

import com.example.agriculturetrace.entity.Batch;
import com.example.agriculturetrace.entity.Product;
import com.example.agriculturetrace.repository.BatchRepository;
import com.example.agriculturetrace.repository.ProductRepository;
import com.example.agriculturetrace.util.Ids;
import com.example.agriculturetrace.util.TimeUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 批次管理服务。
 */
@Service
public class BatchService {

    private final BatchRepository batchRepository;
    private final ProductRepository productRepository;

    public BatchService(BatchRepository batchRepository, ProductRepository productRepository) {
        this.batchRepository = batchRepository;
        this.productRepository = productRepository;
    }

    public Page<Batch> list(String productId, String batchNo, int page, int pageSize) {
        PageRequest request = PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by(Sort.Direction.DESC, "productionDate"));
        boolean hasProduct = productId != null && !productId.isBlank();
        boolean hasBatchNo = batchNo != null && !batchNo.isBlank();
        if (!hasProduct && !hasBatchNo) {
            return batchRepository.findAll(request);
        }
        if (hasProduct && hasBatchNo) {
            return batchRepository.findByProduct_IdAndBatchNoContaining(productId, batchNo.trim(), request);
        }
        if (hasProduct) {
            return batchRepository.findByProduct_Id(productId, request);
        }
        return batchRepository.findByBatchNoContaining(batchNo.trim(), request);
    }

    @Transactional
    public Batch create(Batch batch, String productId) {
        ensureBatchNoAvailable(batch.getBatchNo(), null);
        Product product = productRepository.findById(productId).orElseThrow();
        batch.setId(Ids.uuid32());
        batch.setProduct(product);
        batch.setCreateTime(TimeUtils.nowText());
        return batchRepository.save(batch);
    }

    @Transactional
    public Batch update(Batch batch, String productId) {
        Batch existing = batchRepository.findById(batch.getId()).orElseThrow();
        ensureBatchNoAvailable(batch.getBatchNo(), existing.getId());
        if (productId != null && !productId.isBlank()) {
            existing.setProduct(productRepository.findById(productId).orElseThrow());
        }
        existing.setBatchNo(batch.getBatchNo());
        existing.setProductionDate(batch.getProductionDate());
        existing.setRemark(batch.getRemark());
        return batchRepository.save(existing);
    }

    public void delete(String id) {
        batchRepository.deleteById(id);
    }

    public Map<String, Object> toRow(Batch batch) {
        return toRow(batch, batch.getProduct().getId());
    }

    public Map<String, Object> toRow(Batch batch, String productId) {
        Product product = productRepository.findById(productId).orElseThrow();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", batch.getId());
        row.put("batchNo", batch.getBatchNo());
        row.put("productId", product.getId());
        row.put("productName", product.getName());
        row.put("productionDate", batch.getProductionDate());
        row.put("remark", batch.getRemark());
        row.put("createTime", batch.getCreateTime());
        return row;
    }

    public Map<String, Object> toRowById(String id) {
        Batch batch = batchRepository.findDetailById(id).orElseThrow();
        return toRow(batch);
    }

    private void ensureBatchNoAvailable(String batchNo, String currentId) {
        if (batchNo == null || batchNo.isBlank()) {
            throw new IllegalArgumentException("批次号不能为空");
        }
        batchRepository.findByBatchNo(batchNo.trim())
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("批次号已存在，请更换批次号");
                });
    }
}
