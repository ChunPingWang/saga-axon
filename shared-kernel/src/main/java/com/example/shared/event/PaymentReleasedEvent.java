package com.example.shared.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when payment reservation is released (compensated).
 */
public record PaymentReleasedEvent(
    UUID orderId,
    UUID reservationId,
    BigDecimal releasedAmount,
    String reason,
    Instant timestamp
) {
    public PaymentReleasedEvent {
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

    public PaymentReleasedEvent(UUID orderId, UUID reservationId, BigDecimal releasedAmount, String reason) {
        this(orderId, reservationId, releasedAmount, reason, Instant.now());
    }

    public enum Reason {
        ORDER_CANCELLED,
        INVENTORY_FAILED,
        TIMEOUT,
        COMPENSATION
    }
}
