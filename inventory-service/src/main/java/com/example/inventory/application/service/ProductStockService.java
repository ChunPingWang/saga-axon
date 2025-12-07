package com.example.inventory.application.service;

import com.example.inventory.domain.entity.Product;
import com.example.inventory.domain.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Application service for product stock operations.
 */
@Service
@Transactional
public class ProductStockService {

    private static final Logger log = LoggerFactory.getLogger(ProductStockService.class);

    private final ProductRepository productRepository;

    public ProductStockService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Get product by ID.
     */
    public Optional<Product> getProduct(String productId) {
        return productRepository.findByProductId(productId);
    }

    /**
     * Check if product has sufficient stock.
     */
    public boolean hasSufficientStock(String productId, int quantity) {
        return productRepository.findByProductId(productId)
            .map(product -> product.hasAvailableStock(quantity))
            .orElse(false);
    }

    /**
     * Reserve stock for a product.
     */
    public void reserveStock(String productId, int quantity) {
        Product product = productRepository.findByProductId(productId)
            .orElseThrow(() -> new IllegalStateException("Product not found: " + productId));

        product.reserveStock(quantity);
        productRepository.save(product);
        log.info("Reserved {} units of product {}", quantity, productId);
    }

    /**
     * Confirm a stock reservation.
     */
    public void confirmReservation(String productId, int quantity) {
        Product product = productRepository.findByProductId(productId)
            .orElseThrow(() -> new IllegalStateException("Product not found: " + productId));

        product.confirmReservation(quantity);
        productRepository.save(product);
        log.info("Confirmed {} units reservation for product {}", quantity, productId);
    }

    /**
     * Release a stock reservation.
     */
    public void releaseReservation(String productId, int quantity) {
        Product product = productRepository.findByProductId(productId)
            .orElseThrow(() -> new IllegalStateException("Product not found: " + productId));

        product.releaseReservation(quantity);
        productRepository.save(product);
        log.info("Released {} units reservation for product {}", quantity, productId);
    }
}
