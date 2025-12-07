package com.example.payment.domain.repository;

import com.example.payment.domain.entity.CustomerCredit;

import java.util.Optional;

/**
 * Repository interface for CustomerCredit entity.
 * Domain layer interface - implementation is in infrastructure layer.
 */
public interface CustomerCreditRepository {

    /**
     * Find a customer's credit by customer ID.
     */
    Optional<CustomerCredit> findByCustomerId(String customerId);

    /**
     * Save a customer's credit.
     */
    CustomerCredit save(CustomerCredit customerCredit);

    /**
     * Check if a customer exists.
     */
    boolean existsByCustomerId(String customerId);
}
