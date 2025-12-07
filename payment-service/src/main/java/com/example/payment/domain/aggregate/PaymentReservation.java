package com.example.payment.domain.aggregate;

import com.example.shared.command.ConfirmPaymentCommand;
import com.example.shared.command.ReleasePaymentCommand;
import com.example.shared.command.ReservePaymentCommand;
import com.example.shared.event.PaymentConfirmedEvent;
import com.example.shared.event.PaymentReleasedEvent;
import com.example.shared.event.PaymentReservationFailedEvent;
import com.example.shared.event.PaymentReservedEvent;
import com.example.shared.valueobject.ReservationStatus;
import com.example.payment.domain.entity.CustomerCredit;
import com.example.payment.domain.repository.CustomerCreditRepository;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * PaymentReservation aggregate root.
 * Manages the lifecycle and state of a payment reservation.
 */
@Aggregate
public class PaymentReservation {

    private static final int RESERVATION_EXPIRY_SECONDS = 15;

    @AggregateIdentifier
    private UUID reservationId;
    private UUID orderId;
    private String customerId;
    private BigDecimal amount;
    private ReservationStatus status;
    private Instant expiresAt;
    private Instant createdAt;

    /**
     * Required by Axon for aggregate reconstruction.
     */
    protected PaymentReservation() {
    }

    @CommandHandler
    public PaymentReservation(ReservePaymentCommand command, CustomerCreditRepository creditRepository) {
        CustomerCredit credit = creditRepository.findByCustomerId(command.customerId())
            .orElse(null);

        if (credit == null) {
            AggregateLifecycle.apply(new PaymentReservationFailedEvent(
                command.orderId(),
                command.reservationId(),
                command.customerId(),
                command.amount(),
                BigDecimal.ZERO,
                PaymentReservationFailedEvent.Reason.CUSTOMER_NOT_FOUND.name(),
                "Customer not found: " + command.customerId()
            ));
            return;
        }

        if (!credit.hasAvailableCredit(command.amount())) {
            AggregateLifecycle.apply(new PaymentReservationFailedEvent(
                command.orderId(),
                command.reservationId(),
                command.customerId(),
                command.amount(),
                credit.getAvailableCredit(),
                PaymentReservationFailedEvent.Reason.INSUFFICIENT_CREDIT.name(),
                "Insufficient credit. Required: " + command.amount() + ", Available: " + credit.getAvailableCredit()
            ));
            return;
        }

        // Reserve the credit
        credit.reserveCredit(command.amount());
        creditRepository.save(credit);

        Instant expiresAt = Instant.now().plus(RESERVATION_EXPIRY_SECONDS, ChronoUnit.SECONDS);
        AggregateLifecycle.apply(new PaymentReservedEvent(
            command.orderId(),
            command.reservationId(),
            command.customerId(),
            command.amount(),
            expiresAt
        ));
    }

    @CommandHandler
    public void handle(ConfirmPaymentCommand command, CustomerCreditRepository creditRepository) {
        if (status != ReservationStatus.RESERVED) {
            throw new IllegalStateException("Cannot confirm payment in status: " + status);
        }

        // Confirm the credit deduction
        CustomerCredit credit = creditRepository.findByCustomerId(customerId)
            .orElseThrow(() -> new IllegalStateException("Customer not found: " + customerId));
        credit.confirmReservation(amount);
        creditRepository.save(credit);

        AggregateLifecycle.apply(new PaymentConfirmedEvent(
            command.orderId(),
            command.reservationId(),
            amount
        ));
    }

    @CommandHandler
    public void handle(ReleasePaymentCommand command, CustomerCreditRepository creditRepository) {
        if (status != ReservationStatus.RESERVED) {
            throw new IllegalStateException("Cannot release payment in status: " + status);
        }

        // Release the reserved credit
        CustomerCredit credit = creditRepository.findByCustomerId(customerId)
            .orElseThrow(() -> new IllegalStateException("Customer not found: " + customerId));
        credit.releaseReservation(amount);
        creditRepository.save(credit);

        AggregateLifecycle.apply(new PaymentReleasedEvent(
            command.orderId(),
            command.reservationId(),
            amount,
            command.reason()
        ));
    }

    @EventSourcingHandler
    public void on(PaymentReservedEvent event) {
        this.reservationId = event.reservationId();
        this.orderId = event.orderId();
        this.customerId = event.customerId();
        this.amount = event.amount();
        this.status = ReservationStatus.RESERVED;
        this.expiresAt = event.expiresAt();
        this.createdAt = event.timestamp();
    }

    @EventSourcingHandler
    public void on(PaymentReservationFailedEvent event) {
        // Set status to RELEASED on failure
        this.reservationId = event.reservationId();
        this.orderId = event.orderId();
        this.customerId = event.customerId();
        this.amount = event.requestedAmount();
        this.status = ReservationStatus.RELEASED;
        this.createdAt = event.timestamp();
    }

    @EventSourcingHandler
    public void on(PaymentConfirmedEvent event) {
        this.status = ReservationStatus.CONFIRMED;
    }

    @EventSourcingHandler
    public void on(PaymentReleasedEvent event) {
        this.status = ReservationStatus.RELEASED;
    }

    // Getters
    public UUID getReservationId() {
        return reservationId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
