package com.n11bootcamp.order_service.dto.stock;

import java.util.List;

public class StockUpdateResponse {

    private boolean success;
    private String message;
    private List<StockItemResult> results;

    public static class StockItemResult {
        private Long productId;
        private Integer oldQuantity;
        private Integer newQuantity;

        // Getter / Setter
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public Integer getOldQuantity() { return oldQuantity; }
        public void setOldQuantity(Integer oldQuantity) { this.oldQuantity = oldQuantity; }

        public Integer getNewQuantity() { return newQuantity; }
        public void setNewQuantity(Integer newQuantity) { this.newQuantity = newQuantity; }
    }

    // Getter / Setter
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<StockItemResult> getResults() { return results; }
    public void setResults(List<StockItemResult> results) { this.results = results; }
}
