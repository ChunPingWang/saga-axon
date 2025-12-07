package com.example.inventory.domain.aggregate;

import com.example.inventory.domain.entity.Product;
import com.example.inventory.domain.repository.ProductRepository;
import com.example.shared.command.ConfirmInventoryCommand;
import com.example.shared.command.ReleaseInventoryCommand;
import com.example.shared.command.ReserveInventoryCommand;
import com.example.shared.event.InventoryConfirmedEvent;
import com.example.shared.event.InventoryReleasedEvent;
import com.example.shared.event.InventoryReservationFailedEvent;
import com.example.shared.event.InventoryReservedEvent;
import com.example.shared.valueobject.ReservationStatus;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.axonframework.test.matchers.Matchers.*;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InventoryReservation aggregate.
 */
@DisplayName("InventoryReservation Aggregate")
class InventoryReservationTest {

    private FixtureConfiguration<InventoryReservation> fixture;

    @Mock
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fixture = new AggregateTestFixture<>(InventoryReservation.class);
        fixture.registerInjectableResource(productRepository);
    }

    @Nested
    @DisplayName("ReserveInventoryCommand")
    class ReserveInventoryTests {

        @Test
        @DisplayName("should reserve inventory when product has sufficient stock")
        void shouldReserveInventory() {
            UUID reservationId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            String productId = "IPHONE17";
            int quantity = 1;

            Product product = new Product(productId, "iPhone 17", new BigDecimal("35000"), 10);
            when(productRepository.findByProductId(productId)).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenReturn(product);

            fixture.givenNoPriorActivity()
                .when(new ReserveInventoryCommand(reservationId, orderId, productId, quantity))
                .expectSuccessfulHandlerExecution()
                .expectState(reservation -> {
                    assertEquals(ReservationStatus.RESERVED, reservation.getStatus());
                    assertEquals(orderId, reservation.getOrderId());
                    assertEquals(productId, reservation.getProductId());
                    assertEquals(quantity, reservation.getQuantity());
                });

            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("should fail reservation when product not found")
        void shouldFailWhenProductNotFound() {
            UUID reservationId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            String productId = "UNKNOWN";
            int quantity = 1;

            when(productRepository.findByProductId(productId)).thenReturn(Optional.empty());

            fixture.givenNoPriorActivity()
                .when(new ReserveInventoryCommand(reservationId, orderId, productId, quantity))
                .expectSuccessfulHandlerExecution()
                .expectEventsMatching(exactSequenceOf(
                    messageWithPayload(instanceOf(InventoryReservationFailedEvent.class))
                ))
                .expectState(reservation -> assertEquals(ReservationStatus.RELEASED, reservation.getStatus()));
        }

        @Test
        @DisplayName("should fail reservation when insufficient stock")
        void shouldFailWhenInsufficientStock() {
            UUID reservationId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            String productId = "IPHONE17_SOLDOUT";
            int quantity = 1;

            Product product = new Product(productId, "iPhone 17 (Sold Out)", new BigDecimal("35000"), 0);
            when(productRepository.findByProductId(productId)).thenReturn(Optional.of(product));

            fixture.givenNoPriorActivity()
                .when(new ReserveInventoryCommand(reservationId, orderId, productId, quantity))
                .expectSuccessfulHandlerExecution()
                .expectEventsMatching(exactSequenceOf(
                    messageWithPayload(instanceOf(InventoryReservationFailedEvent.class))
                ))
                .expectState(reservation -> assertEquals(ReservationStatus.RELEASED, reservation.getStatus()));
        }
    }

    @Nested
    @DisplayName("ConfirmInventoryCommand")
    class ConfirmInventoryTests {

        @Test
        @DisplayName("should confirm inventory reservation")
        void shouldConfirmInventory() {
            UUID reservationId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            String productId = "IPHONE17";
            int quantity = 1;

            Product product = new Product(productId, "iPhone 17", new BigDecimal("35000"), 10);
            product.reserveStock(quantity);
            when(productRepository.findByProductId(productId)).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenReturn(product);

            fixture.given(new InventoryReservedEvent(orderId, reservationId, productId, quantity, 9))
                .when(new ConfirmInventoryCommand(reservationId, orderId))
                .expectSuccessfulHandlerExecution()
                .expectEventsMatching(exactSequenceOf(
                    messageWithPayload(instanceOf(InventoryConfirmedEvent.class))
                ))
                .expectState(reservation -> assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus()));
        }
    }

    @Nested
    @DisplayName("ReleaseInventoryCommand")
    class ReleaseInventoryTests {

        @Test
        @DisplayName("should release inventory reservation")
        void shouldReleaseInventory() {
            UUID reservationId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            String productId = "IPHONE17";
            int quantity = 1;
            String reason = "Payment failed";

            Product product = new Product(productId, "iPhone 17", new BigDecimal("35000"), 10);
            product.reserveStock(quantity);
            when(productRepository.findByProductId(productId)).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenReturn(product);

            fixture.given(new InventoryReservedEvent(orderId, reservationId, productId, quantity, 9))
                .when(new ReleaseInventoryCommand(reservationId, orderId, reason))
                .expectSuccessfulHandlerExecution()
                .expectEventsMatching(exactSequenceOf(
                    messageWithPayload(instanceOf(InventoryReleasedEvent.class))
                ))
                .expectState(reservation -> assertEquals(ReservationStatus.RELEASED, reservation.getStatus()));
        }
    }
}
