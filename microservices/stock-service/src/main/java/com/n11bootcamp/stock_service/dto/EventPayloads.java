package com.n11bootcamp.stock_service.dto;

import java.util.List;

public class EventPayloads {

    public static class StockReserveRequestedEvent {
        private Long orderId;
        private String username;
        private List<Item> items;

        public StockReserveRequestedEvent() { }

        public StockReserveRequestedEvent(Long orderId, String username, List<Item> items) {
            this.orderId = orderId;
            this.username = username;
            this.items = items;
        }

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public List<Item> getItems() { return items; }
        public void setItems(List<Item> items) { this.items = items; }

        public static class Item {
            private Long productId;
            private Integer quantity;

            public Item() { }

            public Item(Long productId, Integer quantity) {
                this.productId = productId;
                this.quantity = quantity;
            }

            public Long getProductId() { return productId; }
            public void setProductId(Long productId) { this.productId = productId; }

            public Integer getQuantity() { return quantity; }
            public void setQuantity(Integer quantity) { this.quantity = quantity; }
        }
    }

    public static class StockReservedEvent {
        private Long orderId;
        private String username;
        private String message;

        public StockReservedEvent() { }

        public StockReservedEvent(Long orderId, String username, String message) {
            this.orderId = orderId;
            this.username = username;
            this.message = message;
        }

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class StockRejectedEvent {
        private Long orderId;
        private String username;
        private String reason;

        public StockRejectedEvent() { }

        public StockRejectedEvent(Long orderId, String username, String reason) {
            this.orderId = orderId;
            this.username = username;
            this.reason = reason;
        }

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
