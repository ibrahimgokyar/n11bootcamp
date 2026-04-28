package com.n11bootcamp.stock_service.service;


import com.n11bootcamp.stock_service.dto.EventPayloads;
import com.n11bootcamp.stock_service.dto.StockUpdateRequest;
import com.n11bootcamp.stock_service.dto.StockUpdateResponse;
import jakarta.transaction.Transactional;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class StockSagaHandler {

    private final StockDomainService stock;
    private final RabbitTemplate rabbit;

    @Value("${stock.rabbit.exchange}")
    private String exchange;

    @Value("${stock.rabbit.reservedRoutingKey}")
    private String reservedRoutingKey;

    @Value("${stock.rabbit.rejectedRoutingKey}")
    private String rejectedRoutingKey;

    public StockSagaHandler(StockDomainService stock, RabbitTemplate rabbit) {
        this.stock = stock;
        this.rabbit = rabbit;
    }

    @Transactional
    @RabbitListener(queues = "${stock.rabbit.reserveRequestedQueue}")
    public void handleReserveRequested(EventPayloads.StockReserveRequestedEvent event) {
        StockUpdateRequest req = new StockUpdateRequest(
                event.getItems().stream()
                        .map(i -> new StockUpdateRequest.StockItem(i.getProductId(), i.getQuantity()))
                        .collect(Collectors.toList())
        );

        //StockUpdateResponse resp = stock.decrease(req);
        StockUpdateResponse resp = stock.reserve(req);

        if (resp.isSuccess()) {
            EventPayloads.StockReservedEvent reserved =
                    new EventPayloads.StockReservedEvent(event.getOrderId(), event.getUsername(), "Stock reserved");
            rabbit.convertAndSend(exchange, reservedRoutingKey, reserved);
        } else {
            EventPayloads.StockRejectedEvent rejected =
                    new EventPayloads.StockRejectedEvent(event.getOrderId(), event.getUsername(), resp.getMessage());
            rabbit.convertAndSend(exchange, rejectedRoutingKey, rejected);
        }
    }
}
