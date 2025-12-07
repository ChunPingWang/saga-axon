package com.example.inventory.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

/**
 * Product entity representing a product in the inventory.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "available_stock", nullable = false)
    private int availableStock;

    @Column(name = "reserved_stock", nullable = false)
    private int reservedStock;

    @Version
    private Long version;

    protected Product() {
    }

    public Product(String productId, String name, BigDecimal price, int initialStock) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.availableStock = initialStock;
        this.reservedStock = 0;
    }

    /**
     * Check if the product has enough available stock.
     */
    public boolean hasAvailableStock(int quantity) {
        return availableStock >= quantity;
    }

    /**
     * Reserve stock for a transaction.
     */
    public void reserveStock(int quantity) {
        if (!hasAvailableStock(quantity)) {
            throw new IllegalStateException("Insufficient stock");
        }
        this.availableStock -= quantity;
        this.reservedStock += quantity;
    }

    /**
     * Confirm a reservation (deduct from reserved).
     */
    public void confirmReservation(int quantity) {
        if (reservedStock < quantity) {
            throw new IllegalStateException("Insufficient reserved stock");
        }
        this.reservedStock -= quantity;
    }

    /**
     * Release a reservation (return to available).
     */
    public void releaseReservation(int quantity) {
        if (reservedStock < quantity) {
            throw new IllegalStateException("Insufficient reserved stock");
        }
        this.reservedStock -= quantity;
        this.availableStock += quantity;
    }

    // Getters
    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getAvailableStock() {
        return availableStock;
    }

    public int getReservedStock() {
        return reservedStock;
    }

    public int getTotalStock() {
        return availableStock + reservedStock;
    }

    public Long getVersion() {
        return version;
    }
}
