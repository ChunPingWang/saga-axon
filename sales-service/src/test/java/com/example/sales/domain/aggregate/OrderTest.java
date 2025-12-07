package com.example.sales.domain.aggregate;

import com.example.shared.command.CancelOrderCommand;
import com.example.shared.command.ConfirmOrderCommand;
import com.example.shared.command.CreateOrderCommand;
import com.example.shared.event.OrderCancelledEvent;
import com.example.shared.event.OrderConfirmedEvent;
import com.example.shared.event.OrderCreatedEvent;
import com.example.shared.valueobject.OrderStatus;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.axonframework.test.matchers.Matchers.*;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Order aggregate.
 * Tests the command handling and event sourcing behavior.
 */
@DisplayName("Order Aggregate")
class OrderTest {

    private FixtureConfiguration<Order> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(Order.class);
    }

    @Nested
    @DisplayName("CreateOrderCommand")
    class CreateOrderTests {

        @Test
        @DisplayName("should create order and publish OrderCreatedEvent")
        void shouldCreateOrder() {
            UUID orderId = UUID.randomUUID();
            String customerId = "CUST-001";
            String productId = "IPHONE17";
            int quantity = 1;
            BigDecimal amount = new BigDecimal("35000");

            fixture.givenNoPriorActivity()
                .when(new CreateOrderCommand(orderId, customerId, productId, quantity, amount))
                .expectSuccessfulHandlerExecution()
                .expectEventsMatching(exactSequenceOf(
                    messageWithPayload(instanceOf(OrderCreatedEvent.class))
                ))
                .expectState(order -> {
                    assertEquals(orderId, order.getOrderId());
                    assertEquals(customerId, order.getCustomerId());
                    assertEquals(productId, order.getProductId());
                    assertEquals(quantity, order.getQuantity());
                    assertEquals(amount, order.getAmount());
                    assertEquals(OrderStatus.PENDING, order.getStatus());
                    assertNotNull(order.getCreatedAt());
                });
        }

        @Test
        @DisplayName("should reject null orderId")
        void shouldRejectNullOrderId() {
            assertThrows(IllegalArgumentException.class, () ->
                new CreateOrderCommand(null, "CUST-001", "IPHONE17", 1, new BigDecimal("35000"))
            );
        }

        @Test
        @DisplayName("should reject blank customerId")
        void shouldRejectBlankCustomerId() {
            assertThrows(IllegalArgumentException.class, () ->
                new CreateOrderCommand(UUID.randomUUID(), "", "IPHONE17", 1, new BigDecimal("35000"))
            );
        }

        @Test
        @DisplayName("should reject zero quantity")
        void shouldRejectZeroQuantity() {
            assertThrows(IllegalArgumentException.class, () ->
                new CreateOrderCommand(UUID.randomUUID(), "CUST-001", "IPHONE17", 0, new BigDecimal("35000"))
            );
        }

        @Test
        @DisplayName("should reject zero amount")
        void shouldRejectZeroAmount() {
            assertThrows(IllegalArgumentException.class, () ->
                new CreateOrderCommand(UUID.randomUUID(), "CUST-001", "IPHONE17", 1, BigDecimal.ZERO)
            );
        }
    }

    @Nested
    @DisplayName("ConfirmOrderCommand")
    class ConfirmOrderTests {

        @Test
        @DisplayName("should confirm order and publish OrderConfirmedEvent")
        void shouldConfirmOrder() {
            UUID orderId = UUID.randomUUID();

            fixture.given(new OrderCreatedEvent(orderId, "CUST-001", "IPHONE17", 1, new BigDecimal("35000")))
                .when(new ConfirmOrderCommand(orderId))
                .expectSuccessfulHandlerExecution()
                .expectEventsMatching(exactSequenceOf(
                    messageWithPayload(instanceOf(OrderConfirmedEvent.class))
                ))
                .expectState(order -> assertEquals(OrderStatus.CONFIRMED, order.getStatus()));
        }

        @Test
        @DisplayName("should reject confirmation of already confirmed order")
        void shouldRejectDoubleConfirmation() {
            UUID orderId = UUID.randomUUID();

            fixture.given(
                    new OrderCreatedEvent(orderId, "CUST-001", "IPHONE17", 1, new BigDecimal("35000")),
                    new OrderConfirmedEvent(orderId)
                )
                .when(new ConfirmOrderCommand(orderId))
                .expectException(IllegalStateException.class);
        }

        @Test
        @DisplayName("should reject confirmation of cancelled order")
        void shouldRejectConfirmationOfCancelledOrder() {
            UUID orderId = UUID.randomUUID();

            fixture.given(
                    new OrderCreatedEvent(orderId, "CUST-001", "IPHONE17", 1, new BigDecimal("35000")),
                    new OrderCancelledEvent(orderId, "Test cancellation", false)
                )
                .when(new ConfirmOrderCommand(orderId))
                .expectException(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("CancelOrderCommand")
    class CancelOrderTests {

        @Test
        @DisplayName("should cancel order and publish OrderCancelledEvent")
        void shouldCancelOrder() {
            UUID orderId = UUID.randomUUID();
            String reason = "Payment failed";

            fixture.given(new OrderCreatedEvent(orderId, "CUST-001", "IPHONE17", 1, new BigDecimal("35000")))
                .when(new CancelOrderCommand(orderId, reason, false))
                .expectSuccessfulHandlerExecution()
                .expectEventsMatching(exactSequenceOf(
                    messageWithPayload(instanceOf(OrderCancelledEvent.class))
                ))
                .expectState(order -> assertEquals(OrderStatus.CANCELLED, order.getStatus()));
        }

        @Test
        @DisplayName("should cancel order due to timeout")
        void shouldCancelOrderDueToTimeout() {
            UUID orderId = UUID.randomUUID();

            fixture.given(new OrderCreatedEvent(orderId, "CUST-001", "IPHONE17", 1, new BigDecimal("35000")))
                .when(CancelOrderCommand.forTimeout(orderId))
                .expectSuccessfulHandlerExecution()
                .expectEventsMatching(exactSequenceOf(
                    messageWithPayload(instanceOf(OrderCancelledEvent.class))
                ))
                .expectState(order -> assertEquals(OrderStatus.CANCELLED_TIMEOUT, order.getStatus()));
        }

        @Test
        @DisplayName("should reject cancellation of already confirmed order")
        void shouldRejectCancellationOfConfirmedOrder() {
            UUID orderId = UUID.randomUUID();

            fixture.given(
                    new OrderCreatedEvent(orderId, "CUST-001", "IPHONE17", 1, new BigDecimal("35000")),
                    new OrderConfirmedEvent(orderId)
                )
                .when(new CancelOrderCommand(orderId, "Late cancellation", false))
                .expectException(IllegalStateException.class);
        }

        @Test
        @DisplayName("should reject cancellation of already cancelled order")
        void shouldRejectDoubleCancellation() {
            UUID orderId = UUID.randomUUID();

            fixture.given(
                    new OrderCreatedEvent(orderId, "CUST-001", "IPHONE17", 1, new BigDecimal("35000")),
                    new OrderCancelledEvent(orderId, "First cancellation", false)
                )
                .when(new CancelOrderCommand(orderId, "Second cancellation", false))
                .expectException(IllegalStateException.class);
        }
    }
}
