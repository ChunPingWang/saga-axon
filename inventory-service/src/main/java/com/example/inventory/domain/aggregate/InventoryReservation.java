package com.example.inventory.domain.aggregate;

import com.example.shared.command.ConfirmInventoryCommand;
import com.example.shared.command.ReleaseInventoryCommand;
import com.example.shared.command.ReserveInventoryCommand;
import com.example.shared.event.InventoryConfirmedEvent;
import com.example.shared.event.InventoryReleasedEvent;
import com.example.shared.event.InventoryReservationFailedEvent;
import com.example.shared.event.InventoryReservedEvent;
import com.example.shared.valueobject.ReservationStatus;
import com.example.inventory.domain.entity.Product;
import com.example.inventory.domain.repository.ProductRepository;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.Instant;
import java.util.UUID;

/**
 * InventoryReservation aggregate root.
 * Manages the lifecycle and state of an inventory reservation.
 */
@Aggregate
public class InventoryReservation {

    @AggregateIdentifier
    private UUID reservationId;
    private UUID orderId;
    private String productId;
    private int quantity;
    private ReservationStatus status;
    private Instant createdAt;

    /**
     * Required by Axon for aggregate reconstruction.
     */
    protected InventoryReservation() {
    }

    @CommandHandler
    public InventoryReservation(ReserveInventoryCommand command, ProductRepository productRepository) {
        Product product = productRepository.findByProductId(command.productId())
            .orElse(null);

        if (product == null) {
            AggregateLifecycle.apply(new InventoryReservationFailedEvent(
                command.orderId(),
                command.reservationId(),
                command.productId(),
                command.quantity(),
                0,
                InventoryReservationFailedEvent.Reason.PRODUCT_NOT_FOUND.name(),
                "Product not found: " + command.productId()
            ));
            return;
        }

        if (!product.hasAvailableStock(command.quantity())) {
            AggregateLifecycle.apply(new InventoryReservationFailedEvent(
                command.orderId(),
                command.reservationId(),
                command.productId(),
                command.quantity(),
                product.getAvailableStock(),
                InventoryReservationFailedEvent.Reason.OUT_OF_STOCK.name(),
                "Insufficient stock. Required: " + command.quantity() + ", Available: " + product.getAvailableStock()
            ));
            return;
        }

        // Reserve the stock
        product.reserveStock(command.quantity());
        productRepository.save(product);

        AggregateLifecycle.apply(new InventoryReservedEvent(
            command.orderId(),
            command.reservationId(),
            command.productId(),
            command.quantity(),
            product.getAvailableStock()
        ));
    }

    @CommandHandler
    public void handle(ConfirmInventoryCommand command, ProductRepository productRepository) {
        if (status != ReservationStatus.RESERVED) {
            throw new IllegalStateException("Cannot confirm inventory in status: " + status);
        }

        // Confirm the stock deduction
        Product product = productRepository.findByProductId(productId)
            .orElseThrow(() -> new IllegalStateException("Product not found: " + productId));
        product.confirmReservation(quantity);
        productRepository.save(product);

        AggregateLifecycle.apply(new InventoryConfirmedEvent(
            command.orderId(),
            command.reservationId(),
            productId,
            quantity
        ));
    }

    @CommandHandler
    public void handle(ReleaseInventoryCommand command, ProductRepository productRepository) {
        if (status != ReservationStatus.RESERVED) {
            throw new IllegalStateException("Cannot release inventory in status: " + status);
        }

        // Release the reserved stock
        Product product = productRepository.findByProductId(productId)
            .orElseThrow(() -> new IllegalStateException("Product not found: " + productId));
        product.releaseReservation(quantity);
        productRepository.save(product);

        AggregateLifecycle.apply(new InventoryReleasedEvent(
            command.orderId(),
            command.reservationId(),
            productId,
            quantity,
            command.reason()
        ));
    }

    @EventSourcingHandler
    public void on(InventoryReservedEvent event) {
        this.reservationId = event.reservationId();
        this.orderId = event.orderId();
        this.productId = event.productId();
        this.quantity = event.quantity();
        this.status = ReservationStatus.RESERVED;
        this.createdAt = event.timestamp();
    }

    @EventSourcingHandler
    public void on(InventoryReservationFailedEvent event) {
        // Set status to RELEASED on failure
        this.reservationId = event.reservationId();
        this.orderId = event.orderId();
        this.productId = event.productId();
        this.quantity = event.requestedQuantity();
        this.status = ReservationStatus.RELEASED;
        this.createdAt = event.timestamp();
    }

    @EventSourcingHandler
    public void on(InventoryConfirmedEvent event) {
        this.status = ReservationStatus.CONFIRMED;
    }

    @EventSourcingHandler
    public void on(InventoryReleasedEvent event) {
        this.status = ReservationStatus.RELEASED;
    }

    // Getters
    public UUID getReservationId() {
        return reservationId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
