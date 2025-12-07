package com.example.sales.infrastructure.query;

import com.example.shared.valueobject.OrderStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Query model (read model) for Order.
 * Optimized for reads, updated by event projections.
 */
@Entity
@Table(name = "order_view")
public class OrderQueryModel {

    @Id
    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "status_message")
    private String statusMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrderQueryModel() {
    }

    public OrderQueryModel(UUID orderId, String customerId, String productId,
                          int quantity, BigDecimal amount, OrderStatus status,
                          Instant createdAt) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.amount = amount;
        this.status = status;
        this.statusMessage = getDefaultStatusMessage(status);
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void updateStatus(OrderStatus newStatus, Instant timestamp) {
        this.status = newStatus;
        this.statusMessage = getDefaultStatusMessage(newStatus);
        this.updatedAt = timestamp;
    }

    public void updateStatus(OrderStatus newStatus, String message, Instant timestamp) {
        this.status = newStatus;
        this.statusMessage = message;
        this.updatedAt = timestamp;
    }

    private String getDefaultStatusMessage(OrderStatus status) {
        return switch (status) {
            case PENDING -> "訂單已受理，正在處理中";
            case PROCESSING -> "訂單處理中";
            case CONFIRMED -> "訂單已確認";
            case CANCELLED -> "訂單已取消";
            case CANCELLED_TIMEOUT -> "訂單已取消（超時）";
        };
    }

    // Getters
    public UUID getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
