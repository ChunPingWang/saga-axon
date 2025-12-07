package com.example.shared.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when inventory reservation is confirmed.
 */
public record InventoryConfirmedEvent(
    UUID orderId,
    UUID reservationId,
    String productId,
    int confirmedQuantity,
    Instant timestamp
) {
    public InventoryConfirmedEvent {
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

    public InventoryConfirmedEvent(UUID orderId, UUID reservationId, String productId, int confirmedQuantity) {
        this(orderId, reservationId, productId, confirmedQuantity, Instant.now());
    }
}
