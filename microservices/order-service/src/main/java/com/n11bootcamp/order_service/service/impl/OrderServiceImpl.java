package com.n11bootcamp.order_service.service.impl;
import com.n11bootcamp.order_service.dto.CreateOrderRequest;
import com.n11bootcamp.order_service.dto.OrderResponse;
import com.n11bootcamp.order_service.dto.payment.PaymentRequest;
import com.n11bootcamp.order_service.dto.stock.StockReserveRequestedEvent;
import com.n11bootcamp.order_service.entity.Order;
import com.n11bootcamp.order_service.entity.OrderDetails;
import com.n11bootcamp.order_service.entity.OrderItem;
import com.n11bootcamp.order_service.entity.OrderStatus;
import com.n11bootcamp.order_service.event.OrderCreatedEvent;
import com.n11bootcamp.order_service.repository.OrderRepository;
import com.n11bootcamp.order_service.saga.PaymentCardStore;
import com.n11bootcamp.order_service.service.OrderService;
import com.n11bootcamp.order_service.service.PaymentServiceClient;
import com.n11bootcamp.order_service.service.StockServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final PaymentServiceClient paymentServiceClient; // şu an createOrder içinde kullanılmıyor ama dursun
    private final StockServiceClient stockServiceClient;     // aynı şekilde
    private final ApplicationEventPublisher publisher;
    private final RabbitTemplate rabbitTemplate;
    private final PaymentCardStore paymentCardStore;

    // stock-service ile ortak exchange
    @Value("${stock.rabbit.exchange}")
    private String stockExchange;

    // Order -> Stock: rezerv isteği için routing key
    @Value("${stock.rabbit.reserveRequestedRoutingKey}")
    private String stockReserveRequestedRoutingKey;

    public OrderServiceImpl(OrderRepository orderRepository,
                            PaymentServiceClient paymentServiceClient,
                            StockServiceClient stockServiceClient,
                            ApplicationEventPublisher publisher,
                            RabbitTemplate rabbitTemplate,
                            PaymentCardStore paymentCardStore) {
        this.orderRepository = orderRepository;
        this.paymentServiceClient = paymentServiceClient;
        this.stockServiceClient = stockServiceClient;
        this.publisher = publisher;
        this.rabbitTemplate = rabbitTemplate;
        this.paymentCardStore = paymentCardStore;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        // 1️⃣ Order entity oluştur → CREATED
        Order order = new Order();
        order.setUsername(request.getUsername());
        order.setStatus(OrderStatus.CREATED);
        order.setTotalPrice(
                request.getItems().stream()
                        .mapToDouble(i -> i.getPrice() * i.getQuantity())
                        .sum()
        );

        // 1.1️⃣ OrderItem mapping
        List<OrderItem> items = request.getItems().stream().map(dto -> {
            OrderItem item = new OrderItem();
            item.setProductId(dto.getProductId());
            item.setProductName(dto.getProductName());
            item.setPrice(dto.getPrice());
            item.setQuantity(dto.getQuantity());
            item.setOrder(order);
            return item;
        }).collect(Collectors.toList());
        order.setItems(items);

        // 1.2️⃣ OrderDetails mapping (Checkout bilgileri)
        OrderDetails details = new OrderDetails();
        details.setFirstName(request.getFirstName());
        details.setLastName(request.getLastName());
        details.setStreetAddress(request.getStreetAddress());
        details.setCity(request.getCity());
        details.setCountry(request.getCountry());
        details.setPhone(request.getPhone());
        details.setEmail(request.getEmail());
        order.setOrderDetails(details);

        // 1.3️⃣ Order DB'ye CREATED olarak kaydet
        Order savedOrder = orderRepository.save(order);
        LOGGER.info("Order CREATED kaydedildi. orderId={}, username={}, totalPrice={}",
                savedOrder.getId(), savedOrder.getUsername(), savedOrder.getTotalPrice());

        // 1.4️⃣ Kart bilgisini RAM üzerinde saga için sakla (DB'ye yazmıyoruz!)
        if (request.getCard() != null) {
            PaymentRequest.Card cardForStore = new PaymentRequest.Card();
            cardForStore.setCardHolderName(request.getCard().getCardHolderName());
            cardForStore.setCardNumber(request.getCard().getCardNumber());
            cardForStore.setExpireMonth(request.getCard().getExpireMonth());
            cardForStore.setExpireYear(request.getCard().getExpireYear());
            cardForStore.setCvc(request.getCard().getCvc());

            paymentCardStore.put(savedOrder.getId(), cardForStore);
            LOGGER.info("Kart bilgisi RAM store'a kaydedildi. orderId={}", savedOrder.getId());
        } else {
            LOGGER.warn("CreateOrderRequest.card null → ödeme sırasında kart bulunamayabilir. orderId={}",
                    savedOrder.getId());
        }

        // 2️⃣ Saga başlangıcı: StockReserveRequestedEvent yayınla
        StockReserveRequestedEvent eventPayload = new StockReserveRequestedEvent();
        eventPayload.setOrderId(savedOrder.getId());
        eventPayload.setUsername(savedOrder.getUsername());

        List<StockReserveRequestedEvent.Item> evItems = new ArrayList<>();
        for (OrderItem it : savedOrder.getItems()) {
            evItems.add(new StockReserveRequestedEvent.Item(
                    it.getProductId(),
                    it.getQuantity()
            ));
        }
        eventPayload.setItems(evItems);

        LOGGER.info("StockReserveRequestedEvent publish ediliyor. exchange={}, routingKey={}, payload={}",
                stockExchange, stockReserveRequestedRoutingKey, eventPayload);

        // RabbitMQ'ya gönder
        rabbitTemplate.convertAndSend(
                stockExchange,                  // stock.events.exchange
                stockReserveRequestedRoutingKey,// order.stock.reserve.requested
                eventPayload
        );

        // 2.1️⃣ (İsteğe bağlı) Spring içi event publish (senin eski yapın)
        OrderCreatedEvent springEvent = new OrderCreatedEvent();
        springEvent.setOrderId(savedOrder.getId());
        springEvent.setUsername(savedOrder.getUsername());
        springEvent.setTotalPrice(savedOrder.getTotalPrice());
        springEvent.setItems(savedOrder.getItems().stream().map(item -> {
            OrderCreatedEvent.OrderItem oi = new OrderCreatedEvent.OrderItem();
            oi.setProductId(item.getProductId());
            oi.setProductName(item.getProductName());
            oi.setPrice(item.getPrice());
            oi.setQuantity(item.getQuantity());
            return oi;
        }).collect(Collectors.toList()));
        publisher.publishEvent(springEvent);

        // 3️⃣ Şu anda CREATED durumunu döneriz.
        OrderResponse response = new OrderResponse();
        response.setOrderId(savedOrder.getId());
        response.setUsername(savedOrder.getUsername());
        response.setStatus(savedOrder.getStatus().name()); // CREATED
        response.setTotalPrice(savedOrder.getTotalPrice());
        response.setItems(
                savedOrder.getItems().stream().map(item -> {
                    OrderResponse.OrderItemResponse i = new OrderResponse.OrderItemResponse();
                    i.setProductId(item.getProductId());
                    i.setProductName(item.getProductName());
                    i.setPrice(item.getPrice());
                    i.setQuantity(item.getQuantity());
                    return i;
                }).collect(Collectors.toList())
        );

        return response;
    }

    @Override
    public List<OrderResponse> findAllOrders() {
        return orderRepository.findAll().stream().map(order -> {
            OrderResponse response = new OrderResponse();
            response.setOrderId(order.getId());
            response.setUsername(order.getUsername());
            response.setStatus(order.getStatus().name());
            response.setTotalPrice(order.getTotalPrice());
            response.setItems(
                    order.getItems().stream().map(item -> {
                        OrderResponse.OrderItemResponse i = new OrderResponse.OrderItemResponse();
                        i.setProductId(item.getProductId());
                        i.setProductName(item.getProductName());
                        i.setPrice(item.getPrice());
                        i.setQuantity(item.getQuantity());
                        return i;
                    }).collect(Collectors.toList())
            );
            return response;
        }).collect(Collectors.toList());
    }

    @Override
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getId());
        response.setUsername(order.getUsername());
        response.setStatus(order.getStatus().name());
        response.setTotalPrice(order.getTotalPrice());
        response.setItems(
                order.getItems().stream().map(item -> {
                    OrderResponse.OrderItemResponse i = new OrderResponse.OrderItemResponse();
                    i.setProductId(item.getProductId());
                    i.setProductName(item.getProductName());
                    i.setPrice(item.getPrice());
                    i.setQuantity(item.getQuantity());
                    return i;
                }).collect(Collectors.toList())
        );
        return response;
    }

    @Override
    public List<OrderResponse> findOrdersByUsername(String username) {
        List<Order> orders;
        try {
            orders = orderRepository.findByUsername(username);
        } catch (Throwable t) {
            orders = orderRepository.findAll().stream()
                    .filter(o -> username != null && username.equals(o.getUsername()))
                    .collect(Collectors.toList());
        }

        return orders.stream().map(order -> {
            OrderResponse response = new OrderResponse();
            response.setOrderId(order.getId());
            response.setUsername(order.getUsername());
            response.setStatus(order.getStatus().name());
            response.setTotalPrice(order.getTotalPrice());
            response.setItems(
                    order.getItems().stream().map(item -> {
                        OrderResponse.OrderItemResponse i = new OrderResponse.OrderItemResponse();
                        i.setProductId(item.getProductId());
                        i.setProductName(item.getProductName());
                        i.setPrice(item.getPrice());
                        i.setQuantity(item.getQuantity());
                        return i;
                    }).collect(Collectors.toList())
            );
            return response;
        }).collect(Collectors.toList());
    }
}
