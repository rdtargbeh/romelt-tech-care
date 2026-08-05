package romelt_techcare.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import romelt_techcare.backend.enums.NotificationCategory;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationEventStatus;
import romelt_techcare.backend.enums.NotificationEventType;
import romelt_techcare.backend.enums.NotificationPriority;
import romelt_techcare.backend.enums.NotificationRecipientType;
import romelt_techcare.backend.enums.NotificationResourceType;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION EVENT RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns one complete notification-outbox event to authenticated
 * administrator and internal operational APIs.
 * ================================================================
 */
public record NotificationEventResponse(

        UUID notificationEventId,

        NotificationEventType eventType,

        NotificationCategory notificationCategory,

        NotificationChannel channel,

        NotificationResourceType resourceType,

        UUID resourceId,

        String correlationKey,

        String idempotencyKey,

        NotificationRecipientType recipientType,

        UUID customerId,

        UUID recipientAdminUserId,

        String recipientEmail,

        String recipientPhone,

        String recipientName,

        UUID notificationTemplateId,

        String templateKey,

        Integer templateVersion,

        String templateLocale,

        JsonNode templateDataJson,

        NotificationEventStatus eventStatus,

        NotificationPriority priority,

        Instant scheduledFor,

        Instant availableAt,

        Integer attemptCount,

        Integer maximumAttempts,

        Instant nextAttemptAt,

        Instant lastAttemptedAt,

        Instant processingStartedAt,

        Instant processedAt,

        Instant failedAt,

        Instant cancelledAt,

        String failureCode,

        String failureReason,

        Instant lockedAt,

        String lockedBy,

        Instant lockExpiresAt,

        UUID createdByAdminUserId,

        Instant createdAt,

        Instant updatedAt,

        Long rowVersion
) {
}