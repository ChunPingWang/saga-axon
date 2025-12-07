package com.example.shared.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when payment is successfully reserved.
 */
public record PaymentReservedEvent(
    UUID orderId,
    UUID reservationId,
    String customerId,
    BigDecimal amount,
    Instant expiresAt,
    Instant timestamp
) {
    public PaymentReservedEvent {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (reservationId == null) {
            throw new IllegalArgumentException("Reservation ID cannot be null");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be null or blank");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public PaymentReservedEvent(UUID orderId, UUID reservationId, String customerId, BigDecimal amount, Instant expiresAt) {
        this(orderId, reservationId, customerId, amount, expiresAt, Instant.now());
    }
}
