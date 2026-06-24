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
    private static final List<String> INTERNATIONAL_ORIGINS = List.of(
            "中国", "日本", "韩国", "泰国", "越南", "马来西亚", "印度尼西亚", "菲律宾", "印度", "巴基斯坦",
            "澳大利亚", "新西兰", "美国", "加拿大", "墨西哥", "巴西", "阿根廷", "智利", "秘鲁", "哥伦比亚",
            "英国", "法国", "德国", "意大利", "西班牙", "葡萄牙", "荷兰", "比利时", "丹麦", "挪威", "瑞典",
            "俄罗斯", "乌克兰", "波兰", "土耳其", "以色列", "南非", "埃及", "摩洛哥", "埃塞俄比亚", "肯尼亚"
    );
    private static final List<String> DOMESTIC_ORIGINS = List.of(
            "北京", "天津", "河北", "山西", "内蒙古", "辽宁", "吉林", "黑龙江", "上海", "江苏", "浙江", "安徽",
            "福建", "江西", "山东", "河南", "湖北", "湖南", "广东", "广西", "海南", "重庆", "四川", "贵州",
            "云南", "西藏", "陕西", "甘肃", "青海", "宁夏", "新疆", "香港", "澳门", "台湾",
            "山东烟台", "黑龙江五常", "山东日照", "云南昆明", "河北保定"
    );

    public ProductController(ProductService productService,
                             BatchService batchService,
                             TraceDataService traceDataService) {
        this.productService = productService;
        this.batchService = batchService;
        this.traceDataService = traceDataService;
    }

    /**
     * 分页查询产品列表，支持按产品名称 keyword 模糊搜索。
     * 返回 records/total/page/pageSize，正好对应前端表格分页组件需要的数据结构。
     */
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

    /**
     * 查询单个产品详情，供编辑弹窗、溯源辅助页面等按 ID 读取完整产品字段。
     */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable String id) {
        return Result.success(productService.get(id));
    }

    /**
     * 新增产品基础信息。
     * 真正的 ID、创建时间、数据指纹和审计日志由 ProductService 统一处理。
     */
    @PostMapping
    public Result<?> create(@RequestBody Product product) {
        return Result.success(productService.create(product));
    }

    /**
     * 更新产品基础字段；Service 会判断字段是否真的变化，变化时重算 dataHash 并写审计链。
     */
    @PutMapping
    public Result<?> update(@RequestBody Product product) {
        return Result.success(productService.update(product));
    }

    /**
     * 删除产品并记录删除前快照，便于审计页面追踪被删除对象的原始数据。
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable String id) {
        productService.delete(id);
        return Result.success(null);
    }

    /**
     * 一次性创建产品、首个批次和三类溯源明细。
     * 这个接口服务“新增产品时顺手录入完整溯源信息”的前端流程。
     */
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

    /**
     * 返回前端产品产地下拉框的候选值，区分国内产地和国际来源。
     */
    @GetMapping("/origins")
    public Result<?> origins() {
        return Result.success(Map.of(
                "international", INTERNATIONAL_ORIGINS,
                "domestic", DOMESTIC_ORIGINS
        ));
    }

    /**
     * 从复合请求体中提取产品字段并组装 Product 实体。
     * 前端提交的批次和记录字段会留给其他服务处理。
     */
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
