package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationRecipientType;
import romelt_techcare.backend.enums.NotificationResourceType;
import romelt_techcare.backend.enums.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION SUMMARY RESPONSE
 * ================================================================
 *
 * Purpose:
 * Provides compact notification information for administrator lists,
 * history pages, dashboards, and notification monitoring.
 * ================================================================
 */
public record NotificationSummaryResponse(

        UUID notificationId,

        NotificationChannel channel,

        NotificationRecipientType recipientType,

        UUID customerId,

        UUID adminUserId,

        NotificationResourceType resourceType,

        UUID resourceId,

        String recipientName,

        String recipientAddress,

        String title,

        String messageText,

        NotificationStatus notificationStatus,

        String providerName,

        String providerMessageId,

        Integer attemptCount,

        boolean read,

        boolean dismissed,

        Instant sentAt,

        Instant deliveredAt,

        Instant failedAt,

        Instant createdAt
) {
}