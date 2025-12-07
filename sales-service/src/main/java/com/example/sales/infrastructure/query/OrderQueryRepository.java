package com.example.sales.infrastructure.query;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for OrderQueryModel (read model).
 */
@Repository
public interface OrderQueryRepository extends JpaRepository<OrderQueryModel, UUID> {
}
