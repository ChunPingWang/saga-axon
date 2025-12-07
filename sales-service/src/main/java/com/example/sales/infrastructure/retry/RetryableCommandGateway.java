package com.example.sales.infrastructure.retry;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * A wrapper around CommandGateway that provides retry functionality
 * for compensation commands.
 *
 * According to US4, compensation commands should retry up to 3 times
 * with a 500ms delay between attempts.
 */
@Component
public class RetryableCommandGateway {

    private static final Logger log = LoggerFactory.getLogger(RetryableCommandGateway.class);

    private final CommandGateway commandGateway;

    public RetryableCommandGateway(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    /**
     * Send a compensation command with retry logic.
     * Retries up to 3 times with 500ms delay between attempts.
     *
     * @param command The compensation command to send
     * @param <C> The command type
     * @param <R> The result type
     * @return CompletableFuture with the result
     */
    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 500),
        retryFor = Exception.class
    )
    public <C, R> CompletableFuture<R> sendWithRetry(C command) {
        log.info("Sending compensation command: {}", command.getClass().getSimpleName());
        return commandGateway.send(command);
    }

    /**
     * Send a compensation command synchronously with retry logic.
     *
     * @param command The compensation command to send
     * @param <C> The command type
     * @param <R> The result type
     * @return The result
     */
    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 500),
        retryFor = Exception.class
    )
    public <C, R> R sendAndWaitWithRetry(C command) {
        log.info("Sending compensation command (sync): {}", command.getClass().getSimpleName());
        return commandGateway.sendAndWait(command);
    }

    /**
     * Recovery method called after all retry attempts are exhausted.
     *
     * @param e The exception that caused the failure
     * @param command The command that failed
     * @param <C> The command type
     * @param <R> The result type
     * @return null (compensation failed permanently)
     */
    @Recover
    public <C, R> CompletableFuture<R> recover(Exception e, C command) {
        log.error("Compensation command failed after all retries: {} - {}",
            command.getClass().getSimpleName(), e.getMessage());
        // In a production system, you might want to:
        // 1. Store the failed command for manual intervention
        // 2. Send an alert
        // 3. Log to a dead letter queue
        return CompletableFuture.failedFuture(e);
    }

    /**
     * Recovery method for synchronous calls.
     */
    @Recover
    public <C, R> R recoverSync(Exception e, C command) {
        log.error("Compensation command (sync) failed after all retries: {} - {}",
            command.getClass().getSimpleName(), e.getMessage());
        throw new CompensationFailedException("Compensation failed after retries", e);
    }

    /**
     * Exception thrown when compensation fails after all retry attempts.
     */
    public static class CompensationFailedException extends RuntimeException {
        public CompensationFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
