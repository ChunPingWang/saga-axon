package com.example.shared.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a new order is created.
 */
public record OrderCreatedEvent(
    UUID orderId,
    String customerId,
    String productId,
    int quantity,
    BigDecimal amount,
    Instant timestamp
) {
    public OrderCreatedEvent {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be null or blank");
        }
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Product ID cannot be null or blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public OrderCreatedEvent(UUID orderId, String customerId, String productId, int quantity, BigDecimal amount) {
        this(orderId, customerId, productId, quantity, amount, Instant.now());
    }
}
