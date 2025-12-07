package com.example.shared.valueobject;

/**
 * Order status enum representing the lifecycle states of an order.
 */
public enum OrderStatus {
    /**
     * Order has been created and is awaiting processing.
     */
    PENDING,

    /**
     * Order is being processed (payment and inventory reservations in progress).
     */
    PROCESSING,

    /**
     * Order has been confirmed (payment collected, inventory reserved).
     */
    CONFIRMED,

    /**
     * Order has been cancelled due to payment or inventory failure.
     */
    CANCELLED,

    /**
     * Order has been cancelled due to timeout.
     */
    CANCELLED_TIMEOUT;

    /**
     * Check if the status is a terminal state.
     */
    public boolean isTerminal() {
        return this == CONFIRMED || this == CANCELLED || this == CANCELLED_TIMEOUT;
    }

    /**
     * Check if the order can transition to the given status.
     */
    public boolean canTransitionTo(OrderStatus target) {
        if (this.isTerminal()) {
            return false;
        }
        return switch (this) {
            case PENDING -> target == PROCESSING || target == CANCELLED || target == CANCELLED_TIMEOUT;
            case PROCESSING -> target == CONFIRMED || target == CANCELLED || target == CANCELLED_TIMEOUT;
            default -> false;
        };
    }
}
