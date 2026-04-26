package com.n11bootcamp.order_service.saga;

import com.n11bootcamp.order_service.dto.payment.PaymentRequest;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Kart bilgilerini DB'ye yazmadan, sadece saga akışı boyunca
 * RAM üzerinde tutmak için basit bir store.
 */
@Component
public class PaymentCardStore {

    // key: orderId, value: PaymentRequest.Card
    private final ConcurrentMap<Long, PaymentRequest.Card> store = new ConcurrentHashMap<>();

    public void put(Long orderId, PaymentRequest.Card card) {
        if (orderId == null || card == null) return;

        // Güvenlik için shallow copy
        PaymentRequest.Card copy = new PaymentRequest.Card();
        copy.setCardHolderName(card.getCardHolderName());
        copy.setCardNumber(card.getCardNumber());
        copy.setExpireMonth(card.getExpireMonth());
        copy.setExpireYear(card.getExpireYear());
        copy.setCvc(card.getCvc());

        store.put(orderId, copy);
    }

    /**
     * Kartı RAM'den alır ve siler (tek seferlik kullanım).
     */
    public PaymentRequest.Card take(Long orderId) {
        if (orderId == null) return null;
        return store.remove(orderId);
    }
}
