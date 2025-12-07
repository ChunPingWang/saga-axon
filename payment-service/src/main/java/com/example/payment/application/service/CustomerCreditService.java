package com.example.payment.application.service;

import com.example.payment.domain.entity.CustomerCredit;
import com.example.payment.domain.repository.CustomerCreditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Application service for customer credit operations.
 */
@Service
@Transactional
public class CustomerCreditService {

    private static final Logger log = LoggerFactory.getLogger(CustomerCreditService.class);

    private final CustomerCreditRepository creditRepository;

    public CustomerCreditService(CustomerCreditRepository creditRepository) {
        this.creditRepository = creditRepository;
    }

    /**
     * Get customer credit by ID.
     */
    public Optional<CustomerCredit> getCustomerCredit(String customerId) {
        return creditRepository.findByCustomerId(customerId);
    }

    /**
     * Check if customer has sufficient credit.
     */
    public boolean hasSufficientCredit(String customerId, BigDecimal amount) {
        return creditRepository.findByCustomerId(customerId)
            .map(credit -> credit.hasAvailableCredit(amount))
            .orElse(false);
    }

    /**
     * Reserve credit for a customer.
     */
    public void reserveCredit(String customerId, BigDecimal amount) {
        CustomerCredit credit = creditRepository.findByCustomerId(customerId)
            .orElseThrow(() -> new IllegalStateException("Customer not found: " + customerId));

        credit.reserveCredit(amount);
        creditRepository.save(credit);
        log.info("Reserved {} credit for customer {}", amount, customerId);
    }

    /**
     * Confirm a credit reservation.
     */
    public void confirmReservation(String customerId, BigDecimal amount) {
        CustomerCredit credit = creditRepository.findByCustomerId(customerId)
            .orElseThrow(() -> new IllegalStateException("Customer not found: " + customerId));

        credit.confirmReservation(amount);
        creditRepository.save(credit);
        log.info("Confirmed {} credit reservation for customer {}", amount, customerId);
    }

    /**
     * Release a credit reservation.
     */
    public void releaseReservation(String customerId, BigDecimal amount) {
        CustomerCredit credit = creditRepository.findByCustomerId(customerId)
            .orElseThrow(() -> new IllegalStateException("Customer not found: " + customerId));

        credit.releaseReservation(amount);
        creditRepository.save(credit);
        log.info("Released {} credit reservation for customer {}", amount, customerId);
    }
}
