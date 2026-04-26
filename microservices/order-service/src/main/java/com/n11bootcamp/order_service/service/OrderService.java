package com.n11bootcamp.order_service.service;

import com.n11bootcamp.order_service.dto.CreateOrderRequest;
import com.n11bootcamp.order_service.dto.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request);

    // Yeni: vendor'a ait siparişleri page'li döner
    // Admin için tüm siparişleri listele
    List<OrderResponse> findAllOrders();

    // Belirli siparişi getir
    OrderResponse getOrderById(Long orderId);

    // --- Yeni: kullanıcının siparişlerini getir ---
    List<OrderResponse> findOrdersByUsername(String username);
}