package com.example.shared.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when an order is cancelled.
 */
public record OrderCancelledEvent(
    UUID orderId,
    String reason,
    boolean isTimeout,
    Instant timestamp
) {
    public OrderCancelledEvent {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason cannot be null or blank");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public OrderCancelledEvent(UUID orderId, String reason, boolean isTimeout) {
        this(orderId, reason, isTimeout, Instant.now());
    }

    public static OrderCancelledEvent forTimeout(UUID orderId) {
        return new OrderCancelledEvent(orderId, "Operation timed out", true);
    }
}
