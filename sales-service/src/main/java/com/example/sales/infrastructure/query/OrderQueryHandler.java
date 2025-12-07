package com.example.sales.infrastructure.query;

import com.example.sales.application.service.OrderApplicationService.FindOrderByIdQuery;
import com.example.shared.event.OrderCancelledEvent;
import com.example.shared.event.OrderConfirmedEvent;
import com.example.shared.event.OrderCreatedEvent;
import com.example.shared.valueobject.OrderStatus;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Query handler for Order queries.
 * Also handles event projections to update the query model.
 */
@Component
public class OrderQueryHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderQueryHandler.class);

    private final OrderQueryRepository repository;

    public OrderQueryHandler(OrderQueryRepository repository) {
        this.repository = repository;
    }

    @QueryHandler
    public Optional<OrderQueryModel> handle(FindOrderByIdQuery query) {
        return repository.findById(query.orderId());
    }

    @EventHandler
    public void on(OrderCreatedEvent event) {
        log.debug("Projecting OrderCreatedEvent for order: {}", event.orderId());
        OrderQueryModel model = new OrderQueryModel(
            event.orderId(),
            event.customerId(),
            event.productId(),
            event.quantity(),
            event.amount(),
            OrderStatus.PENDING,
            event.timestamp()
        );
        repository.save(model);
    }

    @EventHandler
    public void on(OrderConfirmedEvent event) {
        log.debug("Projecting OrderConfirmedEvent for order: {}", event.orderId());
        repository.findById(event.orderId()).ifPresent(model -> {
            model.updateStatus(OrderStatus.CONFIRMED, event.timestamp());
            repository.save(model);
        });
    }

    @EventHandler
    public void on(OrderCancelledEvent event) {
        log.debug("Projecting OrderCancelledEvent for order: {}", event.orderId());
        repository.findById(event.orderId()).ifPresent(model -> {
            OrderStatus status = event.isTimeout() ? OrderStatus.CANCELLED_TIMEOUT : OrderStatus.CANCELLED;
            model.updateStatus(status, event.reason(), event.timestamp());
            repository.save(model);
        });
    }
}
