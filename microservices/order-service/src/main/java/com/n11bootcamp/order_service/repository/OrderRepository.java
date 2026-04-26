package com.n11bootcamp.order_service.repository;

import com.n11bootcamp.order_service.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Admin için tüm siparişleri getirmek yeterli

    List<Order> findByUsername(String username);
}
