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
 * Unit tests for OrderSaga - Success path (User Story 1).
 * Tests the happy path where both payment and inventory reservations succeed.
 */
@DisplayName("OrderSaga - Success Path")
class OrderSagaSuccessTest {

    private SagaTestFixture<OrderSaga> fixture;

    @BeforeEach
    void setUp() {
        fixture = new SagaTestFixture<>(OrderSaga.class);
    }

    @Test
    @DisplayName("should start saga and send reservation commands when order is created")
    void shouldStartSagaAndSendReservationCommands() {
        UUID orderId = UUID.randomUUID();

        fixture.givenNoPriorActivity()
            .whenPublishingA(new OrderCreatedEvent(
                orderId,
                "CUST-001",
                "IPHONE17",
                1,
                new BigDecimal("35000")
            ))
            .expectActiveSagas(1)
            .expectDispatchedCommandsMatching(listWithAllOf(
                messageWithPayload(instanceOf(ReservePaymentCommand.class)),
                messageWithPayload(instanceOf(ReserveInventoryCommand.class))
            ));
    }

    @Test
    @DisplayName("should confirm order when both payment and inventory are reserved")
    void shouldConfirmOrderWhenBothReservationsSucceed() {
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
            .andThenAPublished(new PaymentReservedEvent(
                orderId,
                paymentReservationId,
                "CUST-001",
                new BigDecimal("35000"),
                Instant.now().plusSeconds(15)
            ))
            .whenPublishingA(new InventoryReservedEvent(
                orderId,
                inventoryReservationId,
                "IPHONE17",
                1,
                9
            ))
            .expectDispatchedCommandsMatching(listWithAllOf(
                messageWithPayload(instanceOf(ConfirmPaymentCommand.class)),
                messageWithPayload(instanceOf(ConfirmInventoryCommand.class)),
                messageWithPayload(instanceOf(ConfirmOrderCommand.class))
            ));
    }

    @Test
    @DisplayName("should confirm order regardless of reservation event order")
    void shouldConfirmOrderRegardlessOfEventOrder() {
        UUID orderId = UUID.randomUUID();
        UUID paymentReservationId = UUID.randomUUID();
        UUID inventoryReservationId = UUID.randomUUID();

        // Inventory reserved first, then payment
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
            .whenPublishingA(new PaymentReservedEvent(
                orderId,
                paymentReservationId,
                "CUST-001",
                new BigDecimal("35000"),
                Instant.now().plusSeconds(15)
            ))
            .expectDispatchedCommandsMatching(listWithAllOf(
                messageWithPayload(instanceOf(ConfirmPaymentCommand.class)),
                messageWithPayload(instanceOf(ConfirmInventoryCommand.class)),
                messageWithPayload(instanceOf(ConfirmOrderCommand.class))
            ));
    }

    @Test
    @DisplayName("should end saga when order is confirmed")
    void shouldEndSagaWhenOrderConfirmed() {
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
            .andThenAPublished(new PaymentReservedEvent(
                orderId,
                paymentReservationId,
                "CUST-001",
                new BigDecimal("35000"),
                Instant.now().plusSeconds(15)
            ))
            .andThenAPublished(new InventoryReservedEvent(
                orderId,
                inventoryReservationId,
                "IPHONE17",
                1,
                9
            ))
            .whenPublishingA(new OrderConfirmedEvent(orderId))
            .expectActiveSagas(0);
    }
}
