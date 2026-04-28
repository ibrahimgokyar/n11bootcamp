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

    /**
     * Eski çalışan yapı için bırakıldı.
     * Direkt availableQuantity düşürür.
     */
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

    /**
     * Eski çalışan yapı için bırakıldı.
     * Direkt availableQuantity artırır.
     */
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

    /**
     * Yeni saga akışı için:
     * Sipariş oluşunca stok kalıcı satılmış sayılmaz, rezerve edilir.
     *
     * availableQuantity -= quantity
     * reservedQuantity += quantity
     */
    @Transactional
    public StockUpdateResponse reserve(StockUpdateRequest req) {
        try {
            // önce tüm ürünleri doğrula
            for (StockUpdateRequest.StockItem it : req.getItems()) {
                ProductStock ps = repo.findById(it.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("Product not found: " + it.getProductId()));

                if (ps.getAvailableQuantity() < it.getQuantity()) {
                    throw new IllegalStateException("Insufficient stock for productId=" + it.getProductId());
                }
            }

            // sonra rezerve et
            for (StockUpdateRequest.StockItem it : req.getItems()) {
                ProductStock ps = repo.findById(it.getProductId()).orElseThrow();
                ps.reserve(it.getQuantity());
                repo.save(ps);
            }

            return StockUpdateResponse.ok("Stock reserved");
        } catch (Exception e) {
            return StockUpdateResponse.fail(e.getMessage());
        }
    }

    /**
     * Yeni saga akışı için:
     * Ödeme başarısız olursa rezerve edilen stok geri bırakılır.
     *
     * reservedQuantity -= quantity
     * availableQuantity += quantity
     */
    @Transactional
    public StockUpdateResponse release(StockUpdateRequest req) {
        try {
            // önce tüm ürünleri doğrula
            for (StockUpdateRequest.StockItem it : req.getItems()) {
                ProductStock ps = repo.findById(it.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("Product not found: " + it.getProductId()));

                if (ps.getReservedQuantity() < it.getQuantity()) {
                    throw new IllegalStateException("Insufficient reserved stock for productId=" + it.getProductId());
                }
            }

            // sonra rezervasyonu geri bırak
            for (StockUpdateRequest.StockItem it : req.getItems()) {
                ProductStock ps = repo.findById(it.getProductId()).orElseThrow();
                ps.release(it.getQuantity());
                repo.save(ps);
            }

            return StockUpdateResponse.ok("Stock released");
        } catch (Exception e) {
            return StockUpdateResponse.fail(e.getMessage());
        }
    }

    /**
     * Yeni saga akışı için:
     * Ödeme başarılı olursa rezerv satışa dönüşür.
     *
     * reservedQuantity -= quantity
     * availableQuantity zaten reserve aşamasında düşmüştü.
     */
    @Transactional
    public StockUpdateResponse commit(StockUpdateRequest req) {
        try {
            // önce tüm ürünleri doğrula
            for (StockUpdateRequest.StockItem it : req.getItems()) {
                ProductStock ps = repo.findById(it.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("Product not found: " + it.getProductId()));

                if (ps.getReservedQuantity() < it.getQuantity()) {
                    throw new IllegalStateException("Insufficient reserved stock for productId=" + it.getProductId());
                }
            }

            // sonra commit et
            for (StockUpdateRequest.StockItem it : req.getItems()) {
                ProductStock ps = repo.findById(it.getProductId()).orElseThrow();
                ps.commit(it.getQuantity());
                repo.save(ps);
            }

            return StockUpdateResponse.ok("Stock committed");
        } catch (Exception e) {
            return StockUpdateResponse.fail(e.getMessage());
        }
    }
}