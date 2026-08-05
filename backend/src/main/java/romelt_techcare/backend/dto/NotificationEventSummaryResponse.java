package romelt_techcare.backend.dto;

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
 * ROMELT TECHCARE — NOTIFICATION EVENT SUMMARY RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns compact notification-event information for administrator
 * lists, monitoring dashboards, and searches.
 * ================================================================
 */
public record NotificationEventSummaryResponse(

        UUID notificationEventId,

        NotificationEventType eventType,

        NotificationCategory notificationCategory,

        NotificationChannel channel,

        NotificationResourceType resourceType,

        UUID resourceId,

        NotificationRecipientType recipientType,

        UUID recipientAdminUserId,

        String recipientName,

        String recipientEmail,

        String recipientPhone,

        String templateKey,

        Integer templateVersion,

        NotificationEventStatus eventStatus,

        NotificationPriority priority,

        Integer attemptCount,

        Integer maximumAttempts,

        Instant availableAt,

        Instant nextAttemptAt,

        Instant processedAt,

        Instant failedAt,

        Instant createdAt,

        Instant updatedAt
) {
}