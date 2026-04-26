package com.n11bootcamp.stock_service.service;

import com.n11bootcamp.stock_service.dto.StockUpdateRequest;
import com.n11bootcamp.stock_service.dto.StockUpdateResponse;
import com.n11bootcamp.stock_service.entity.ProductStock;
import com.n11bootcamp.stock_service.repository.ProductStockRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class StockDomainService {

    private final ProductStockRepository repo;

    public StockDomainService(ProductStockRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public StockUpdateResponse decrease(StockUpdateRequest req) {
        try {
            // önce doğrula
            for (StockUpdateRequest.StockItem it : req.getItems()) {
                ProductStock ps = repo.findById(it.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("Product not found: " + it.getProductId()));
                if (ps.getAvailableQuantity() < it.getQuantity()) {
                    throw new IllegalStateException("Insufficient stock for productId=" + it.getProductId());
                }
            }
            // uygula
            for (StockUpdateRequest.StockItem it : req.getItems()) {
                ProductStock ps = repo.findById(it.getProductId()).orElseThrow();
                ps.decrease(it.getQuantity());
                repo.save(ps);
            }
            return StockUpdateResponse.ok("Stock decreased");
        } catch (Exception e) {
            return StockUpdateResponse.fail(e.getMessage());
        }
    }

    @Transactional
    public StockUpdateResponse increase(StockUpdateRequest req) {
        try {
            for (StockUpdateRequest.StockItem it : req.getItems()) {
                ProductStock ps = repo.findById(it.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("Product not found: " + it.getProductId()));
                ps.increase(it.getQuantity());
                repo.save(ps);
            }
            return StockUpdateResponse.ok("Stock increased");
        } catch (Exception e) {
            return StockUpdateResponse.fail(e.getMessage());
        }
    }
}

