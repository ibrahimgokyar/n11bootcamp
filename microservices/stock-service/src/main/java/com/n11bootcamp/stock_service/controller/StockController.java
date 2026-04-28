package com.n11bootcamp.stock_service.controller;

import com.n11bootcamp.stock_service.dto.StockUpdateRequest;
import com.n11bootcamp.stock_service.dto.StockUpdateResponse;
import com.n11bootcamp.stock_service.service.StockDomainService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockDomainService stock;

    public StockController(StockDomainService stock) {
        this.stock = stock;
    }

    // Eski çalışan yapı: direkt availableQuantity düşürür
    @PostMapping("/decrease")
    public ResponseEntity<StockUpdateResponse> decrease(@RequestBody StockUpdateRequest req) {
        return ResponseEntity.ok(stock.decrease(req));
    }

    // Eski çalışan yapı: direkt availableQuantity artırır
    @PostMapping("/increase")
    public ResponseEntity<StockUpdateResponse> increase(@RequestBody StockUpdateRequest req) {
        return ResponseEntity.ok(stock.increase(req));
    }

    // Yeni saga yapısı: availableQuantity düşer, reservedQuantity artar
    @PostMapping("/reserve")
    public ResponseEntity<StockUpdateResponse> reserve(@RequestBody StockUpdateRequest req) {
        return ResponseEntity.ok(stock.reserve(req));
    }

    // Yeni saga yapısı: payment fail/cancel durumunda reservedQuantity düşer, availableQuantity geri artar
    @PostMapping("/release")
    public ResponseEntity<StockUpdateResponse> release(@RequestBody StockUpdateRequest req) {
        return ResponseEntity.ok(stock.release(req));
    }

    // Yeni saga yapısı: payment success durumunda reservedQuantity düşer, satış kesinleşir
    @PostMapping("/commit")
    public ResponseEntity<StockUpdateResponse> commit(@RequestBody StockUpdateRequest req) {
        return ResponseEntity.ok(stock.commit(req));
    }
}