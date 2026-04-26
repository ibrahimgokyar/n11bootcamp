package com.n11bootcamp.stock_service.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "product_stock")
public class ProductStock {

    @Id
    private Long productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer availableQuantity;

    public ProductStock() {}

    public ProductStock(Long productId, String productName, Integer availableQuantity) {
        this.productId = productId;
        this.productName = productName;
        this.availableQuantity = availableQuantity;
    }

    // getters & setters
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }

    // domain methods
    public void decrease(int q) {
        if (q < 0) throw new IllegalArgumentException("q<0");
        if (availableQuantity < q) throw new IllegalStateException("Insufficient stock");
        availableQuantity -= q;
    }

    public void increase(int q) {
        if (q < 0) throw new IllegalArgumentException("q<0");
        availableQuantity += q;
    }
}
