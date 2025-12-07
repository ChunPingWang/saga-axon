package com.example.shared.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when inventory is successfully reserved.
 */
public record InventoryReservedEvent(
    UUID orderId,
    UUID reservationId,
    String productId,
    int quantity,
    int remainingStock,
    Instant timestamp
) {
    public InventoryReservedEvent {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (reservationId == null) {
            throw new IllegalArgumentException("Reservation ID cannot be null");
        }
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Product ID cannot be null or blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public InventoryReservedEvent(UUID orderId, UUID reservationId, String productId, int quantity, int remainingStock) {
        this(orderId, reservationId, productId, quantity, remainingStock, Instant.now());
    }
}
