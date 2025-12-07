package com.example.shared.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when payment is confirmed.
 */
public record PaymentConfirmedEvent(
    UUID orderId,
    UUID reservationId,
    BigDecimal confirmedAmount,
    Instant timestamp
) {
    public PaymentConfirmedEvent {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (reservationId == null) {
            throw new IllegalArgumentException("Reservation ID cannot be null");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public PaymentConfirmedEvent(UUID orderId, UUID reservationId, BigDecimal confirmedAmount) {
        this(orderId, reservationId, confirmedAmount, Instant.now());
    }
}
