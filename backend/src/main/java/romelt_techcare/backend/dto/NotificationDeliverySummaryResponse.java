package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationDeliveryStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION DELIVERY SUMMARY RESPONSE
 * ================================================================
 *
 * Used by:
 * - Delivery queue
 * - Notification monitoring
 * - Admin notification center
 * ================================================================
 */
public record NotificationDeliverySummaryResponse(

        UUID notificationDeliveryId,

        UUID notificationEventId,

        UUID customerId,

        UUID recipientAdminUserId,

        NotificationChannel channel,

        String recipientName,

        String recipientAddress,

        String subject,

        NotificationDeliveryStatus deliveryStatus,

        Integer attemptCount,

        Integer maximumAttempts,

        Instant nextAttemptAt,

        Instant sentAt,

        Instant deliveredAt,

        Instant readAt,

        Instant dismissedAt,

        Instant createdAt
) {
}