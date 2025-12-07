package com.example.payment.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

/**
 * CustomerCredit entity representing a customer's credit account.
 */
@Entity
@Table(name = "customer_credits")
public class CustomerCredit {

    @Id
    @Column(name = "customer_id", nullable = false, length = 50)
    private String customerId;

    @Column(name = "credit_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "available_credit", nullable = false, precision = 19, scale = 2)
    private BigDecimal availableCredit;

    @Column(name = "reserved_credit", nullable = false, precision = 19, scale = 2)
    private BigDecimal reservedCredit;

    @Version
    private Long version;

    protected CustomerCredit() {
    }

    public CustomerCredit(String customerId, BigDecimal creditLimit) {
        this.customerId = customerId;
        this.creditLimit = creditLimit;
        this.availableCredit = creditLimit;
        this.reservedCredit = BigDecimal.ZERO;
    }

    /**
     * Check if the customer has enough available credit.
     */
    public boolean hasAvailableCredit(BigDecimal amount) {
        return availableCredit.compareTo(amount) >= 0;
    }

    /**
     * Reserve credit for a transaction.
     */
    public void reserveCredit(BigDecimal amount) {
        if (!hasAvailableCredit(amount)) {
            throw new IllegalStateException("Insufficient credit");
        }
        this.availableCredit = this.availableCredit.subtract(amount);
        this.reservedCredit = this.reservedCredit.add(amount);
    }

    /**
     * Confirm a reservation (deduct from reserved).
     */
    public void confirmReservation(BigDecimal amount) {
        if (reservedCredit.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient reserved credit");
        }
        this.reservedCredit = this.reservedCredit.subtract(amount);
    }

    /**
     * Release a reservation (return to available).
     */
    public void releaseReservation(BigDecimal amount) {
        if (reservedCredit.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient reserved credit");
        }
        this.reservedCredit = this.reservedCredit.subtract(amount);
        this.availableCredit = this.availableCredit.add(amount);
    }

    // Getters
    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public BigDecimal getAvailableCredit() {
        return availableCredit;
    }

    public BigDecimal getReservedCredit() {
        return reservedCredit;
    }

    public Long getVersion() {
        return version;
    }
}
