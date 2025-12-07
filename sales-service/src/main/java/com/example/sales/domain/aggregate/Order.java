package com.example.sales.domain.aggregate;

import com.example.shared.command.CancelOrderCommand;
import com.example.shared.command.ConfirmOrderCommand;
import com.example.shared.command.CreateOrderCommand;
import com.example.shared.event.OrderCancelledEvent;
import com.example.shared.event.OrderConfirmedEvent;
import com.example.shared.event.OrderCreatedEvent;
import com.example.shared.valueobject.OrderStatus;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Order aggregate root.
 * Manages the lifecycle and state of an order.
 */
@Aggregate
public class Order {

    @AggregateIdentifier
    private UUID orderId;
    private String customerId;
    private String productId;
    private int quantity;
    private BigDecimal amount;
    private OrderStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Required by Axon for aggregate reconstruction.
     */
    protected Order() {
    }

    @CommandHandler
    public Order(CreateOrderCommand command) {
        AggregateLifecycle.apply(new OrderCreatedEvent(
            command.orderId(),
            command.customerId(),
            command.productId(),
            command.quantity(),
            command.amount()
        ));
    }

    @CommandHandler
    public void handle(ConfirmOrderCommand command) {
        if (status.isTerminal()) {
            throw new IllegalStateException("Cannot confirm order in status: " + status);
        }
        AggregateLifecycle.apply(new OrderConfirmedEvent(command.orderId()));
    }

    @CommandHandler
    public void handle(CancelOrderCommand command) {
        if (status.isTerminal()) {
            throw new IllegalStateException("Cannot cancel order in status: " + status);
        }
        AggregateLifecycle.apply(new OrderCancelledEvent(
            command.orderId(),
            command.reason(),
            command.isTimeout()
        ));
    }

    @EventSourcingHandler
    public void on(OrderCreatedEvent event) {
        this.orderId = event.orderId();
        this.customerId = event.customerId();
        this.productId = event.productId();
        this.quantity = event.quantity();
        this.amount = event.amount();
        this.status = OrderStatus.PENDING;
        this.createdAt = event.timestamp();
        this.updatedAt = event.timestamp();
    }

    @EventSourcingHandler
    public void on(OrderConfirmedEvent event) {
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = event.timestamp();
    }

    @EventSourcingHandler
    public void on(OrderCancelledEvent event) {
        this.status = event.isTimeout() ? OrderStatus.CANCELLED_TIMEOUT : OrderStatus.CANCELLED;
        this.updatedAt = event.timestamp();
    }

    // Getters for read access
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
