package com.n11bootcamp.order_service.service;

import com.n11bootcamp.order_service.dto.stock.StockUpdateRequest;
import com.n11bootcamp.order_service.dto.stock.StockUpdateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "stock-service", path = "/api/stocks")
public interface StockServiceClient {

    // Eski çalışan yapı: direkt stok düşürme
    @PostMapping("/decrease")
    StockUpdateResponse decreaseStock(@RequestBody StockUpdateRequest request);

    // Eski çalışan yapı: direkt stok geri artırma
    @PostMapping("/increase")
    StockUpdateResponse increaseStock(@RequestBody StockUpdateRequest request);

    // Yeni saga yapısı: stok rezerve etme
    @PostMapping("/reserve")
    StockUpdateResponse reserveStock(@RequestBody StockUpdateRequest request);

    // Yeni saga yapısı: ödeme başarısızsa rezervasyonu geri bırakma
    @PostMapping("/release")
    StockUpdateResponse releaseStock(@RequestBody StockUpdateRequest request);

    // Yeni saga yapısı: ödeme başarılıysa rezervasyonu satışa çevirme
    @PostMapping("/commit")
    StockUpdateResponse commitStock(@RequestBody StockUpdateRequest request);
}