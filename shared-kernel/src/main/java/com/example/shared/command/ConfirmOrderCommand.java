package com.example.shared.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.UUID;

/**
 * Command to confirm an order after successful payment and inventory reservation.
 */
public record ConfirmOrderCommand(
    @TargetAggregateIdentifier UUID orderId
) {
    public ConfirmOrderCommand {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
    }
}
