package romelt_techcare.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationRecipientType;
import romelt_techcare.backend.enums.NotificationResourceType;
import romelt_techcare.backend.enums.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION RESPONSE
 * ================================================================
 *
 * Purpose:
 * Represents the complete internal or administrator-facing
 * notification record.
 *
 * Security:
 * This response must not be exposed through public customer
 * endpoints because it may include recipient and provider details.
 * ================================================================
 */
public record NotificationResponse(

        UUID notificationId,

        NotificationChannel channel,

        NotificationRecipientType recipientType,

        UUID customerId,

        UUID adminUserId,

        NotificationResourceType resourceType,

        UUID resourceId,

        String recipientAddress,

        String recipientName,

        String title,

        String messageText,

        String messageHtml,

        NotificationStatus notificationStatus,

        String providerName,

        String providerMessageId,

        JsonNode providerResponseJson,

        Integer attemptCount,

        Instant lastAttemptedAt,

        Instant sentAt,

        Instant deliveredAt,

        Instant failedAt,

        String failureCode,

        String failureMessage,

        Instant readAt,

        Instant dismissedAt,

        boolean read,

        boolean dismissed,

        Instant createdAt,

        Instant updatedAt,

        Long rowVersion
) {
}