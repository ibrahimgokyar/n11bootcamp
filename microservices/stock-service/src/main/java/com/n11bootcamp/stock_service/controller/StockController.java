package com.n11bootcamp.stock_service.controller;

import com.n11bootcamp.stock_service.dto.StockUpdateRequest;
import com.n11bootcamp.stock_service.dto.StockUpdateResponse;
import com.n11bootcamp.stock_service.service.StockDomainService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks") // ✅ plural
public class StockController {
    private final StockDomainService stock;

    public StockController(StockDomainService stock) { this.stock = stock; }

    @PostMapping("/decrease")
    public ResponseEntity<StockUpdateResponse> decrease(@RequestBody StockUpdateRequest req) {
        return ResponseEntity.ok(stock.decrease(req));
    }

    @PostMapping("/increase")
    public ResponseEntity<StockUpdateResponse> increase(@RequestBody StockUpdateRequest req) {
        return ResponseEntity.ok(stock.increase(req));
    }
}
