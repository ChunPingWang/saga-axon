package com.example.shared.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Command to reserve payment (credit) for an order.
 */
public record ReservePaymentCommand(
    @TargetAggregateIdentifier UUID reservationId,
    UUID orderId,
    String customerId,
    BigDecimal amount
) {
    public ReservePaymentCommand {
        if (reservationId == null) {
            throw new IllegalArgumentException("Reservation ID cannot be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be null or blank");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}
