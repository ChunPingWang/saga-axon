package com.example.sales.infrastructure.web;

import com.example.sales.application.dto.CreateOrderRequest;
import com.example.sales.application.dto.OrderResponse;
import com.example.sales.application.service.OrderApplicationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for Order operations.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderApplicationService orderService;

    public OrderController(OrderApplicationService orderService) {
        this.orderService = orderService;
    }

    /**
     * Create a new order.
     * POST /api/v1/orders
     *
     * @return 202 Accepted with order ID
     */
    @PostMapping
    public CompletableFuture<ResponseEntity<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        log.info("Received create order request: customerId={}, productId={}",
            request.customerId(), request.productId());

        return orderService.createOrder(request)
            .thenApply(response -> ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(response));
    }

    /**
     * Get order by ID.
     * GET /api/v1/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public CompletableFuture<ResponseEntity<OrderResponse>> getOrder(
            @PathVariable UUID orderId) {
        log.debug("Received get order request: orderId={}", orderId);

        return orderService.getOrder(orderId)
            .thenApply(optionalOrder -> optionalOrder
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build()));
    }

    /**
     * Handle validation errors.
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
            jakarta.validation.ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "VALIDATION_ERROR",
            "message", ex.getMessage()
        ));
    }

    /**
     * Handle illegal argument errors.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "INVALID_REQUEST",
            "message", ex.getMessage()
        ));
    }
}
