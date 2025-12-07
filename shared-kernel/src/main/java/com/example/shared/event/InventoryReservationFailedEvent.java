package com.example.shared.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when inventory reservation fails.
 */
public record InventoryReservationFailedEvent(
    UUID orderId,
    UUID reservationId,
    String productId,
    int requestedQuantity,
    int availableStock,
    String reason,
    String message,
    Instant timestamp
) {
    public InventoryReservationFailedEvent {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (reservationId == null) {
            throw new IllegalArgumentException("Reservation ID cannot be null");
        }
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Product ID cannot be null or blank");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason cannot be null or blank");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public InventoryReservationFailedEvent(UUID orderId, UUID reservationId, String productId, int requestedQuantity,
            int availableStock, String reason, String message) {
        this(orderId, reservationId, productId, requestedQuantity, availableStock, reason, message, Instant.now());
    }

    public enum Reason {
        OUT_OF_STOCK,
        PRODUCT_NOT_FOUND,
        SYSTEM_ERROR
    }
}
