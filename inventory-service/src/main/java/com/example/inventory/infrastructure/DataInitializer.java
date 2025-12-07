package com.example.inventory.infrastructure;

import com.example.inventory.domain.entity.Product;
import com.example.inventory.domain.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Initializes test data for the inventory service.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ProductRepository productRepository;

    public DataInitializer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        // Initialize products for testing
        initializeProducts();
    }

    private void initializeProducts() {
        // iPhone 17 - Main product for MVP
        if (!productRepository.existsByProductId("IPHONE17")) {
            Product iphone17 = new Product(
                "IPHONE17",
                "iPhone 17 Pro Max",
                new BigDecimal("35000"),
                10  // Initial stock
            );
            productRepository.save(iphone17);
            log.info("Created product IPHONE17 with initial stock of 10");
        }

        // Out of stock product for testing
        if (!productRepository.existsByProductId("IPHONE17_SOLDOUT")) {
            Product iphone17SoldOut = new Product(
                "IPHONE17_SOLDOUT",
                "iPhone 17 Pro Max (Sold Out)",
                new BigDecimal("35000"),
                0  // No stock
            );
            productRepository.save(iphone17SoldOut);
            log.info("Created product IPHONE17_SOLDOUT with 0 stock");
        }
    }
}
