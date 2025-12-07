package com.example.shared.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.UUID;

/**
 * Command to cancel an order.
 */
public record CancelOrderCommand(
    @TargetAggregateIdentifier UUID orderId,
    String reason,
    boolean isTimeout
) {
    public CancelOrderCommand {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason cannot be null or blank");
        }
    }

    public static CancelOrderCommand forPaymentFailure(UUID orderId, String reason) {
        return new CancelOrderCommand(orderId, reason, false);
    }

    public static CancelOrderCommand forInventoryFailure(UUID orderId, String reason) {
        return new CancelOrderCommand(orderId, reason, false);
    }

    public static CancelOrderCommand forTimeout(UUID orderId) {
        return new CancelOrderCommand(orderId, "Operation timed out", true);
    }
}
