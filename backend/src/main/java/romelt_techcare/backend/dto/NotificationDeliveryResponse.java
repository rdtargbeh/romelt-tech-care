package romelt_techcare.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationDeliveryStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION DELIVERY RESPONSE
 * ================================================================
 */
public record NotificationDeliveryResponse(

        UUID notificationDeliveryId,

        UUID notificationEventId,

        UUID customerId,

        UUID recipientAdminUserId,

        NotificationChannel channel,

        String recipientAddress,

        String recipientName,

        String senderAddress,

        String senderName,

        String subject,

        String renderedBodyText,

        String renderedBodyHtml,

        NotificationDeliveryStatus deliveryStatus,

        String providerName,

        String providerMessageId,

        JsonNode providerResponseJson,

        Integer attemptCount,

        Integer maximumAttempts,

        Instant nextAttemptAt,

        Instant firstAttemptedAt,

        Instant lastAttemptedAt,

        Instant processingStartedAt,

        Instant sentAt,

        Instant deliveredAt,

        Instant openedAt,

        Instant clickedAt,

        Instant bouncedAt,

        Instant failedAt,

        Instant cancelledAt,

        Instant suppressedAt,

        Instant readAt,

        Instant dismissedAt,

        String failureCode,

        String failureMessage,

        String suppressionReason,

        Instant createdAt,

        Instant updatedAt,

        Long rowVersion
) {
}