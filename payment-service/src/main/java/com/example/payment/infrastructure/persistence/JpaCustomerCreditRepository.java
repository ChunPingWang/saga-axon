package com.example.payment.infrastructure.persistence;

import com.example.payment.domain.entity.CustomerCredit;
import com.example.payment.domain.repository.CustomerCreditRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA implementation of CustomerCreditRepository.
 */
@Repository
public interface JpaCustomerCreditRepository extends JpaRepository<CustomerCredit, String>, CustomerCreditRepository {

    @Override
    Optional<CustomerCredit> findByCustomerId(String customerId);

    @Override
    default boolean existsByCustomerId(String customerId) {
        return existsById(customerId);
    }
}
