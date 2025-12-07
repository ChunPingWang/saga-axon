package com.example.shared.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Command to create a new order.
 */
public record CreateOrderCommand(
    @TargetAggregateIdentifier UUID orderId,
    String customerId,
    String productId,
    int quantity,
    BigDecimal amount
) {
    public CreateOrderCommand {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be null or blank");
        }
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Product ID cannot be null or blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}
