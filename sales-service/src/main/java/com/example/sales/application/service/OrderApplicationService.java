package com.example.sales.application.service;

import com.example.sales.application.dto.CreateOrderRequest;
import com.example.sales.application.dto.OrderResponse;
import com.example.sales.infrastructure.query.OrderQueryModel;
import com.example.shared.command.CreateOrderCommand;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Application service for order operations.
 * Coordinates between the API layer and the domain layer.
 */
@Service
public class OrderApplicationService {

    private static final Logger log = LoggerFactory.getLogger(OrderApplicationService.class);
    private static final BigDecimal IPHONE17_PRICE = new BigDecimal("35000");

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    public OrderApplicationService(CommandGateway commandGateway, QueryGateway queryGateway) {
        this.commandGateway = commandGateway;
        this.queryGateway = queryGateway;
    }

    /**
     * Create a new order.
     * Returns immediately with order acceptance, actual processing is asynchronous.
     */
    public CompletableFuture<OrderResponse> createOrder(CreateOrderRequest request) {
        UUID orderId = UUID.randomUUID();
        Instant now = Instant.now();

        // MVP: Fixed price for iPhone 17
        BigDecimal amount = IPHONE17_PRICE;
        int quantity = request.quantity() != null ? request.quantity() : 1;

        log.info("Creating order {} for customer {} - product: {}, quantity: {}, amount: {}",
            orderId, request.customerId(), request.productId(), quantity, amount);

        CreateOrderCommand command = new CreateOrderCommand(
            orderId,
            request.customerId(),
            request.productId(),
            quantity,
            amount
        );

        return commandGateway.send(command)
            .thenApply(result -> OrderResponse.accepted(orderId, now));
    }

    /**
     * Get order by ID.
     */
    public CompletableFuture<Optional<OrderResponse>> getOrder(UUID orderId) {
        return queryGateway.query(
            new FindOrderByIdQuery(orderId),
            ResponseTypes.optionalInstanceOf(OrderQueryModel.class)
        ).thenApply(optionalModel ->
            optionalModel.map(this::toOrderResponse)
        );
    }

    private OrderResponse toOrderResponse(OrderQueryModel model) {
        return new OrderResponse(
            model.getOrderId(),
            model.getCustomerId(),
            model.getProductId(),
            model.getQuantity(),
            model.getAmount(),
            model.getStatus(),
            model.getStatusMessage(),
            model.getCreatedAt(),
            model.getUpdatedAt()
        );
    }

    /**
     * Query to find order by ID.
     */
    public record FindOrderByIdQuery(UUID orderId) {}
}
