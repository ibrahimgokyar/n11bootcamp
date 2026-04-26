package com.n11bootcamp.stock_service.service;


import com.n11bootcamp.stock_service.entity.ProductStock;
import com.n11bootcamp.stock_service.repository.ProductStockRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StockInitDataRunner implements CommandLineRunner {

    private final ProductStockRepository repo;

    public StockInitDataRunner(ProductStockRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) {
        if (repo.count() == 0) {
            repo.save(new ProductStock(1L, "Ürün 1", 50));
            repo.save(new ProductStock(2L, "Ürün 2", 30));
            repo.save(new ProductStock(3L, "Ürün 3", 10));
            repo.save(new ProductStock(4L, "Ürün 4", 40));
            repo.save(new ProductStock(5L, "Ürün 5", 50));
            repo.save(new ProductStock(6L, "Ürün 6", 60));
            repo.save(new ProductStock(7L, "Ürün 7", 70));
            repo.save(new ProductStock(8L, "Ürün 8", 80));

        }
    }
}
