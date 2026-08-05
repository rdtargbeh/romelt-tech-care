package romelt_techcare.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION DELIVERY ATTEMPT RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns one complete provider-attempt record to authenticated
 * administrator and internal operational APIs.
 * ================================================================
 */
public record NotificationDeliveryAttemptResponse(

        UUID notificationDeliveryAttemptId,

        UUID notificationDeliveryId,

        Integer attemptNumber,

        String providerName,

        String providerRequestId,

        String providerMessageId,

        String providerStatus,

        Instant startedAt,

        Instant completedAt,

        Long durationMs,

        boolean success,

        boolean retryable,

        Integer httpStatus,

        String smtpStatus,

        String failureCode,

        String failureMessage,

        JsonNode providerRequestJson,

        JsonNode providerResponseJson,

        Instant createdAt,

        Long rowVersion
) {
}