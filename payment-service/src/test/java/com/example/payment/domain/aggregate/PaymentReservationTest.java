package com.example.payment.domain.aggregate;

import com.example.payment.domain.entity.CustomerCredit;
import com.example.payment.domain.repository.CustomerCreditRepository;
import com.example.shared.command.ConfirmPaymentCommand;
import com.example.shared.command.ReleasePaymentCommand;
import com.example.shared.command.ReservePaymentCommand;
import com.example.shared.event.PaymentConfirmedEvent;
import com.example.shared.event.PaymentReleasedEvent;
import com.example.shared.event.PaymentReservationFailedEvent;
import com.example.shared.event.PaymentReservedEvent;
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
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.axonframework.test.matchers.Matchers.*;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentReservation aggregate.
 */
@DisplayName("PaymentReservation Aggregate")
class PaymentReservationTest {

    private FixtureConfiguration<PaymentReservation> fixture;

    @Mock
    private CustomerCreditRepository creditRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fixture = new AggregateTestFixture<>(PaymentReservation.class);
        fixture.registerInjectableResource(creditRepository);
    }

    @Nested
    @DisplayName("ReservePaymentCommand")
    class ReservePaymentTests {

        @Test
        @DisplayName("should reserve payment when customer has sufficient credit")
        void shouldReservePayment() {
            UUID reservationId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            String customerId = "CUST-001";
            BigDecimal amount = new BigDecimal("35000");

            CustomerCredit credit = new CustomerCredit(customerId, new BigDecimal("50000"));
            when(creditRepository.findByCustomerId(customerId)).thenReturn(Optional.of(credit));
            when(creditRepository.save(any())).thenReturn(credit);

            fixture.givenNoPriorActivity()
                .when(new ReservePaymentCommand(reservationId, orderId, customerId, amount))
                .expectSuccessfulHandlerExecution()
                .expectState(reservation -> {
                    assertEquals(ReservationStatus.RESERVED, reservation.getStatus());
                    assertEquals(orderId, reservation.getOrderId());
                    assertEquals(customerId, reservation.getCustomerId());
                    assertEquals(amount, reservation.getAmount());
                });

            verify(creditRepository).save(any(CustomerCredit.class));
        }

        @Test
        @DisplayName("should fail reservation when customer not found")
        void shouldFailWhenCustomerNotFound() {
            UUID reservationId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            String customerId = "UNKNOWN";
            BigDecimal amount = new BigDecimal("35000");

            when(creditRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());

            // When customer not found, the aggregate publishes a failure event
            // but doesn't throw an exception
            fixture.givenNoPriorActivity()
                .when(new ReservePaymentCommand(reservationId, orderId, customerId, amount))
                .expectSuccessfulHandlerExecution()
                .expectEventsMatching(exactSequenceOf(
                    messageWithPayload(instanceOf(PaymentReservationFailedEvent.class))
                ))
                .expectState(reservation -> assertEquals(ReservationStatus.RELEASED, reservation.getStatus()));
        }

        @Test
        @DisplayName("should fail reservation when insufficient credit")
        void shouldFailWhenInsufficientCredit() {
            UUID reservationId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            String customerId = "CUST-002";
            BigDecimal amount = new BigDecimal("35000");

            CustomerCredit credit = new CustomerCredit(customerId, new BigDecimal("10000"));
            when(creditRepository.findByCustomerId(customerId)).thenReturn(Optional.of(credit));

            fixture.givenNoPriorActivity()
                .when(new ReservePaymentCommand(reservationId, orderId, customerId, amount))
                .expectSuccessfulHandlerExecution()
                .expectEventsMatching(exactSequenceOf(
                    messageWithPayload(instanceOf(PaymentReservationFailedEvent.class))
                ))
                .expectState(reservation -> assertEquals(ReservationStatus.RELEASED, reservation.getStatus()));
        }
    }

    @Nested
    @DisplayName("ConfirmPaymentCommand")
    class ConfirmPaymentTests {

        @Test
        @DisplayName("should confirm payment reservation")
        void shouldConfirmPayment() {
            UUID reservationId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            String customerId = "CUST-001";
            BigDecimal amount = new BigDecimal("35000");

            CustomerCredit credit = new CustomerCredit(customerId, new BigDecimal("50000"));
            credit.reserveCredit(amount);
            when(creditRepository.findByCustomerId(customerId)).thenReturn(Optional.of(credit));
            when(creditRepository.save(any())).thenReturn(credit);

            fixture.given(new PaymentReservedEvent(orderId, reservationId, customerId, amount, Instant.now().plusSeconds(15)))
                .when(new ConfirmPaymentCommand(reservationId, orderId))
                .expectSuccessfulHandlerExecution()
                .expectEventsMatching(exactSequenceOf(
                    messageWithPayload(instanceOf(PaymentConfirmedEvent.class))
                ))
                .expectState(reservation -> assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus()));
        }
    }

    @Nested
    @DisplayName("ReleasePaymentCommand")
    class ReleasePaymentTests {

        @Test
        @DisplayName("should release payment reservation")
        void shouldReleasePayment() {
            UUID reservationId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            String customerId = "CUST-001";
            BigDecimal amount = new BigDecimal("35000");
            String reason = "Inventory failed";

            CustomerCredit credit = new CustomerCredit(customerId, new BigDecimal("50000"));
            credit.reserveCredit(amount);
            when(creditRepository.findByCustomerId(customerId)).thenReturn(Optional.of(credit));
            when(creditRepository.save(any())).thenReturn(credit);

            fixture.given(new PaymentReservedEvent(orderId, reservationId, customerId, amount, Instant.now().plusSeconds(15)))
                .when(new ReleasePaymentCommand(reservationId, orderId, reason))
                .expectSuccessfulHandlerExecution()
                .expectEventsMatching(exactSequenceOf(
                    messageWithPayload(instanceOf(PaymentReleasedEvent.class))
                ))
                .expectState(reservation -> assertEquals(ReservationStatus.RELEASED, reservation.getStatus()));
        }
    }
}
