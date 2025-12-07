package com.example.inventory.domain.repository;

import com.example.inventory.domain.entity.Product;

import java.util.Optional;

/**
 * Repository interface for Product entity.
 * Domain layer interface - implementation is in infrastructure layer.
 */
public interface ProductRepository {

    /**
     * Find a product by product ID.
     */
    Optional<Product> findByProductId(String productId);

    /**
     * Save a product.
     */
    Product save(Product product);

    /**
     * Check if a product exists.
     */
    boolean existsByProductId(String productId);
}
