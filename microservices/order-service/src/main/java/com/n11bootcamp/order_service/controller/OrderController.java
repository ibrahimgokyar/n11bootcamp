package com.n11bootcamp.order_service.controller;

import com.n11bootcamp.order_service.dto.CreateOrderRequest;
import com.n11bootcamp.order_service.dto.OrderResponse;
import com.n11bootcamp.order_service.service.OrderService;
import com.n11bootcamp.order_service.service.impl.OrderServiceImpl;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = {"http://localhost:3000", "http://85.159.71.66:3000","http://94.73.134.50:3000",
        "http://localhost:8081/", "http://85.159.71.66:8081/,http://94.73.134.50:8081/"})
public class OrderController {

    private final OrderServiceImpl orderServiceImpl;
    public OrderController(OrderServiceImpl orderServiceImpl) {
        this.orderServiceImpl = orderServiceImpl;
    }

    @PostMapping
    public OrderResponse createOrder(@RequestBody CreateOrderRequest request) {
        return orderServiceImpl.createOrder(request);
    }

    @GetMapping("/all")
    public List<OrderResponse> getAllOrders() {
        return orderServiceImpl.findAllOrders();
    }

    @GetMapping("/{id}")
    public OrderResponse getOrderById(@PathVariable Long id) {
        return orderServiceImpl.getOrderById(id);
    }

    @GetMapping("/user/{username}")
    public List<OrderResponse> getOrdersByUser(@PathVariable String username) {
        return orderServiceImpl.findOrdersByUsername(username);
    }
}
