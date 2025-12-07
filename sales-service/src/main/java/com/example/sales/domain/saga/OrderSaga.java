package com.example.sales.domain.saga;

import com.example.shared.command.*;
import com.example.shared.event.*;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

/**
 * OrderSaga coordinates the distributed transaction across Payment and Inventory services.
 * Implements the Choreography-based Saga pattern with compensation logic.
 */
@Saga
public class OrderSaga {

    private static final Logger log = LoggerFactory.getLogger(OrderSaga.class);
    private static final String ORDER_TIMEOUT_DEADLINE = "order-timeout";
    private static final Duration TIMEOUT_DURATION = Duration.ofSeconds(15);

    @Autowired
    private transient CommandGateway commandGateway;

    @Autowired
    private transient DeadlineManager deadlineManager;

    // Saga state
    private UUID orderId;
    private String customerId;
    private String productId;
    private int quantity;
    private BigDecimal amount;

    private UUID paymentReservationId;
    private UUID inventoryReservationId;

    private StepStatus paymentStatus = StepStatus.PENDING;
    private StepStatus inventoryStatus = StepStatus.PENDING;

    private String deadlineId;
    private String compensationReason;
    private boolean compensating = false;

    /**
     * Start the saga when an order is created.
     * Sends commands to reserve payment and inventory.
     */
    @StartSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void on(OrderCreatedEvent event) {
        log.info("Saga started for order: {}", event.orderId());

        this.orderId = event.orderId();
        this.customerId = event.customerId();
        this.productId = event.productId();
        this.quantity = event.quantity();
        this.amount = event.amount();

        // Schedule timeout deadline
        this.deadlineId = deadlineManager.schedule(
            TIMEOUT_DURATION,
            ORDER_TIMEOUT_DEADLINE
        );

        // Generate reservation IDs
        this.paymentReservationId = UUID.randomUUID();
        this.inventoryReservationId = UUID.randomUUID();

        // Send commands to reserve payment and inventory in parallel
        commandGateway.send(new ReservePaymentCommand(
            paymentReservationId,
            orderId,
            customerId,
            amount
        ));

        commandGateway.send(new ReserveInventoryCommand(
            inventoryReservationId,
            orderId,
            productId,
            quantity
        ));
    }

    /**
     * Handle successful payment reservation.
     */
    @SagaEventHandler(associationProperty = "orderId")
    public void on(PaymentReservedEvent event) {
        log.info("Payment reserved for order: {}", orderId);
        this.paymentReservationId = event.reservationId();
        this.paymentStatus = StepStatus.SUCCESS;
        checkCompletion();
    }

    /**
     * Handle payment reservation failure.
     */
    @SagaEventHandler(associationProperty = "orderId")
    public void on(PaymentReservationFailedEvent event) {
        log.warn("Payment reservation failed for order: {} - {}", orderId, event.reason());
        this.paymentStatus = StepStatus.FAILED;
        compensate("Payment failed: " + event.reason());
    }

    /**
     * Handle successful inventory reservation.
     */
    @SagaEventHandler(associationProperty = "orderId")
    public void on(InventoryReservedEvent event) {
        log.info("Inventory reserved for order: {}", orderId);
        this.inventoryReservationId = event.reservationId();
        this.inventoryStatus = StepStatus.SUCCESS;
        checkCompletion();
    }

    /**
     * Handle inventory reservation failure.
     */
    @SagaEventHandler(associationProperty = "orderId")
    public void on(InventoryReservationFailedEvent event) {
        log.warn("Inventory reservation failed for order: {} - {}", orderId, event.reason());
        this.inventoryStatus = StepStatus.FAILED;
        compensate("Inventory failed: " + event.reason());
    }

    /**
     * Handle payment confirmation.
     */
    @SagaEventHandler(associationProperty = "orderId")
    public void on(PaymentConfirmedEvent event) {
        log.info("Payment confirmed for order: {}", orderId);
    }

    /**
     * Handle inventory confirmation.
     */
    @SagaEventHandler(associationProperty = "orderId")
    public void on(InventoryConfirmedEvent event) {
        log.info("Inventory confirmed for order: {}", orderId);
    }

    /**
     * Handle payment release (compensation).
     */
    @SagaEventHandler(associationProperty = "orderId")
    public void on(PaymentReleasedEvent event) {
        log.info("Payment released for order: {}", orderId);
        this.paymentStatus = StepStatus.COMPENSATED;
        checkCompensationComplete();
    }

    /**
     * Handle inventory release (compensation).
     */
    @SagaEventHandler(associationProperty = "orderId")
    public void on(InventoryReleasedEvent event) {
        log.info("Inventory released for order: {}", orderId);
        this.inventoryStatus = StepStatus.COMPENSATED;
        checkCompensationComplete();
    }

    /**
     * Handle order confirmed event - end the saga successfully.
     */
    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void on(OrderConfirmedEvent event) {
        log.info("Order confirmed, ending saga: {}", orderId);
        cancelDeadline();
    }

    /**
     * Handle order cancelled event - end the saga.
     */
    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void on(OrderCancelledEvent event) {
        log.info("Order cancelled, ending saga: {}", orderId);
        cancelDeadline();
    }

    /**
     * Handle timeout deadline.
     */
    @DeadlineHandler(deadlineName = ORDER_TIMEOUT_DEADLINE)
    public void onTimeout() {
        log.warn("Order timeout for: {}", orderId);
        compensate("Operation timed out");
        commandGateway.send(CancelOrderCommand.forTimeout(orderId));
    }

    /**
     * Check if both reservations are successful and confirm the order.
     */
    private void checkCompletion() {
        if (paymentStatus == StepStatus.SUCCESS && inventoryStatus == StepStatus.SUCCESS) {
            log.info("Both reservations successful, confirming order: {}", orderId);

            // Cancel the timeout deadline
            cancelDeadline();

            // Confirm payment and inventory
            commandGateway.send(new ConfirmPaymentCommand(paymentReservationId, orderId));
            commandGateway.send(new ConfirmInventoryCommand(inventoryReservationId, orderId));

            // Confirm the order
            commandGateway.send(new ConfirmOrderCommand(orderId));
        }
    }

    /**
     * Perform compensation for failed reservations.
     */
    private void compensate(String reason) {
        // Avoid duplicate compensation
        if (compensating) {
            return;
        }
        compensating = true;
        compensationReason = reason;

        log.info("Starting compensation for order: {}", orderId);

        // Cancel the timeout deadline
        cancelDeadline();

        // Release payment if reserved
        if (paymentStatus == StepStatus.SUCCESS) {
            commandGateway.send(new ReleasePaymentCommand(paymentReservationId, orderId, reason));
        }

        // Release inventory if reserved
        if (inventoryStatus == StepStatus.SUCCESS) {
            commandGateway.send(new ReleaseInventoryCommand(inventoryReservationId, orderId, reason));
        }

        // If neither was reserved, cancel the order directly
        if (paymentStatus != StepStatus.SUCCESS && inventoryStatus != StepStatus.SUCCESS) {
            commandGateway.send(CancelOrderCommand.forPaymentFailure(orderId, reason));
        }
    }

    /**
     * Check if compensation is complete and cancel the order.
     */
    private void checkCompensationComplete() {
        // Check if all compensations are complete
        boolean paymentDone = paymentStatus == StepStatus.COMPENSATED ||
                              paymentStatus == StepStatus.FAILED ||
                              paymentStatus == StepStatus.PENDING;
        boolean inventoryDone = inventoryStatus == StepStatus.COMPENSATED ||
                                inventoryStatus == StepStatus.FAILED ||
                                inventoryStatus == StepStatus.PENDING;

        if (paymentDone && inventoryDone && compensating) {
            log.info("Compensation complete for order: {}", orderId);
            commandGateway.send(CancelOrderCommand.forPaymentFailure(orderId, compensationReason));
        }
    }

    /**
     * Cancel the timeout deadline if it exists.
     */
    private void cancelDeadline() {
        if (deadlineId != null) {
            deadlineManager.cancelSchedule(ORDER_TIMEOUT_DEADLINE, deadlineId);
            deadlineId = null;
        }
    }

    /**
     * Step status for tracking reservation progress.
     */
    public enum StepStatus {
        PENDING,
        SUCCESS,
        FAILED,
        COMPENSATED
    }
}
