package romelt_techcare.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION DELIVERY ATTEMPT CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Internal command used to persist one completed provider attempt.
 *
 * This request must not be exposed through a public controller.
 * ================================================================
 */
public record NotificationDeliveryAttemptCreateRequest(

        @NotNull(
                message = "Notification delivery ID is required."
        )
        UUID notificationDeliveryId,

        @NotNull(
                message = "Notification delivery attempt number is required."
        )
        @Min(
                value = 1,
                message = "Notification delivery attempt number must be greater than zero."
        )
        Integer attemptNumber,

        @Size(
                max = 100,
                message = "Provider name cannot exceed 100 characters."
        )
        String providerName,

        @Size(
                max = 255,
                message = "Provider request ID cannot exceed 255 characters."
        )
        String providerRequestId,

        @Size(
                max = 255,
                message = "Provider message ID cannot exceed 255 characters."
        )
        String providerMessageId,

        @Size(
                max = 100,
                message = "Provider status cannot exceed 100 characters."
        )
        String providerStatus,

        @NotNull(
                message = "Notification delivery attempt start time is required."
        )
        Instant startedAt,

        Instant completedAt,

        @Min(
                value = 0,
                message = "Notification delivery duration cannot be negative."
        )
        Long durationMs,

        boolean success,

        boolean retryable,

        @Min(
                value = 100,
                message = "HTTP status cannot be less than 100."
        )
        @Max(
                value = 599,
                message = "HTTP status cannot exceed 599."
        )
        Integer httpStatus,

        @Size(
                max = 50,
                message = "SMTP status cannot exceed 50 characters."
        )
        String smtpStatus,

        @Size(
                max = 100,
                message = "Failure code cannot exceed 100 characters."
        )
        String failureCode,

        String failureMessage,

        JsonNode providerRequestJson,

        JsonNode providerResponseJson
) {

    public boolean hasValidCompletion() {
        return completedAt == null
                || startedAt == null
                || !completedAt.isBefore(startedAt);
    }

    public boolean hasValidOutcome() {
        if (success) {
            return !retryable
                    && !hasText(failureCode)
                    && !hasText(failureMessage);
        }

        return hasText(failureMessage);
    }

    public boolean hasValidProviderJson() {
        return (
                providerRequestJson == null
                        || providerRequestJson.isObject()
        )
                && (
                providerResponseJson == null
                        || providerResponseJson.isObject()
        );
    }

    private boolean hasText(
            String value
    ) {
        return value != null
                && !value.trim().isEmpty();
    }
}