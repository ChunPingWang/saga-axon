package com.example.shared.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when an order is confirmed.
 */
public record OrderConfirmedEvent(
    UUID orderId,
    Instant timestamp
) {
    public OrderConfirmedEvent {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public OrderConfirmedEvent(UUID orderId) {
        this(orderId, Instant.now());
    }
}
