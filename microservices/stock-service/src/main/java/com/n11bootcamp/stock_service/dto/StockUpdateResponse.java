package com.n11bootcamp.stock_service.dto;


public class StockUpdateResponse {
    private boolean success;
    private String message;

    public StockUpdateResponse() { }

    public StockUpdateResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static StockUpdateResponse ok(String msg) {
        return new StockUpdateResponse(true, msg);
    }

    public static StockUpdateResponse fail(String msg) {
        return new StockUpdateResponse(false, msg);
    }
}
