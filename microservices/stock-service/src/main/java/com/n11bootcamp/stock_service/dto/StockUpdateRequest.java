package com.n11bootcamp.stock_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class StockUpdateRequest {

    @NotNull
    private List<StockItem> items;

    public StockUpdateRequest() { }

    public StockUpdateRequest(List<StockItem> items) {
        this.items = items;
    }

    public List<StockItem> getItems() { return items; }
    public void setItems(List<StockItem> items) { this.items = items; }

    // inner DTO
    public static class StockItem {
        @NotNull
        private Long productId;

        @Min(1)
        private Integer quantity;

        public StockItem() { }

        public StockItem(Long productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
