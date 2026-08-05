package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.NotificationCategory;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationSuppressionReason;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION SUPPRESSION RESPONSE
 * ================================================================
 */
public record NotificationSuppressionResponse(

        UUID notificationSuppressionId,

        UUID customerId,

        NotificationChannel channel,

        NotificationCategory notificationCategory,

        String recipientAddress,

        String normalizedRecipientAddress,

        NotificationSuppressionReason suppressionReason,

        String reasonDetails,

        boolean active,

        boolean effective,

        Instant suppressedAt,

        Instant expiresAt,

        Instant deactivatedAt,

        UUID createdByAdminUserId,

        UUID deactivatedByAdminUserId,

        Instant createdAt,

        Instant updatedAt,

        Long rowVersion
) {
}