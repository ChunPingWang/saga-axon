package com.example.shared.valueobject;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object representing an item in an order.
 * Immutable and self-validating.
 */
public record OrderItem(String productId, int quantity, Money unitPrice) {

    public OrderItem {
        Objects.requireNonNull(productId, "Product ID cannot be null");
        if (productId.isBlank()) {
            throw new IllegalArgumentException("Product ID cannot be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        Objects.requireNonNull(unitPrice, "Unit price cannot be null");
    }

    public Money totalPrice() {
        return new Money(
            unitPrice.amount().multiply(BigDecimal.valueOf(quantity)),
            unitPrice.currency()
        );
    }
}
