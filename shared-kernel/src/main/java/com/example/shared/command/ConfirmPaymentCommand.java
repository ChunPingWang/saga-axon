package com.example.shared.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.UUID;

/**
 * Command to confirm a payment reservation.
 */
public record ConfirmPaymentCommand(
    @TargetAggregateIdentifier UUID reservationId,
    UUID orderId
) {
    public ConfirmPaymentCommand {
        if (reservationId == null) {
            throw new IllegalArgumentException("Reservation ID cannot be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
    }
}
