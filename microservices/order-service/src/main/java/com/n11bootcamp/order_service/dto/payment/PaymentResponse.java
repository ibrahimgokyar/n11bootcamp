package com.n11bootcamp.order_service.dto.payment;

public class PaymentResponse {

    private boolean success;
    private String transactionId;
    private String message;

    // Getter / Setter
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
