package com.example.inventory.infrastructure.persistence;

import com.example.inventory.domain.entity.Product;
import com.example.inventory.domain.repository.ProductRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA implementation of ProductRepository.
 */
@Repository
public interface JpaProductRepository extends JpaRepository<Product, String>, ProductRepository {

    @Override
    Optional<Product> findByProductId(String productId);

    @Override
    default boolean existsByProductId(String productId) {
        return existsById(productId);
    }
}
