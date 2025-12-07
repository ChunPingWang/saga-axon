package com.example.sales.application.dto;

import com.example.shared.valueobject.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for order information.
 */
public record OrderResponse(
    UUID orderId,
    String customerId,
    String productId,
    int quantity,
    BigDecimal amount,
    OrderStatus status,
    String statusMessage,
    Instant createdAt,
    Instant updatedAt
) {
    public static OrderResponse accepted(UUID orderId, Instant createdAt) {
        return new OrderResponse(
            orderId,
            null,
            null,
            0,
            null,
            OrderStatus.PENDING,
            "訂單已受理，正在處理中",
            createdAt,
            createdAt
        );
    }
}
