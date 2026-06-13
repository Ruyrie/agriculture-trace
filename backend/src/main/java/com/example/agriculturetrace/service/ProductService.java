package com.example.agriculturetrace.service;

import com.example.agriculturetrace.entity.Product;
import com.example.agriculturetrace.repository.ProductRepository;
import com.example.agriculturetrace.util.Ids;
import com.example.agriculturetrace.util.TimeUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * 产品管理服务。
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<Product> list(String keyword, int page, int pageSize) {
        PageRequest request = PageRequest.of(Math.max(page - 1, 0), pageSize);
        if (keyword == null || keyword.isBlank()) {
            return productRepository.findAll(request);
        }
        return productRepository.findByNameContainingIgnoreCase(keyword, request);
    }

    public Product get(String id) {
        return productRepository.findById(id).orElseThrow();
    }

    public Product create(Product product) {
        product.setId(Ids.uuid32());
        product.setCreateTime(TimeUtils.nowText());
        return productRepository.save(product);
    }

    public Product update(Product product) {
        Product existing = get(product.getId());
        existing.setName(product.getName());
        existing.setCategory(product.getCategory());
        existing.setOrigin(product.getOrigin());
        existing.setPrice(product.getPrice());
        return productRepository.save(existing);
    }

    public void delete(String id) {
        productRepository.deleteById(id);
    }
}
