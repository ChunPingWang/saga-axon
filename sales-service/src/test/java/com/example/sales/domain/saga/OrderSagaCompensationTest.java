package com.example.sales.domain.saga;

import com.example.shared.command.*;
import com.example.shared.event.*;
import org.axonframework.test.saga.SagaTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.axonframework.test.matchers.Matchers.*;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Unit tests for OrderSaga - Compensation path (User Story 2).
 * Tests the compensation flows when reservations fail.
 */
@DisplayName("OrderSaga - Compensation Path")
class OrderSagaCompensationTest {

    private SagaTestFixture<OrderSaga> fixture;

    @BeforeEach
    void setUp() {
        fixture = new SagaTestFixture<>(OrderSaga.class);
    }

    @Test
    @DisplayName("should release inventory when payment fails after inventory is reserved")
    void shouldReleaseInventoryWhenPaymentFails() {
        UUID orderId = UUID.randomUUID();
        UUID paymentReservationId = UUID.randomUUID();
        UUID inventoryReservationId = UUID.randomUUID();

        // Step 1: When payment fails, release inventory command should be sent
        fixture.givenAPublished(new OrderCreatedEvent(
                orderId,
                "CUST-001",
                "IPHONE17",
                1,
                new BigDecimal("35000")
            ))
            .andThenAPublished(new InventoryReservedEvent(
                orderId,
                inventoryReservationId,
                "IPHONE17",
                1,
                9
            ))
            .whenPublishingA(new PaymentReservationFailedEvent(
                orderId,
                paymentReservationId,
                "CUST-001",
                new BigDecimal("35000"),
                BigDecimal.ZERO,
                "INSUFFICIENT_CREDIT",
                "Insufficient credit"
            ))
            .expectDispatchedCommandsMatching(exactSequenceOf(
                messageWithPayload(instanceOf(ReleaseInventoryCommand.class))
            ));
    }

    @Test
    @DisplayName("should cancel order after inventory is released")
    void shouldCancelOrderAfterInventoryReleased() {
        UUID orderId = UUID.randomUUID();
        UUID paymentReservationId = UUID.randomUUID();
        UUID inventoryReservationId = UUID.randomUUID();

        // Step 2: After inventory is released, cancel order command should be sent
        fixture.givenAPublished(new OrderCreatedEvent(
                orderId,
                "CUST-001",
                "IPHONE17",
                1,
                new BigDecimal("35000")
            ))
            .andThenAPublished(new InventoryReservedEvent(
                orderId,
                inventoryReservationId,
                "IPHONE17",
                1,
                9
            ))
            .andThenAPublished(new PaymentReservationFailedEvent(
                orderId,
                paymentReservationId,
                "CUST-001",
                new BigDecimal("35000"),
                BigDecimal.ZERO,
                "INSUFFICIENT_CREDIT",
                "Insufficient credit"
            ))
            .whenPublishingA(new InventoryReleasedEvent(
                orderId,
                inventoryReservationId,
                "IPHONE17",
                1,
                "Payment failed: INSUFFICIENT_CREDIT"
            ))
            .expectDispatchedCommandsMatching(exactSequenceOf(
                messageWithPayload(instanceOf(CancelOrderCommand.class))
            ));
    }

    @Test
    @DisplayName("should release payment when inventory fails after payment is reserved")
    void shouldReleasePaymentWhenInventoryFails() {
        UUID orderId = UUID.randomUUID();
        UUID paymentReservationId = UUID.randomUUID();
        UUID inventoryReservationId = UUID.randomUUID();

        // When inventory fails, release payment command should be sent
        fixture.givenAPublished(new OrderCreatedEvent(
                orderId,
                "CUST-001",
                "IPHONE17",
                1,
                new BigDecimal("35000")
            ))
            .andThenAPublished(new PaymentReservedEvent(
                orderId,
                paymentReservationId,
                "CUST-001",
                new BigDecimal("35000"),
                Instant.now().plusSeconds(15)
            ))
            .whenPublishingA(new InventoryReservationFailedEvent(
                orderId,
                inventoryReservationId,
                "IPHONE17",
                1,
                0,
                "OUT_OF_STOCK",
                "Product out of stock"
            ))
            .expectDispatchedCommandsMatching(exactSequenceOf(
                messageWithPayload(instanceOf(ReleasePaymentCommand.class))
            ));
    }

    @Test
    @DisplayName("should cancel order after payment is released")
    void shouldCancelOrderAfterPaymentReleased() {
        UUID orderId = UUID.randomUUID();
        UUID paymentReservationId = UUID.randomUUID();
        UUID inventoryReservationId = UUID.randomUUID();

        // After payment is released, cancel order command should be sent
        fixture.givenAPublished(new OrderCreatedEvent(
                orderId,
                "CUST-001",
                "IPHONE17",
                1,
                new BigDecimal("35000")
            ))
            .andThenAPublished(new PaymentReservedEvent(
                orderId,
                paymentReservationId,
                "CUST-001",
                new BigDecimal("35000"),
                Instant.now().plusSeconds(15)
            ))
            .andThenAPublished(new InventoryReservationFailedEvent(
                orderId,
                inventoryReservationId,
                "IPHONE17",
                1,
                0,
                "OUT_OF_STOCK",
                "Product out of stock"
            ))
            .whenPublishingA(new PaymentReleasedEvent(
                orderId,
                paymentReservationId,
                new BigDecimal("35000"),
                "Inventory failed: OUT_OF_STOCK"
            ))
            .expectDispatchedCommandsMatching(exactSequenceOf(
                messageWithPayload(instanceOf(CancelOrderCommand.class))
            ));
    }

    @Test
    @DisplayName("should cancel order without compensation when payment fails first")
    void shouldCancelOrderWhenPaymentFailsFirst() {
        UUID orderId = UUID.randomUUID();
        UUID paymentReservationId = UUID.randomUUID();

        fixture.givenAPublished(new OrderCreatedEvent(
                orderId,
                "CUST-001",
                "IPHONE17",
                1,
                new BigDecimal("35000")
            ))
            .whenPublishingA(new PaymentReservationFailedEvent(
                orderId,
                paymentReservationId,
                "CUST-001",
                new BigDecimal("35000"),
                BigDecimal.ZERO,
                "CUSTOMER_NOT_FOUND",
                "Customer not found"
            ))
            .expectDispatchedCommandsMatching(exactSequenceOf(
                messageWithPayload(instanceOf(CancelOrderCommand.class))
            ));
    }

    @Test
    @DisplayName("should cancel order without compensation when inventory fails first")
    void shouldCancelOrderWhenInventoryFailsFirst() {
        UUID orderId = UUID.randomUUID();
        UUID inventoryReservationId = UUID.randomUUID();

        fixture.givenAPublished(new OrderCreatedEvent(
                orderId,
                "CUST-001",
                "IPHONE17",
                1,
                new BigDecimal("35000")
            ))
            .whenPublishingA(new InventoryReservationFailedEvent(
                orderId,
                inventoryReservationId,
                "IPHONE17",
                1,
                0,
                "PRODUCT_NOT_FOUND",
                "Product not found"
            ))
            .expectDispatchedCommandsMatching(exactSequenceOf(
                messageWithPayload(instanceOf(CancelOrderCommand.class))
            ));
    }

    @Test
    @DisplayName("should end saga when order is cancelled")
    void shouldEndSagaWhenOrderCancelled() {
        UUID orderId = UUID.randomUUID();
        UUID paymentReservationId = UUID.randomUUID();
        UUID inventoryReservationId = UUID.randomUUID();

        fixture.givenAPublished(new OrderCreatedEvent(
                orderId,
                "CUST-001",
                "IPHONE17",
                1,
                new BigDecimal("35000")
            ))
            .andThenAPublished(new InventoryReservedEvent(
                orderId,
                inventoryReservationId,
                "IPHONE17",
                1,
                9
            ))
            .andThenAPublished(new PaymentReservationFailedEvent(
                orderId,
                paymentReservationId,
                "CUST-001",
                new BigDecimal("35000"),
                BigDecimal.ZERO,
                "INSUFFICIENT_CREDIT",
                "Insufficient credit"
            ))
            .andThenAPublished(new InventoryReleasedEvent(
                orderId,
                inventoryReservationId,
                "IPHONE17",
                1,
                "Payment failed"
            ))
            .whenPublishingA(new OrderCancelledEvent(orderId, "Payment failed", false))
            .expectActiveSagas(0);
    }

    @Test
    @DisplayName("should handle both failures gracefully without duplicate commands")
    void shouldHandleBothFailuresGracefully() {
        UUID orderId = UUID.randomUUID();
        UUID paymentReservationId = UUID.randomUUID();
        UUID inventoryReservationId = UUID.randomUUID();

        // When second failure arrives after first failure already triggered compensation,
        // no additional commands should be dispatched (compensation already in progress)
        fixture.givenAPublished(new OrderCreatedEvent(
                orderId,
                "CUST-001",
                "IPHONE17",
                1,
                new BigDecimal("35000")
            ))
            .andThenAPublished(new PaymentReservationFailedEvent(
                orderId,
                paymentReservationId,
                "CUST-001",
                new BigDecimal("35000"),
                BigDecimal.ZERO,
                "CUSTOMER_NOT_FOUND",
                "Customer not found"
            ))
            .whenPublishingA(new InventoryReservationFailedEvent(
                orderId,
                inventoryReservationId,
                "IPHONE17",
                1,
                0,
                "PRODUCT_NOT_FOUND",
                "Product not found"
            ))
            // No new commands since compensation already started
            .expectNoDispatchedCommands();
    }
}
