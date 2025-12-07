package com.example.sales.domain.saga;

import com.example.shared.command.*;
import com.example.shared.event.*;
import org.axonframework.test.saga.SagaTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.axonframework.test.matchers.Matchers.*;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Unit tests for OrderSaga - Timeout path (User Story 3).
 * Tests the timeout mechanism using DeadlineManager.
 */
@DisplayName("OrderSaga - Timeout Path")
class OrderSagaTimeoutTest {

    private static final Duration TIMEOUT_DURATION = Duration.ofSeconds(15);
    private static final String ORDER_TIMEOUT_DEADLINE = "order-timeout";

    private SagaTestFixture<OrderSaga> fixture;

    @BeforeEach
    void setUp() {
        fixture = new SagaTestFixture<>(OrderSaga.class);
    }

    @Test
    @DisplayName("should cancel order when timeout occurs with no reservations")
    void shouldCancelOrderOnTimeoutWithNoReservations() {
        UUID orderId = UUID.randomUUID();

        fixture.givenAPublished(new OrderCreatedEvent(
                orderId,
                "CUST-001",
                "IPHONE17",
                1,
                new BigDecimal("35000")
            ))
            .whenTimeElapses(TIMEOUT_DURATION)
            .expectDispatchedCommandsMatching(exactSequenceOf(
                messageWithPayload(instanceOf(CancelOrderCommand.class))
            ));
    }

    @Test
    @DisplayName("should release payment and cancel order when timeout occurs with payment reserved")
    void shouldReleasePaymentOnTimeoutWhenPaymentReserved() {
        UUID orderId = UUID.randomUUID();
        UUID paymentReservationId = UUID.randomUUID();

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
            .whenTimeElapses(TIMEOUT_DURATION)
            .expectDispatchedCommandsMatching(listWithAllOf(
                messageWithPayload(instanceOf(ReleasePaymentCommand.class)),
                messageWithPayload(instanceOf(CancelOrderCommand.class))
            ));
    }

    @Test
    @DisplayName("should release inventory and cancel order when timeout occurs with inventory reserved")
    void shouldReleaseInventoryOnTimeoutWhenInventoryReserved() {
        UUID orderId = UUID.randomUUID();
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
            .whenTimeElapses(TIMEOUT_DURATION)
            .expectDispatchedCommandsMatching(listWithAllOf(
                messageWithPayload(instanceOf(ReleaseInventoryCommand.class)),
                messageWithPayload(instanceOf(CancelOrderCommand.class))
            ));
    }

    // Note: Test for "both reservations reserved but timeout" is removed because
    // when both reservations succeed, the saga immediately triggers confirmation,
    // canceling the deadline. So timeout cannot occur in this valid scenario.

    @Test
    @DisplayName("should mark order as cancelled due to timeout")
    void shouldMarkOrderAsCancelledDueToTimeout() {
        UUID orderId = UUID.randomUUID();

        // When timeout triggers, CancelOrderCommand should be for timeout
        fixture.givenAPublished(new OrderCreatedEvent(
                orderId,
                "CUST-001",
                "IPHONE17",
                1,
                new BigDecimal("35000")
            ))
            .whenTimeElapses(TIMEOUT_DURATION)
            .expectDispatchedCommandsMatching(
                exactSequenceOf(
                    messageWithPayload(instanceOf(CancelOrderCommand.class))
                )
            );
    }

    @Test
    @DisplayName("should not timeout after successful confirmation")
    void shouldNotTimeoutAfterSuccessfulConfirmation() {
        UUID orderId = UUID.randomUUID();
        UUID paymentReservationId = UUID.randomUUID();
        UUID inventoryReservationId = UUID.randomUUID();

        // First confirm the order
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
            .andThenAPublished(new PaymentConfirmedEvent(orderId, paymentReservationId, new BigDecimal("35000")))
            .andThenAPublished(new InventoryConfirmedEvent(orderId, inventoryReservationId, "IPHONE17", 1))
            .andThenAPublished(new OrderConfirmedEvent(orderId))
            // After order is confirmed, timeout should not trigger any commands
            .whenTimeElapses(TIMEOUT_DURATION)
            .expectNoDispatchedCommands()
            .expectActiveSagas(0);
    }

    @Test
    @DisplayName("should end saga when order is cancelled due to timeout")
    void shouldEndSagaOnTimeoutCancellation() {
        UUID orderId = UUID.randomUUID();

        fixture.givenAPublished(new OrderCreatedEvent(
                orderId,
                "CUST-001",
                "IPHONE17",
                1,
                new BigDecimal("35000")
            ))
            .whenPublishingA(new OrderCancelledEvent(orderId, "Operation timed out", true))
            .expectActiveSagas(0);
    }
}
