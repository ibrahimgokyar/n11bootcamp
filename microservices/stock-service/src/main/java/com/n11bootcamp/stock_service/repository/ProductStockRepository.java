package com.n11bootcamp.stock_service.repository;


import com.n11bootcamp.stock_service.entity.ProductStock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductStockRepository extends JpaRepository<ProductStock, Long> { }
