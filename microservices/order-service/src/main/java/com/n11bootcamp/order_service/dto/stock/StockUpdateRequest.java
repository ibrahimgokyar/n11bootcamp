package com.n11bootcamp.order_service.dto.stock;


import java.util.List;

public class StockUpdateRequest {

    private List<StockItem> items;

    public static class StockItem {
        private Long productId;
        private Integer quantity;

        // Getter / Setter
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    // Getter / Setter
    public List<StockItem> getItems() { return items; }
    public void setItems(List<StockItem> items) { this.items = items; }
}
