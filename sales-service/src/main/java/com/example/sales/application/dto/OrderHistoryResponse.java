package com.example.sales.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for order history.
 */
public record OrderHistoryResponse(
    UUID orderId,
    List<OrderEvent> events
) {
    public record OrderEvent(
        String eventType,
        Instant timestamp,
        Map<String, Object> details
    ) {}
}
