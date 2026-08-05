package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.NotificationCategory;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationSuppressionReason;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION SUPPRESSION SUMMARY RESPONSE
 * ================================================================
 */
public record NotificationSuppressionSummaryResponse(

        UUID notificationSuppressionId,

        UUID customerId,

        NotificationChannel channel,

        NotificationCategory notificationCategory,

        String recipientAddress,

        NotificationSuppressionReason suppressionReason,

        boolean active,

        boolean effective,

        Instant suppressedAt,

        Instant expiresAt,

        Instant deactivatedAt,

        Instant createdAt
) {
}