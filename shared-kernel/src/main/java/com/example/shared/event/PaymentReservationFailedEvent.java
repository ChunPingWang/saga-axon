package com.example.shared.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when payment reservation fails.
 */
public record PaymentReservationFailedEvent(
    UUID orderId,
    UUID reservationId,
    String customerId,
    BigDecimal requestedAmount,
    BigDecimal availableCredit,
    String reason,
    String message,
    Instant timestamp
) {
    public PaymentReservationFailedEvent {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (reservationId == null) {
            throw new IllegalArgumentException("Reservation ID cannot be null");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be null or blank");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason cannot be null or blank");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public PaymentReservationFailedEvent(UUID orderId, UUID reservationId, String customerId, BigDecimal requestedAmount,
            BigDecimal availableCredit, String reason, String message) {
        this(orderId, reservationId, customerId, requestedAmount, availableCredit, reason, message, Instant.now());
    }

    public enum Reason {
        INSUFFICIENT_CREDIT,
        CUSTOMER_NOT_FOUND,
        SYSTEM_ERROR
    }
}
