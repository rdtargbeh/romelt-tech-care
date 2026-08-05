package romelt_techcare.backend.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION DELIVERY ATTEMPT SUMMARY RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns compact provider-attempt information for administrator
 * monitoring and delivery-history lists.
 * ================================================================
 */
public record NotificationDeliveryAttemptSummaryResponse(

        UUID notificationDeliveryAttemptId,

        UUID notificationDeliveryId,

        Integer attemptNumber,

        String providerName,

        String providerMessageId,

        String providerStatus,

        boolean success,

        boolean retryable,

        Integer httpStatus,

        String smtpStatus,

        String failureCode,

        Long durationMs,

        Instant startedAt,

        Instant completedAt,

        Instant createdAt
) {
}