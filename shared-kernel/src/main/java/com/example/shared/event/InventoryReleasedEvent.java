package com.example.shared.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when inventory reservation is released (compensated).
 */
public record InventoryReleasedEvent(
    UUID orderId,
    UUID reservationId,
    String productId,
    int releasedQuantity,
    String reason,
    Instant timestamp
) {
    public InventoryReleasedEvent {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (reservationId == null) {
            throw new IllegalArgumentException("Reservation ID cannot be null");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason cannot be null or blank");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public InventoryReleasedEvent(UUID orderId, UUID reservationId, String productId, int releasedQuantity, String reason) {
        this(orderId, reservationId, productId, releasedQuantity, reason, Instant.now());
    }

    public enum Reason {
        ORDER_CANCELLED,
        PAYMENT_FAILED,
        TIMEOUT,
        COMPENSATION
    }
}
