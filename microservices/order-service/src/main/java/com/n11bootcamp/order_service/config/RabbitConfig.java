package com.n11bootcamp.order_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitConfig.class);

    // === MEVCUT ORDER → KENDİ EXCHANGE / DLX AYARLARIN ===
    @Value("${rabbitmq.exchange}")
    private String ordersExchangeName;

    @Value("${rabbitmq.queue}")
    private String orderCreatedQueueName;

    @Value("${rabbitmq.routingkey}")
    private String orderCreatedRoutingKey;

    @Value("${rabbitmq.dlx.exchange}")
    private String dlxExchangeName;

    @Value("${rabbitmq.dlq.queue}")
    private String dlqQueueName;

    @Value("${rabbitmq.dlq.routingkey}")
    private String dlqRoutingKey;

    // === SAGA / STOCK-SERVICE İLE ORTAK EXCHANGE & QUEUE AYARLARI ===
    @Value("${stock.rabbit.exchange}")
    private String stockExchangeName;

    @Value("${stock.rabbit.reservedRoutingKey}")
    private String stockReservedRoutingKey;

    @Value("${stock.rabbit.rejectedRoutingKey}")
    private String stockRejectedRoutingKey;

    @Value("${order.rabbit.stockReservedQueue}")
    private String orderStockReservedQueueName;

    @Value("${order.rabbit.stockRejectedQueue}")
    private String orderStockRejectedQueueName;

    // ================== ORDER KENDİ EXCHANGE + DLX ==================

    // Main exchange (orders.exchange)
    @Bean
    public TopicExchange ordersExchange() {
        return ExchangeBuilder.topicExchange(ordersExchangeName)
                .durable(true)
                .build();
    }

    // DLX (orders.dlx.exchange)
    @Bean
    public TopicExchange dlxExchange() {
        return ExchangeBuilder.topicExchange(dlxExchangeName)
                .durable(true)
                .build();
    }

    // Main queue (order.created.queue) + DLX yönlendirme
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(orderCreatedQueueName)
                .withArgument("x-dead-letter-exchange", dlxExchangeName)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
    }

    // Dead-letter queue
    @Bean
    public Queue orderCreatedDlq() {
        return QueueBuilder.durable(dlqQueueName).build();
    }

    // Main queue binding
    @Bean
    public Binding bindingOrderCreated() {
        return BindingBuilder.bind(orderCreatedQueue())
                .to(ordersExchange())
                .with(orderCreatedRoutingKey);
    }

    // DLQ binding
    @Bean
    public Binding bindingDlq() {
        return BindingBuilder.bind(orderCreatedDlq())
                .to(dlxExchange())
                .with(dlqRoutingKey);
    }

    // ================== SAGA: STOCK EXCHANGE ÜZERİNDEN QUEUE’LER ==================

    /**
     * Stock-service ile paylaşılan exchange:
     * stock.rabbit.exchange = stock.events.exchange
     */
    @Bean
    public TopicExchange stockEventsExchange() {
        return new TopicExchange(stockExchangeName, true, false);
    }

    /**
     * Stock → Order (stock reserved) kuyruğu:
     * order.rabbit.stockReservedQueue = order.stock.reserved.queue
     */
    @Bean
    public Queue orderStockReservedQueue() {
        return QueueBuilder.durable(orderStockReservedQueueName).build();
    }

    /**
     * Stock → Order (stock rejected) kuyruğu:
     * order.rabbit.stockRejectedQueue = order.stock.rejected.queue
     */
    @Bean
    public Queue orderStockRejectedQueue() {
        return QueueBuilder.durable(orderStockRejectedQueueName).build();
    }

    /**
     * Binding: stock.events.exchange + routingKey = order.stock.reserved
     *  → order.stock.reserved.queue
     */
    @Bean
    public Binding orderStockReservedBinding(Queue orderStockReservedQueue,
                                             TopicExchange stockEventsExchange) {
        return BindingBuilder.bind(orderStockReservedQueue)
                .to(stockEventsExchange)
                .with(stockReservedRoutingKey);
    }

    /**
     * Binding: stock.events.exchange + routingKey = order.stock.rejected
     *  → order.stock.rejected.queue
     */
    @Bean
    public Binding orderStockRejectedBinding(Queue orderStockRejectedQueue,
                                             TopicExchange stockEventsExchange) {
        return BindingBuilder.bind(orderStockRejectedQueue)
                .to(stockEventsExchange)
                .with(stockRejectedRoutingKey);
    }

    // ================== JSON MESSAGE CONVERTER ==================

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ================== RabbitTemplate (confirms + returns + JSON) ==================

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jacksonMessageConverter) {
        if (connectionFactory instanceof CachingConnectionFactory ccf) {
            ccf.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
            ccf.setPublisherReturns(true);
        }

        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);
        template.setMessageConverter(jacksonMessageConverter); // 🔴 JSON gönder/gelsin

        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                LOGGER.info("Message confirmed. correlationData={}", correlationData);
            } else {
                LOGGER.warn("Mesaj onaylanmadı: cause={}", cause);
            }
        });

        template.setReturnsCallback(returned -> {
            LOGGER.warn(
                    "Message returned. replyCode={}, replyText={}, exchange={}, routingKey={}, messageProperties={}",
                    returned.getReplyCode(),
                    returned.getReplyText(),
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getMessage().getMessageProperties()
            );
        });

        return template;
    }

    // ================== @RabbitListener'lar için JSON factory ==================

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jacksonMessageConverter) {

        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jacksonMessageConverter); // 🔴 listener'lar da JSON okusun
        return factory;
    }
}