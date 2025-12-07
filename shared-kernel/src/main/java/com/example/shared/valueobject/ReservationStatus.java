package com.example.shared.valueobject;

/**
 * Reservation status enum representing the lifecycle states of payment/inventory reservations.
 */
public enum ReservationStatus {
    /**
     * Resources have been reserved.
     */
    RESERVED,

    /**
     * Reservation has been confirmed and resources are committed.
     */
    CONFIRMED,

    /**
     * Reservation has been released (compensated).
     */
    RELEASED,

    /**
     * Reservation has expired due to timeout.
     */
    EXPIRED;

    /**
     * Check if the status is a terminal state.
     */
    public boolean isTerminal() {
        return this == CONFIRMED || this == RELEASED || this == EXPIRED;
    }

    /**
     * Check if the reservation can transition to the given status.
     */
    public boolean canTransitionTo(ReservationStatus target) {
        if (this.isTerminal()) {
            return false;
        }
        return switch (this) {
            case RESERVED -> target == CONFIRMED || target == RELEASED || target == EXPIRED;
            default -> false;
        };
    }
}
