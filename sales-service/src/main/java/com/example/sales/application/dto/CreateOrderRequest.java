package com.example.sales.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for creating a new order.
 */
public record CreateOrderRequest(
    @NotBlank(message = "Customer ID is required")
    String customerId,

    @NotBlank(message = "Product ID is required")
    String productId,

    @Positive(message = "Quantity must be positive")
    Integer quantity
) {
    public CreateOrderRequest {
        // Default quantity to 1 for MVP
        if (quantity == null) {
            quantity = 1;
        }
    }
}
