package com.example.shared.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.UUID;

/**
 * Command to release an inventory reservation (compensation).
 */
public record ReleaseInventoryCommand(
    @TargetAggregateIdentifier UUID reservationId,
    UUID orderId,
    String reason
) {
    public ReleaseInventoryCommand {
        if (reservationId == null) {
            throw new IllegalArgumentException("Reservation ID cannot be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason cannot be null or blank");
        }
    }
}
