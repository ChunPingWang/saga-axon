package com.example.shared.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.UUID;

/**
 * Command to reserve inventory for an order.
 */
public record ReserveInventoryCommand(
    @TargetAggregateIdentifier UUID reservationId,
    UUID orderId,
    String productId,
    int quantity
) {
    public ReserveInventoryCommand {
        if (reservationId == null) {
            throw new IllegalArgumentException("Reservation ID cannot be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Product ID cannot be null or blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }
}
