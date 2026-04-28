package com.n11bootcamp.order_service.saga;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.n11bootcamp.order_service.dto.payment.PaymentRequest;
import com.n11bootcamp.order_service.dto.payment.PaymentResponse;
import com.n11bootcamp.order_service.dto.stock.StockUpdateRequest;
import com.n11bootcamp.order_service.entity.Order;
import com.n11bootcamp.order_service.entity.OrderDetails;
import com.n11bootcamp.order_service.entity.OrderItem;
import com.n11bootcamp.order_service.entity.OrderStatus;
import com.n11bootcamp.order_service.repository.OrderRepository;
import com.n11bootcamp.order_service.service.PaymentServiceClient;
import com.n11bootcamp.order_service.service.StockServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
public class OrderSagaListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderSagaListener.class);

    private final OrderRepository orderRepository;
    private final PaymentServiceClient paymentServiceClient;
    private final StockServiceClient stockServiceClient;
    private final PaymentCardStore paymentCardStore;

    public OrderSagaListener(OrderRepository orderRepository,
                             PaymentServiceClient paymentServiceClient,
                             StockServiceClient stockServiceClient,
                             PaymentCardStore paymentCardStore) {
        this.orderRepository = orderRepository;
        this.paymentServiceClient = paymentServiceClient;
        this.stockServiceClient = stockServiceClient;
        this.paymentCardStore = paymentCardStore;
    }

    // ================== STOCK RESERVED EVENT ==================

    /**
     * Stock-service "stok rezerve edildi" event'i gönderdiğinde burası çalışır.
     *
     * stock.rabbit.reservedRoutingKey = order.stock.reserved
     * order.rabbit.stockReservedQueue  = order.stock.reserved.queue
     */
    @Transactional
    @RabbitListener(queues = "${order.rabbit.stockReservedQueue}")
    public void onStockReserved(StockReservedEvent event) {
        LOGGER.info("[SAGA] StockReservedEvent alındı: orderId={}, username={}, message={}",
                event.getOrderId(), event.getUsername(), event.getMessage());

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            LOGGER.warn("[SAGA] Order zaten son durumda, status={}, orderId={}",
                    order.getStatus(), order.getId());
            return;
        }

        // 1) ORDER → STOCK_DEDUCTED
        order.setStatus(OrderStatus.STOCK_DEDUCTED);
        orderRepository.save(order);

        // 2) Payment isteği hazırla
        OrderDetails details = order.getOrderDetails();
        PaymentRequest pr = new PaymentRequest();
        pr.setOrderId(order.getId());
        pr.setUsername(order.getUsername());
        if (details != null) {
            pr.setFirstName(details.getFirstName());
            pr.setLastName(details.getLastName());
            pr.setStreetAddress(details.getStreetAddress());
            pr.setAddress(details.getStreetAddress());
            pr.setEmail(details.getEmail());
        }
        pr.setAmount(order.getTotalPrice());
        pr.setPaymentMethod("IYZICO"); // istersen Order'a paymentMethod kolonu ekleyip oradan da çekebilirsin

        // 🔹 Kart bilgisini RAM store'dan al
        PaymentRequest.Card storedCard = paymentCardStore.take(order.getId());
        if (storedCard == null) {
            LOGGER.warn("[SAGA] Payment için kart bulunamadı (RAM store boş). orderId={}", order.getId());
            markOrderCancelledAndCompensateStock(order);
            return;
        }
        pr.setCard(storedCard);

        // Items → payment request
        List<PaymentRequest.Item> payItems = new ArrayList<>();
        for (OrderItem it : order.getItems()) {
            PaymentRequest.Item pi = new PaymentRequest.Item();
            pi.setProductId(it.getProductId());
            pi.setProductName(it.getProductName());
            pi.setPrice(it.getPrice());
            pi.setQuantity(it.getQuantity());
            payItems.add(pi);
        }
        pr.setItems(payItems);

        LOGGER.info("[SAGA] Payment isteği gönderiliyor: orderId={}, amount={}",
                order.getId(), order.getTotalPrice());

        PaymentResponse resp;
        try {
            resp = paymentServiceClient.makePayment(pr);
        } catch (Exception ex) {
            LOGGER.error("[SAGA] Payment servis çağrısı hata verdi, orderId={}", order.getId(), ex);
            markOrderCancelledAndCompensateStock(order);
            return;
        }

        if (resp == null || !resp.isSuccess()) {
            LOGGER.warn("[SAGA] Payment başarısız. orderId={}, message={}",
                    order.getId(), resp != null ? resp.getMessage() : "null response");
            markOrderCancelledAndCompensateStock(order);
            return;
        }

        // 3) Payment başarılı → ORDER: PAID → COMPLETED
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);

        LOGGER.info("[SAGA] Order COMPLETED: orderId={}", order.getId());
    }

    // ================== STOCK REJECTED EVENT ==================

    /**
     * Stock-service "stok rezerve edilemedi" event'i gönderdiğinde burası çalışır.
     *
     * stock.rabbit.rejectedRoutingKey = order.stock.rejected
     * order.rabbit.stockRejectedQueue  = order.stock.rejected.queue
     */
    @Transactional
    @RabbitListener(queues = "${order.rabbit.stockRejectedQueue}")
    public void onStockRejected(StockRejectedEvent event) {
        LOGGER.info("[SAGA] StockRejectedEvent alındı: orderId={}, username={}, message={}",
                event.getOrderId(), event.getUsername(), event.getMessage());

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        LOGGER.info("[SAGA] Order CANCELLED (stock rejected): orderId={}", order.getId());
    }

    // ================== YARDIMCI: CANCEL + STOCK TELAFİ ==================

    private void markOrderCancelledAndCompensateStock(Order order) {
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        try {
            StockUpdateRequest req = new StockUpdateRequest();
            List<StockUpdateRequest.StockItem> items = new ArrayList<>();
            for (OrderItem it : order.getItems()) {
                StockUpdateRequest.StockItem si = new StockUpdateRequest.StockItem();
                si.setProductId(it.getProductId());
                si.setQuantity(it.getQuantity());
                items.add(si);
            }
            req.setItems(items);

            LOGGER.info("[SAGA] Stok telafisi (increaseStock) çağrılıyor. orderId={}", order.getId());
           // stockServiceClient.increaseStock(req);
            stockServiceClient.releaseStock(req);
        } catch (Exception ex) {
            LOGGER.error("[SAGA] Stok telafisi başarısız. orderId={}", order.getId(), ex);
        }
    }

    // ================== EVENT DTO’LARI (stock-service ile UYUMLU) ==================

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StockReservedEvent {
        private Long orderId;
        private String username;
        private String message;

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StockRejectedEvent {
        private Long orderId;
        private String username;
        private String message;

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
