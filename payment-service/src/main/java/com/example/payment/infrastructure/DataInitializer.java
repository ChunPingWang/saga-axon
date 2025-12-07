package com.example.payment.infrastructure;

import com.example.payment.domain.entity.CustomerCredit;
import com.example.payment.domain.repository.CustomerCreditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Initializes test data for the payment service.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CustomerCreditRepository creditRepository;

    public DataInitializer(CustomerCreditRepository creditRepository) {
        this.creditRepository = creditRepository;
    }

    @Override
    public void run(String... args) {
        // Initialize customer credits for testing
        initializeCustomerCredits();
    }

    private void initializeCustomerCredits() {
        // CUST-001: Has enough credit (50,000 TWD)
        if (!creditRepository.existsByCustomerId("CUST-001")) {
            CustomerCredit cust001 = new CustomerCredit("CUST-001", new BigDecimal("50000"));
            creditRepository.save(cust001);
            log.info("Created customer credit for CUST-001 with limit 50,000 TWD");
        }

        // CUST-002: Has insufficient credit (10,000 TWD - less than iPhone price 35,000)
        if (!creditRepository.existsByCustomerId("CUST-002")) {
            CustomerCredit cust002 = new CustomerCredit("CUST-002", new BigDecimal("10000"));
            creditRepository.save(cust002);
            log.info("Created customer credit for CUST-002 with limit 10,000 TWD");
        }

        // CUST-003: Has exactly enough credit (35,000 TWD)
        if (!creditRepository.existsByCustomerId("CUST-003")) {
            CustomerCredit cust003 = new CustomerCredit("CUST-003", new BigDecimal("35000"));
            creditRepository.save(cust003);
            log.info("Created customer credit for CUST-003 with limit 35,000 TWD");
        }
    }
}
